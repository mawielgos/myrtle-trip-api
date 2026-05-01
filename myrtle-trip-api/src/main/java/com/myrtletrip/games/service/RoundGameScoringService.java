package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
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
    private final List<RoundGameScorer> scorerList;

    public RoundGameScoringService(RoundRepository roundRepository,
                                   RoundScoringDataService roundScoringDataService,
                                   ScorecardRepository scorecardRepository,
                                   HoleScoreRepository holeScoreRepository,
                                   List<RoundGameScorer> scorerList) {
        this.roundRepository = roundRepository;
        this.roundScoringDataService = roundScoringDataService;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.scorerList = scorerList;
    }

    @Transactional(readOnly = true)
    public RoundGameResult getRoundResult(Long roundId) {
        Round round = loadRound(roundId);
        RoundScoringData data = roundScoringDataService.build(round);

        if (!isCompleteForTeamGameScoring(round, data)) {
            return createUnscoredResult(data);
        }

        RoundGameScorer scorer = findScorer(round.getFormat());
        return scorer.scoreRound(data);
    }

    @Transactional
    public RoundGameResult recalculateRound(Long roundId) {
        Round round = loadRound(roundId);
        RoundScoringData data = roundScoringDataService.build(round);

        clearUsedHoleScoreFlags(round);

        if (!isCompleteForTeamGameScoring(round, data)) {
            return createUnscoredResult(data);
        }

        RoundGameScorer scorer = findScorer(round.getFormat());
        RoundGameResult result = scorer.scoreRound(data);

        markUsedHoleScores(round, data);

        return result;
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

    private boolean isCompleteForTeamGameScoring(Round round, RoundScoringData data) {
        if (round.getFormat() == RoundFormat.TEAM_SCRAMBLE) {
            return isCompleteTeamScrambleData(data);
        }

        return isCompletePlayerScoreData(data);
    }

    private boolean isCompletePlayerScoreData(RoundScoringData data) {
        for (TeamScoringData team : data.getTeams()) {
            for (PlayerScoringData player : team.getPlayers()) {
                if (!playerHasCompleteEighteenHoleScore(player)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean playerHasCompleteEighteenHoleScore(PlayerScoringData player) {
        for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
            PlayerHoleScoringData hole = findPlayerHole(player, holeNumber);
            if (hole == null || hole.getGross() == null || hole.getNet() == null) {
                return false;
            }
        }

        return true;
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

    private void clearUsedHoleScoreFlags(Round round) {
        if (round.getFormat() == RoundFormat.TEAM_SCRAMBLE) {
            return;
        }

        List<HoleScore> roundHoleScores = holeScoreRepository.findByScorecard_Round_Id(round.getId());
        for (HoleScore holeScore : roundHoleScores) {
            holeScore.setUsedInTeamGame(Boolean.FALSE);
        }
        holeScoreRepository.saveAll(roundHoleScores);
    }

    private Round loadRound(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
    }

    private RoundGameScorer findScorer(RoundFormat format) {
        for (RoundGameScorer scorer : scorerList) {
            if (scorer.supports() == format) {
                return scorer;
            }
        }
        throw new IllegalStateException("No scorer registered for format " + format);
    }

    private void markUsedHoleScores(Round round, RoundScoringData data) {
        if (round.getFormat() == RoundFormat.TEAM_SCRAMBLE) {
            return;
        }

        clearUsedHoleScoreFlags(round);

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

                List<PlayerHolePick> selected = selectUsedPicks(round.getFormat(), holeNumber, picks);

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

            Scorecard scorecard = scorecardByPlayerId.get(player.getPlayerId());
            if (scorecard == null) {
                throw new IllegalStateException(
                        "Missing scorecard for playerId=" + player.getPlayerId()
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

    private List<PlayerHolePick> selectUsedPicks(RoundFormat format,
                                                 int holeNumber,
                                                 List<PlayerHolePick> sortedByNet) {
        switch (format) {
            case MIDDLE_MAN:
                return selectMiddleMan(sortedByNet);

            case ONE_TWO_THREE:
                return selectOneTwoThree(holeNumber, sortedByNet);

            case THREE_LOW_NET:
                return selectLowest(sortedByNet, 3);

            case TWO_MAN_LOW_NET:
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
