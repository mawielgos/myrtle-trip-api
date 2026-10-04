package com.myrtletrip.scoreentry.service;

import com.myrtletrip.scoreentry.dto.HoleScoreResponse;
import com.myrtletrip.scoreentry.dto.RoundScorecardResponse;
import com.myrtletrip.scoreentry.dto.ScorecardResponse;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScoringReadModelService {

    private final HoleScoreRepository holeRepo;
    private final ScorecardRepository scorecardRepo;

    public ScoringReadModelService(HoleScoreRepository holeRepo,
                                   ScorecardRepository scorecardRepo) {
        this.holeRepo = holeRepo;
        this.scorecardRepo = scorecardRepo;
    }

    @Transactional(readOnly = true)
    public ScorecardResponse getScorecard(Long scorecardId) {
        Scorecard scorecard = scorecardRepo.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found"));

        List<HoleScore> holes = holeRepo.findByScorecard_IdOrderByHoleNumberAsc(scorecardId);

        ScorecardResponse response = new ScorecardResponse();
        response.setScorecardId(scorecard.getId());
        response.setRoundId(scorecard.getRound().getId());
        response.setPlayerId(scorecard.getPlayer().getId());
        response.setPlayingHandicap(scorecard.getPlayingHandicap());
        response.setGrossScore(scorecard.getGrossScore());
        response.setNetScore(scorecard.getNetScore());
        response.setAdjustedGrossScore(scorecard.getAdjustedGrossScore());

        List<HoleScoreResponse> holeResponses = holes.stream().map(h -> {
            HoleScoreResponse dto = new HoleScoreResponse();
            dto.setHoleNumber(h.getHoleNumber());
            dto.setStrokes(h.getStrokes());
            dto.setNetStrokes(h.getNetStrokes());
            dto.setAdjustedStrokes(h.getAdjustedStrokes());
            return dto;
        }).toList();

        response.setHoles(holeResponses);
        return response;
    }

    @Transactional(readOnly = true)
    public List<RoundScorecardResponse> getRoundScorecards(Long roundId) {
        List<Scorecard> scorecards = scorecardRepo.findByRound_Id(roundId);

        return scorecards.stream().map(sc -> {
                    RoundScorecardResponse dto = new RoundScorecardResponse();

                    dto.setScorecardId(sc.getId());
                    dto.setPlayerId(sc.getPlayer().getId());
                    dto.setPlayerName(sc.getPlayer().getDisplayName());
                    dto.setPlayingHandicap(sc.getPlayingHandicap());
                    dto.setGrossScore(sc.getGrossScore());
                    dto.setNetScore(sc.getNetScore());
                    dto.setAdjustedGrossScore(sc.getAdjustedGrossScore());

                    return dto;
                })
                .sorted((a, b) -> Integer.compare(
                        a.getNetScore() == null ? 999 : a.getNetScore(),
                        b.getNetScore() == null ? 999 : b.getNetScore()
                ))
                .toList();
    }
}
