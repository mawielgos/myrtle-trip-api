package com.myrtletrip.games.service;

import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TwoLowNetScorer extends AbstractTeamGameScorer {

    @Override
    public RoundEventType supports() {
        return RoundEventType.TEAM_TWO_LOW_NET;
    }

    @Override
    public RoundGameResult scoreRound(RoundScoringData data) {
        RoundGameResult result = createBaseResult(data);

        for (TeamScoringData team : data.getTeams()) {
            requireMinimumPlayerCount(team, 4, "4-Man 2-Low Net");

            TeamGameResult teamResult = findTeamResult(result, team.getTeamId());

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                List<Integer> grosses = sortedHoleGrosses(team, holeNumber);
                List<Integer> nets = sortedHoleNets(team, holeNumber);

                if (!hasEnoughScores(grosses, 2) || !hasEnoughScores(nets, 2)) {
                    continue;
                }

                int holeGross = sumLowest(grosses, 2);
                int holeNet = sumLowest(nets, 2);

                addHoleResult(teamResult, holeNumber, holeGross, holeNet, 0);
            }
        }

        assignMatchPointsTwoTeam(result);
        assignPlacementsByLowNet(result);
        return result;
    }
}
