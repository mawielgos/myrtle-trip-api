package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.HoleGameResult;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TwoManLowNetScorer extends AbstractTeamNetScorer {

    private final ScorecardRepository scorecardRepository;

    public TwoManLowNetScorer(ScorecardRepository scorecardRepository,
                              HoleScoreRepository holeScoreRepository) {
        super(holeScoreRepository);
        this.scorecardRepository = scorecardRepository;
    }

    @Override
    public RoundFormat supports() {
        return RoundFormat.TWO_MAN_LOW_NET;
    }

    @Override
    public RoundGameResult scoreRound(RoundScoringData data) {
        Long roundId = data.getRoundId();

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
        if (scorecards.isEmpty()) {
            throw new IllegalStateException("No scorecards found for round " + roundId);
        }

        Map<Long, List<Scorecard>> byTeam = groupScorecardsByTeam(scorecards);

        RoundGameResult result = initResult(data);
        List<TeamGameResult> teams = new ArrayList<>();

        for (Map.Entry<Long, List<Scorecard>> entry : byTeam.entrySet()) {
            List<Scorecard> teamScorecards = entry.getValue();
            Scorecard first = teamScorecards.get(0);

            if (teamScorecards.size() != 2) {
                throw new IllegalStateException("Two-man low net requires exactly 2 players per team");
            }

            TeamGameResult teamResult = new TeamGameResult();
            teamResult.setTeamId(first.getTeam().getId());
            teamResult.setTeamName(first.getTeam().getTeamName());

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                int holeGross = 0;
                int holeNet = 0;

                for (Scorecard sc : teamScorecards) {
                    HoleScore hs = findHoleScore(sc.getId(), holeNumber);
                    holeGross += requireGrossScore(hs, roundId, sc.getPlayer().getId(), holeNumber);
                    holeNet += requireNetScore(hs, roundId, sc.getPlayer().getId(), holeNumber);
                }

                HoleGameResult holeResult = new HoleGameResult();
                holeResult.setHoleNumber(holeNumber);
                holeResult.setGrossScore(holeGross);
                holeResult.setNetScore(holeNet);

                teamResult.getHoleResults().add(holeResult);
                teamResult.setTotalGross(teamResult.getTotalGross() + holeGross);
                teamResult.setTotalNet(teamResult.getTotalNet() + holeNet);
            }

            teams.add(teamResult);
        }

        result.setTeams(teams);

        if (teams.size() == 2) {
            assignMatchPointsTwoTeam(teams);
        }

        assignPlacementsByLowNet(teams);
        return result;
    }
}