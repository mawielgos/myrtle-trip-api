package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.round.model.RoundFormat;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MiddleManScorer extends AbstractTeamGameScorer {

    @Override
    public RoundFormat supports() {
        return RoundFormat.MIDDLE_MAN;
    }

    @Override
    public RoundGameResult scoreRound(RoundScoringData data) {
        RoundGameResult result = createBaseResult(data);

        for (TeamScoringData team : data.getTeams()) {
            requireExactPlayerCount(team, 4, "Middle Man");

            TeamGameResult teamResult = findTeamResult(result, team.getTeamId());

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                List<Integer> grosses = sortedHoleGrosses(team, holeNumber);
                List<Integer> nets = sortedHoleNets(team, holeNumber);

                int holeGross = grosses.get(1) + grosses.get(2);
                int holeNet = nets.get(1) + nets.get(2);

                addHoleResult(teamResult, holeNumber, holeGross, holeNet, 0);
            }
        }

        assignMatchPointsTwoTeam(result);
        assignPlacementsByLowNet(result);
        return result;
    }
}