package com.myrtletrip.games.service;

import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.event.service.RoundEventService;
import com.myrtletrip.games.dto.HoleGameResult;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.EventScoringContext;
import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundGameScoringService {

    private final RoundRepository roundRepository;
    private final RoundScoringDataService roundScoringDataService;
    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final RoundGameScorerRegistry scorerRegistry;
    private final RoundEventService roundEventService;
    private final TripEditingGuardService tripEditingGuardService;

    public RoundGameScoringService(RoundRepository roundRepository,
                                   RoundScoringDataService roundScoringDataService,
                                   ScorecardRepository scorecardRepository,
                                   HoleScoreRepository holeScoreRepository,
                                   RoundGameScorerRegistry scorerRegistry,
                                   RoundEventService roundEventService,
                                   TripEditingGuardService tripEditingGuardService) {
        this.roundRepository = roundRepository;
        this.roundScoringDataService = roundScoringDataService;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.scorerRegistry = scorerRegistry;
        this.roundEventService = roundEventService;
        this.tripEditingGuardService = tripEditingGuardService;
    }

    @Transactional(readOnly = true)
    public RoundGameResult getRoundResult(Long roundId) {
        Round round = loadRound(roundId);
        RoundEvent event = resolvePrimaryTeamOrFallbackEvent(round);
        return getRoundResult(buildEventScoringContext(round, event));
    }

    @Transactional(readOnly = true)
    public RoundGameResult getRoundResult(Long roundId, RoundEventType eventType) {
        Round round = loadRound(roundId);
        RoundEvent event = resolveEvent(round, eventType);
        return getRoundResult(buildEventScoringContext(round, event));
    }

    private RoundGameResult getRoundResult(EventScoringContext context) {
        if (context.isIndividualEvent()) {
            return createIndividualStrokePlayResult(context.getScoringData());
        }

        if (!isCompleteForTeamGameScoring(context)) {
            return createUnscoredResult(context.getScoringData());
        }

        RoundGameScorer scorer = scorerRegistry.getScorer(context.getEventType());
        return scorer.scoreRound(context.getScoringData());
    }

    @Transactional
    public RoundGameResult recalculateRound(Long roundId) {
        Round round = loadRound(roundId);
        RoundEvent event = resolvePrimaryTeamOrFallbackEvent(round);
        return recalculateRound(buildEventScoringContext(round, event));
    }

    @Transactional
    public RoundGameResult recalculateRound(Long roundId, RoundEventType eventType) {
        Round round = loadRound(roundId);
        RoundEvent event = resolveEvent(round, eventType);
        return recalculateRound(buildEventScoringContext(round, event));
    }

    /**
     * Recalculates every active event on a round and returns the event-scoped results.
     *
     * Existing callers still use recalculateRound(...) for compatibility, but this
     * method is the transition point for correction/finalization flows that need to
     * be fully event-scoped instead of relying on the round's legacy format field.
     */
    @Transactional
    public List<RoundGameResult> recalculateRoundEvents(Long roundId) {
        Round round = loadRound(roundId);
        tripEditingGuardService.assertCorrectionAllowedForRound(round);

        List<RoundGameResult> results = new ArrayList<RoundGameResult>();
        for (RoundEvent event : roundEventService.findActiveEventsForRound(round.getId())) {
            results.add(recalculateRound(buildEventScoringContext(round, event)));
        }
        return results;
    }

    private RoundGameResult recalculateRound(EventScoringContext context) {
        tripEditingGuardService.assertCorrectionAllowedForRound(context.getRound());

        clearUsedHoleScoreFlags(context);

        if (context.isIndividualEvent()) {
            return createIndividualStrokePlayResult(context.getScoringData());
        }

        if (!isCompleteForTeamGameScoring(context)) {
            return createUnscoredResult(context.getScoringData());
        }

        RoundGameScorer scorer = scorerRegistry.getScorer(context.getEventType());
        RoundGameResult result = scorer.scoreRound(context.getScoringData());

        markUsedHoleScores(context);

        return result;
    }

    private EventScoringContext buildEventScoringContext(Round round, RoundEvent event) {
        RoundEventType eventType = event == null ? RoundEventType.fromLegacyRoundFormat(round.getFormat()) : event.getEventType();
        RoundScoringData data = roundScoringDataService.build(round);
        applyEventFormat(data, eventType);
        return new EventScoringContext(round, event, eventType, data);
    }

    private void applyEventFormat(RoundScoringData data, RoundEventType eventType) {
        if (data == null || eventType == null) {
            return;
        }
        data.setFormat(eventType.legacyRoundFormat());
    }

    private RoundGameResult createIndividualStrokePlayResult(RoundScoringData data) {
        RoundGameResult result = new RoundGameResult();
        result.setRoundId(data.getRoundId());
        result.setFormat(data.getFormat());

        List<TeamGameResult> playerResults = new ArrayList<>();
        for (TeamScoringData group : data.getTeams()) {
            for (PlayerScoringData player : group.getPlayers()) {
                TeamGameResult playerResult = new TeamGameResult();
                playerResult.setTeamId(player.getPlayerId());
                playerResult.setTeamName(player.getPlayerName());
                playerResult.setTotalGross(totalGross(player));
                playerResult.setTotalNet(totalNet(player));

                for (PlayerHoleScoringData playerHole : player.getHoles()) {
                    HoleGameResult holeResult = new HoleGameResult();
                    holeResult.setHoleNumber(playerHole.getHoleNumber());
                    holeResult.setGrossScore(playerHole.getGross());
                    holeResult.setNetScore(playerHole.getNet());
                    holeResult.setPoints(0);
                    playerResult.getHoleResults().add(holeResult);
                }

                playerResults.add(playerResult);
            }
        }

        playerResults.sort(new Comparator<TeamGameResult>() {
            @Override
            public int compare(TeamGameResult a, TeamGameResult b) {
                Integer aNet = a.getTotalNet();
                Integer bNet = b.getTotalNet();

                if (aNet == null && bNet == null) {
                    return compareNames(a.getTeamName(), b.getTeamName());
                }
                if (aNet == null) {
                    return 1;
                }
                if (bNet == null) {
                    return -1;
                }

                int netCompare = Integer.compare(aNet, bNet);
                if (netCompare != 0) {
                    return netCompare;
                }

                Integer aGross = a.getTotalGross();
                Integer bGross = b.getTotalGross();
                if (aGross == null && bGross == null) {
                    return compareNames(a.getTeamName(), b.getTeamName());
                }
                if (aGross == null) {
                    return 1;
                }
                if (bGross == null) {
                    return -1;
                }

                int grossCompare = Integer.compare(aGross, bGross);
                if (grossCompare != 0) {
                    return grossCompare;
                }

                return compareNames(a.getTeamName(), b.getTeamName());
            }
        });

        assignIndividualPlacements(playerResults);
        result.setTeams(playerResults);
        return result;
    }

    private Integer totalGross(PlayerScoringData player) {
        int total = 0;
        for (PlayerHoleScoringData hole : player.getHoles()) {
            if (hole.getGross() == null) {
                return null;
            }
            total += hole.getGross();
        }
        return total;
    }

    private Integer totalNet(PlayerScoringData player) {
        int total = 0;
        for (PlayerHoleScoringData hole : player.getHoles()) {
            if (hole.getNet() == null) {
                return null;
            }
            total += hole.getNet();
        }
        return total;
    }

    private void assignIndividualPlacements(List<TeamGameResult> playerResults) {
        Integer previousNet = null;
        int previousPlacement = 0;

        for (int i = 0; i < playerResults.size(); i++) {
            TeamGameResult playerResult = playerResults.get(i);
            Integer net = playerResult.getTotalNet();

            if (net == null) {
                playerResult.setPlacement(null);
                continue;
            }

            int placement = i + 1;
            if (previousNet != null && previousNet.equals(net)) {
                placement = previousPlacement;
            }

            playerResult.setPlacement(placement);
            previousNet = net;
            previousPlacement = placement;
        }
    }

    private RoundGameResult createUnscoredResult(RoundScoringData data) {
        RoundGameResult result = new RoundGameResult();
        result.setRoundId(data.getRoundId());
        result.setFormat(data.getFormat());

        for (TeamScoringData team : data.getTeams()) {
            com.myrtletrip.games.dto.TeamGameResult teamResult = new com.myrtletrip.games.dto.TeamGameResult();
            teamResult.setTeamId(team.getTeamId());
            teamResult.setTeamName(team.getTeamName());
            result.getTeams().add(teamResult);
        }

        return result;
    }

    private boolean isCompleteForTeamGameScoring(EventScoringContext context) {
        if (context.getEventType() == RoundEventType.TEAM_SCRAMBLE) {
            return isCompleteTeamScrambleData(context.getScoringData());
        }

        return isCompletePlayerScoreData(context);
    }

    private boolean isCompletePlayerScoreData(EventScoringContext context) {
        RoundScoringData data = context.getScoringData();
        for (TeamScoringData team : data.getTeams()) {
            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                int requiredScoreCount = requiredScoreCountForHole(context.getEventType(), holeNumber);
                int grossCount = 0;
                int netCount = 0;

                for (PlayerScoringData player : team.getPlayers()) {
                    PlayerHoleScoringData hole = findPlayerHole(player, holeNumber);
                    if (hole == null) {
                        continue;
                    }
                    if (hole.getGross() != null) {
                        grossCount++;
                    }
                    if (hole.getNet() != null) {
                        netCount++;
                    }
                }

                if (grossCount < requiredScoreCount || netCount < requiredScoreCount) {
                    return false;
                }
            }
        }

        return true;
    }

    private int requiredScoreCountForHole(RoundEventType eventType, int holeNumber) {
        if (eventType == RoundEventType.TEAM_TWO_LOW_NET) {
            return 2;
        }
        if (eventType == RoundEventType.TEAM_THREE_LOW_NET) {
            return 3;
        }
        if (eventType == RoundEventType.TEAM_ONE_TWO_THREE) {
            int cycle = ((holeNumber - 1) % 3) + 1;
            return cycle;
        }
        if (eventType == RoundEventType.TEAM_TWO_MAN_LOW_NET) {
            return 1;
        }
        if (eventType == RoundEventType.TEAM_MIDDLE_MAN) {
            return 4;
        }
        return 1;
    }

    private PlayerHoleScoringData findPlayerHole(PlayerScoringData player, int holeNumber) {
        for (PlayerHoleScoringData hole : player.getHoles()) {
            if (hole.getHoleNumber() != null && hole.getHoleNumber() == holeNumber) {
                return hole;
            }
        }

        return null;
    }

    private boolean isCompleteTeamScrambleData(RoundScoringData data) {
        for (TeamScoringData team : data.getTeams()) {
            if (team.getScrambleTotalScore() != null) {
                continue;
            }

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                if (!teamHasScrambleScore(team, holeNumber)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean teamHasScrambleScore(TeamScoringData team, int holeNumber) {
        for (com.myrtletrip.games.model.TeamHoleScoringData hole : team.getScrambleHoleScores()) {
            if (hole.getHoleNumber() != null
                    && hole.getHoleNumber() == holeNumber
                    && hole.getGross() != null) {
                return true;
            }
        }

        return false;
    }

    private void clearUsedHoleScoreFlags(EventScoringContext context) {
        if (context.getEventType() == RoundEventType.TEAM_SCRAMBLE) {
            return;
        }

        List<HoleScore> roundHoleScores = holeScoreRepository.findByScorecard_Round_Id(context.getRound().getId());
        for (HoleScore holeScore : roundHoleScores) {
            holeScore.setUsedInTeamGame(Boolean.FALSE);
        }
        holeScoreRepository.saveAll(roundHoleScores);
    }

    private Round loadRound(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
    }

    private RoundEvent resolvePrimaryTeamOrFallbackEvent(Round round) {
        List<RoundEvent> events = roundEventService.findActiveEventsForRound(round.getId());
        for (RoundEvent event : events) {
            if (event != null && event.getEventType() != null && event.getEventType().isTeamEvent()) {
                return event;
            }
        }
        if (!events.isEmpty() && events.get(0) != null && events.get(0).getEventType() != null) {
            return events.get(0);
        }
        return null;
    }

    private RoundEvent resolveEvent(Round round, RoundEventType requestedEventType) {
        if (requestedEventType == null) {
            return resolvePrimaryTeamOrFallbackEvent(round);
        }

        List<RoundEvent> events = roundEventService.findActiveEventsForRound(round.getId());
        for (RoundEvent event : events) {
            if (event != null && requestedEventType == event.getEventType()) {
                return event;
            }
        }

        RoundEventType legacyEventType = RoundEventType.fromLegacyRoundFormat(round.getFormat());
        if (requestedEventType == legacyEventType) {
            return null;
        }

        throw new IllegalArgumentException(
                "Round " + round.getId() + " does not have active event type " + requestedEventType
        );
    }

    private void markUsedHoleScores(EventScoringContext context) {
        if (context.getEventType() == RoundEventType.TEAM_SCRAMBLE) {
            return;
        }

        clearUsedHoleScoreFlags(context);

        Round round = context.getRound();
        RoundScoringData data = context.getScoringData();
        RoundEventType eventType = context.getEventType();

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(round.getId());
        Map<Long, Scorecard> scorecardByPlayerId = new HashMap<Long, Scorecard>();
        for (Scorecard scorecard : scorecards) {
            scorecardByPlayerId.put(scorecard.getPlayer().getId(), scorecard);
        }

        for (TeamScoringData team : data.getTeams()) {
            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                List<PlayerHolePick> picks = buildHolePicks(team, holeNumber, scorecardByPlayerId);

                if (picks.isEmpty()) {
                    continue;
                }

                picks.sort(new Comparator<PlayerHolePick>() {
                    @Override
                    public int compare(PlayerHolePick a, PlayerHolePick b) {
                        if (a.getNet() == null && b.getNet() == null) {
                            return compareNames(a.getPlayerName(), b.getPlayerName());
                        }
                        if (a.getNet() == null) {
                            return 1;
                        }
                        if (b.getNet() == null) {
                            return -1;
                        }

                        int netCompare = Integer.compare(a.getNet(), b.getNet());
                        if (netCompare != 0) {
                            return netCompare;
                        }

                        return compareNames(a.getPlayerName(), b.getPlayerName());
                    }
                });

                List<PlayerHolePick> selected = selectUsedPicks(eventType, holeNumber, picks);

                for (PlayerHolePick pick : selected) {
                    HoleScore holeScore = findHoleScoreOrThrow(pick.getScorecardId(), holeNumber);
                    holeScore.setUsedInTeamGame(Boolean.TRUE);
                    holeScoreRepository.save(holeScore);
                }
            }
        }
    }

    private HoleScore findHoleScoreOrThrow(Long scorecardId, int holeNumber) {
        HoleScore holeScore = holeScoreRepository.findByScorecard_IdAndHoleNumber(scorecardId, holeNumber)
                .orElse(null);

        if (holeScore == null) {
            throw new IllegalStateException(
                    "Missing HoleScore for scorecardId=" + scorecardId
                            + ", holeNumber=" + holeNumber
            );
        }

        return holeScore;
    }

    private int compareNames(String a, String b) {
        String left = a == null ? "" : a;
        String right = b == null ? "" : b;
        return String.CASE_INSENSITIVE_ORDER.compare(left, right);
    }

    private List<PlayerHolePick> buildHolePicks(TeamScoringData team,
                                                int holeNumber,
                                                Map<Long, Scorecard> scorecardByPlayerId) {
        List<PlayerHolePick> picks = new ArrayList<PlayerHolePick>();

        for (PlayerScoringData player : team.getPlayers()) {
            Scorecard scorecard = scorecardByPlayerId.get(player.getPlayerId());
            if (scorecard == null) {
                throw new IllegalStateException(
                        "Missing scorecard for playerId=" + player.getPlayerId()
                );
            }

            if (!isScorecardEligibleForTeamHole(scorecard, holeNumber)) {
                continue;
            }

            PlayerHoleScoringData matchingHole = null;

            for (PlayerHoleScoringData hole : player.getHoles()) {
                if (hole.getHoleNumber() != null && hole.getHoleNumber() == holeNumber) {
                    matchingHole = hole;
                    break;
                }
            }

            if (matchingHole == null) {
                throw new IllegalStateException(
                        "Missing player hole score for playerId=" + player.getPlayerId()
                                + ", hole=" + holeNumber
                );
            }

            if (matchingHole.getNet() == null) {
                continue;
            }

            PlayerHolePick pick = new PlayerHolePick();
            pick.setPlayerName(player.getPlayerName());
            pick.setScorecardId(scorecard.getId());
            pick.setNet(matchingHole.getNet());
            picks.add(pick);
        }

        return picks;
    }


    private boolean isScorecardEligibleForTeamHole(Scorecard scorecard, int holeNumber) {
        if (scorecard == null) {
            return false;
        }
        ScorecardParticipationStatus status = scorecard.getParticipationStatus();
        if (status == null || status == ScorecardParticipationStatus.ACTIVE) {
            return true;
        }
        if (status == ScorecardParticipationStatus.WITHDRAWN) {
            Integer withdrawalHoleNumber = scorecard.getWithdrawalHoleNumber();
            return withdrawalHoleNumber != null && withdrawalHoleNumber > 0 && holeNumber <= withdrawalHoleNumber;
        }
        return false;
    }

    private List<PlayerHolePick> selectUsedPicks(RoundEventType eventType,
                                                 int holeNumber,
                                                 List<PlayerHolePick> sortedByNet) {
        switch (eventType) {
            case TEAM_MIDDLE_MAN:
                return selectMiddleMan(sortedByNet);

            case TEAM_ONE_TWO_THREE:
                return selectOneTwoThree(holeNumber, sortedByNet);

            case TEAM_THREE_LOW_NET:
                return selectLowest(sortedByNet, 3);

            case TEAM_TWO_LOW_NET:
                return selectLowest(sortedByNet, 2);

            case TEAM_TWO_MAN_LOW_NET:
                return selectLowest(sortedByNet, 1);

            default:
                return new ArrayList<PlayerHolePick>();
        }
    }

    private List<PlayerHolePick> selectMiddleMan(List<PlayerHolePick> sortedByNet) {
        if (sortedByNet.size() != 4) {
            throw new IllegalStateException(
                    "Middle Man requires exactly 4 hole scores, found " + sortedByNet.size()
            );
        }

        List<PlayerHolePick> selected = new ArrayList<PlayerHolePick>();
        selected.add(sortedByNet.get(1));
        selected.add(sortedByNet.get(2));
        return selected;
    }

    private List<PlayerHolePick> selectOneTwoThree(int holeNumber, List<PlayerHolePick> sortedByNet) {
        int holePattern = ((holeNumber - 1) % 3) + 1;
        return selectLowest(sortedByNet, holePattern);
    }

    private List<PlayerHolePick> selectLowest(List<PlayerHolePick> sortedByNet, int count) {
        if (sortedByNet.size() < count) {
            throw new IllegalStateException(
                    "Not enough hole scores to select " + count + ", found " + sortedByNet.size()
            );
        }

        List<PlayerHolePick> selected = new ArrayList<PlayerHolePick>();
        for (int i = 0; i < count; i++) {
            selected.add(sortedByNet.get(i));
        }
        return selected;
    }

    private static class PlayerHolePick {
        private String playerName;
        private Long scorecardId;
        private Integer net;

        public String getPlayerName() {
            return playerName;
        }

        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        public Long getScorecardId() {
            return scorecardId;
        }

        public void setScorecardId(Long scorecardId) {
            this.scorecardId = scorecardId;
        }

        public Integer getNet() {
            return net;
        }

        public void setNet(Integer net) {
            this.net = net;
        }
    }
}
