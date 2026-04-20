package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.round.model.RoundFormat;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThreeLowNetScorer extends AbstractTeamGameScorer {

    @Override
    public RoundFormat supports() {
        return RoundFormat.THREE_LOW_NET;
    }

    @Override
    public RoundGameResult scoreRound(RoundScoringData data) {
        RoundGameResult result = createBaseResult(data);

        for (TeamScoringData team : data.getTeams()) {
            requireMinimumPlayerCount(team, 4, "Three Low Net");

            TeamGameResult teamResult = findTeamResult(result, team.getTeamId());

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                List<Integer> grosses = sortedHoleGrosses(team, holeNumber);
                List<Integer> nets = sortedHoleNets(team, holeNumber);

                int holeGross = sumLowest(grosses, 3);
                int holeNet = sumLowest(nets, 3);

                addHoleResult(teamResult, holeNumber, holeGross, holeNet, 0);
            }
        }

        assignMatchPointsTwoTeam(result);
        assignPlacementsByLowNet(result);
        return result;
    }
}