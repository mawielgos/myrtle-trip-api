package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.round.model.RoundFormat;
import org.springframework.stereotype.Service;

@Service
public class TeamScrambleScorer extends AbstractTeamGameScorer {

    @Override
    public RoundFormat supports() {
        return RoundFormat.TEAM_SCRAMBLE;
    }

    @Override
    public RoundGameResult scoreRound(RoundScoringData data) {
        RoundGameResult result = createBaseResult(data);

        for (TeamScoringData team : data.getTeams()) {
            TeamGameResult teamResult = findTeamResult(result, team.getTeamId());

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                var hole = requireTeamHole(team, holeNumber);

                if (hole.getGross() == null) {
                    throw new IllegalStateException(
                            "Missing scramble team score for teamId=" + team.getTeamId()
                                    + ", hole=" + holeNumber
                    );
                }

                addHoleResult(teamResult, holeNumber, hole.getGross(), hole.getGross(), 0);
            }
        }

        assignPlacementsByLowGross(result);
        return result;
    }
}