package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.round.model.RoundFormat;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OneTwoThreeScorer extends AbstractTeamGameScorer {

    @Override
    public RoundFormat supports() {
        return RoundFormat.ONE_TWO_THREE;
    }

    @Override
    public RoundGameResult scoreRound(RoundScoringData data) {
        RoundGameResult result = createBaseResult(data);

        for (TeamScoringData team : data.getTeams()) {
            requireExactPlayerCount(team, 4, "1-2-3");

            TeamGameResult teamResult = findTeamResult(result, team.getTeamId());

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                List<Integer> grosses = sortedHoleGrosses(team, holeNumber);
                List<Integer> nets = sortedHoleNets(team, holeNumber);

                int countToUse = switch ((holeNumber - 1) % 3) {
                    case 0 -> 1;
                    case 1 -> 2;
                    default -> 3;
                };

                int holeGross = sumLowest(grosses, countToUse);
                int holeNet = sumLowest(nets, countToUse);

                addHoleResult(teamResult, holeNumber, holeGross, holeNet, 0);
            }
        }

        assignMatchPointsTwoTeam(result);
        assignPlacementsByLowNet(result);
        return result;
    }
}
