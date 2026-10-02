package com.myrtletrip.games.service;

import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.games.model.EventScoringContext;
import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundUsedHoleScoreService {

    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;

    public RoundUsedHoleScoreService(ScorecardRepository scorecardRepository,
                                     HoleScoreRepository holeScoreRepository) {
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
    }

    public void clearUsedHoleScoreFlags(EventScoringContext context) {
        if (context.getEventType() == RoundEventType.TEAM_SCRAMBLE) {
            return;
        }

        List<HoleScore> roundHoleScores = holeScoreRepository.findByScorecard_Round_Id(context.getRound().getId());
        for (HoleScore holeScore : roundHoleScores) {
            holeScore.setUsedInTeamGame(Boolean.FALSE);
        }
        holeScoreRepository.saveAll(roundHoleScores);
    }

    public void markUsedHoleScores(EventScoringContext context) {
        if (context.getEventType() == RoundEventType.TEAM_SCRAMBLE) {
            return;
        }

        clearUsedHoleScoreFlags(context);

        Long roundId = context.getRound().getId();
        RoundScoringData data = context.getScoringData();
        RoundEventType eventType = context.getEventType();

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
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
