package com.myrtletrip.games.service;

import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.EventScoringContext;
import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamHoleScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import org.springframework.stereotype.Service;

@Service
public class RoundTeamGameResultService {

    public boolean isCompleteForScoring(EventScoringContext context) {
        if (context.getEventType() == RoundEventType.TEAM_SCRAMBLE) {
            return isCompleteTeamScrambleData(context.getScoringData());
        }

        return isCompletePlayerScoreData(context);
    }

    public RoundGameResult createUnscoredResult(RoundScoringData data) {
        RoundGameResult result = new RoundGameResult();
        result.setRoundId(data.getRoundId());
        result.setFormat(data.getFormat());

        for (TeamScoringData team : data.getTeams()) {
            TeamGameResult teamResult = new TeamGameResult();
            teamResult.setTeamId(team.getTeamId());
            teamResult.setTeamName(team.getTeamName());
            result.getTeams().add(teamResult);
        }

        return result;
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
            return ((holeNumber - 1) % 3) + 1;
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
        for (TeamHoleScoringData hole : team.getScrambleHoleScores()) {
            if (hole.getHoleNumber() != null
                    && hole.getHoleNumber() == holeNumber
                    && hole.getGross() != null) {
                return true;
            }
        }

        return false;
    }
}
