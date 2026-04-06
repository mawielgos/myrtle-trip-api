package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.HoleGameResult;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractTeamNetScorer implements RoundGameScorer {

    protected final HoleScoreRepository holeScoreRepository;

    protected AbstractTeamNetScorer(HoleScoreRepository holeScoreRepository) {
        this.holeScoreRepository = holeScoreRepository;
    }

    protected Integer requireNetScore(HoleScore holeScore, Long roundId, Long playerId, int holeNumber) {
        if (holeScore == null || holeScore.getNetStrokes() == null) {
            throw new IllegalStateException(
                    "Missing net score for roundId=" + roundId
                            + ", playerId=" + playerId
                            + ", hole=" + holeNumber
            );
        }
        return holeScore.getNetStrokes();
    }

    protected Integer requireGrossScore(HoleScore holeScore, Long roundId, Long playerId, int holeNumber) {
        if (holeScore == null || holeScore.getStrokes() == null) {
            throw new IllegalStateException(
                    "Missing gross score for roundId=" + roundId
                            + ", playerId=" + playerId
                            + ", hole=" + holeNumber
            );
        }
        return holeScore.getStrokes();
    }

    protected HoleScore findHoleScore(Long scorecardId, int holeNumber) {
        return holeScoreRepository.findByScorecard_Id(scorecardId).stream()
                .filter(h -> h.getHoleNumber() != null && h.getHoleNumber() == holeNumber)
                .findFirst()
                .orElse(null);
    }

    protected Map<Long, List<Scorecard>> groupScorecardsByTeam(List<Scorecard> scorecards) {
        return scorecards.stream()
                .collect(Collectors.groupingBy(sc -> {
                    if (sc.getTeam() == null || sc.getTeam().getId() == null) {
                        throw new IllegalStateException("Scorecard " + sc.getId() + " is missing team assignment");
                    }
                    return sc.getTeam().getId();
                }));
    }

    protected RoundGameResult initResult(RoundScoringData data) {
        RoundGameResult result = new RoundGameResult();
        result.setRoundId(data.getRoundId());
        result.setFormat(data.getFormat());
        return result;
    }

    protected void assignPlacementsByLowNet(List<TeamGameResult> teams) {
        List<TeamGameResult> sorted = new ArrayList<>(teams);
        sorted.sort(Comparator.comparing(TeamGameResult::getTotalNet));

        int placement = 1;
        for (TeamGameResult team : sorted) {
            team.setPlacement(placement++);
        }
    }

    protected void assignMatchPointsTwoTeam(List<TeamGameResult> teams) {
        if (teams.size() != 2) {
            return;
        }

        TeamGameResult a = teams.get(0);
        TeamGameResult b = teams.get(1);

        int aPoints = 0;
        int bPoints = 0;

        for (int i = 0; i < a.getHoleResults().size(); i++) {
            HoleGameResult ah = a.getHoleResults().get(i);
            HoleGameResult bh = b.getHoleResults().get(i);

            if (ah.getNetScore() < bh.getNetScore()) {
                ah.setPoints(1);
                bh.setPoints(0);
                aPoints++;
            } else if (bh.getNetScore() < ah.getNetScore()) {
                ah.setPoints(0);
                bh.setPoints(1);
                bPoints++;
            }
        }

        a.setTotalPoints(aPoints);
        b.setTotalPoints(bPoints);
    }
}
