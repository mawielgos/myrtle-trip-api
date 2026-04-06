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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class MiddleManScorer extends AbstractTeamNetScorer {

    private final ScorecardRepository scorecardRepository;

    public MiddleManScorer(ScorecardRepository scorecardRepository,
                           HoleScoreRepository holeScoreRepository) {
        super(holeScoreRepository);
        this.scorecardRepository = scorecardRepository;
    }

    @Override
    public RoundFormat supports() {
        return RoundFormat.MIDDLE_MAN;
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

            TeamGameResult teamResult = new TeamGameResult();
            teamResult.setTeamId(first.getTeam().getId());
            teamResult.setTeamName(first.getTeam().getTeamName());

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                List<Integer> grosses = new ArrayList<>();
                List<Integer> nets = new ArrayList<>();

                for (Scorecard sc : teamScorecards) {
                    HoleScore hs = findHoleScore(sc.getId(), holeNumber);
                    grosses.add(requireGrossScore(hs, roundId, sc.getPlayer().getId(), holeNumber));
                    nets.add(requireNetScore(hs, roundId, sc.getPlayer().getId(), holeNumber));
                }

                grosses.sort(Comparator.naturalOrder());
                nets.sort(Comparator.naturalOrder());

                if (nets.size() < 3 || grosses.size() < 3) {
                    throw new IllegalStateException("Middle Man requires at least 3 player scores per team");
                }

                int middleIndex = nets.size() / 2;
                int holeGross = grosses.get(middleIndex);
                int holeNet = nets.get(middleIndex);

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