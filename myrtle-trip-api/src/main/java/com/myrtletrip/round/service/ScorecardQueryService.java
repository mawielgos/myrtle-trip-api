package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.ScorecardDetailResponse;
import com.myrtletrip.round.dto.ScorecardHoleResponse;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.entity.RoundTeeHole;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScorecardQueryService {

    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final RoundTeeHoleRepository roundTeeHoleRepository;

    public ScorecardQueryService(ScorecardRepository scorecardRepository,
                                 HoleScoreRepository holeScoreRepository,
                                 RoundTeeHoleRepository roundTeeHoleRepository) {
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
    }

    @Transactional(readOnly = true)
    public ScorecardDetailResponse getScorecardDetail(Long scorecardId) {
        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new EntityNotFoundException("Scorecard not found: " + scorecardId));

        List<HoleScore> holeScores = holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(scorecardId);

        ScorecardDetailResponse response = new ScorecardDetailResponse();
        response.setScorecardId(scorecard.getId());
        response.setPlayerId(scorecard.getPlayer().getId());
        response.setPlayerName(scorecard.getPlayer().getDisplayName());
        response.setCourseHandicap(scorecard.getCourseHandicap());
        response.setPlayingHandicap(scorecard.getPlayingHandicap());
        response.setGrossScore(scorecard.getGrossScore());
        response.setAdjustedGrossScore(scorecard.getAdjustedGrossScore());
        response.setNetScore(scorecard.getNetScore());
        response.setTeeName(scorecard.getRound().getStandardRoundTee() != null
                ? scorecard.getRound().getStandardRoundTee().getTeeName()
                : null);
        response.setAlternateTeeName(scorecard.getRound().getAlternateRoundTee() != null
                ? scorecard.getRound().getAlternateRoundTee().getTeeName()
                : null);
        response.setCurrentTeeName(resolveCurrentTeeName(scorecard));
        response.setUseAlternateTee(isUsingAlternateTee(scorecard));
        Map<Integer, HoleScore> holeScoreByNumber = new HashMap<>();
        for (HoleScore holeScore : holeScores) {
            if (holeScore.getHoleNumber() != null) {
                holeScoreByNumber.put(holeScore.getHoleNumber(), holeScore);
            }
        }

        RoundTee roundTee = scorecard.getRoundTee();
        if (roundTee != null && roundTee.getId() != null) {
            List<RoundTeeHole> roundTeeHoles =
                    roundTeeHoleRepository.findByRoundTee_IdOrderByHoleNumberAsc(roundTee.getId());

            for (RoundTeeHole roundTeeHole : roundTeeHoles) {
                ScorecardHoleResponse hole = new ScorecardHoleResponse();
                hole.setHoleNumber(roundTeeHole.getHoleNumber());
                hole.setPar(roundTeeHole.getPar());
                hole.setHandicap(roundTeeHole.getHandicap());

                HoleScore holeScore = holeScoreByNumber.get(roundTeeHole.getHoleNumber());
                hole.setStrokes(holeScore != null ? holeScore.getStrokes() : null);
                hole.setNetStrokes(holeScore != null ? holeScore.getNetStrokes() : null);
                hole.setAdjustedStrokes(holeScore != null ? holeScore.getAdjustedStrokes() : null);

                response.getHoles().add(hole);
            }
        } else {
            for (HoleScore holeScore : holeScores) {
                ScorecardHoleResponse hole = new ScorecardHoleResponse();
                hole.setHoleNumber(holeScore.getHoleNumber());
                hole.setStrokes(holeScore.getStrokes());
                hole.setNetStrokes(holeScore.getNetStrokes());
                hole.setAdjustedStrokes(holeScore.getAdjustedStrokes());
                response.getHoles().add(hole);
            }
        }

        return response;
    }
    private boolean isUsingAlternateTee(Scorecard scorecard) {
        if (scorecard == null || scorecard.getRound() == null) {
            return false;
        }

        RoundTee currentRoundTee = scorecard.getRoundTee();
        RoundTee alternateRoundTee = scorecard.getRound().getAlternateRoundTee();

        if (currentRoundTee == null || alternateRoundTee == null) {
            return false;
        }

        if (currentRoundTee.getId() == null || alternateRoundTee.getId() == null) {
            return false;
        }

        return currentRoundTee.getId().equals(alternateRoundTee.getId());
    }
    
    private String resolveCurrentTeeName(Scorecard scorecard) {
        RoundTee roundTee = scorecard.getRoundTee();
        return roundTee != null ? roundTee.getTeeName() : null;
    }
}
