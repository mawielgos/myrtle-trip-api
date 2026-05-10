package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.RoundHandicapService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScorecardQueryService {

    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final RoundTeeHoleRepository roundTeeHoleRepository;
    private final RoundTeeResolver roundTeeResolver;
    private final RoundHandicapService roundHandicapService;

    public ScorecardQueryService(ScorecardRepository scorecardRepository,
                                 HoleScoreRepository holeScoreRepository,
                                 RoundTeeHoleRepository roundTeeHoleRepository,
                                 RoundTeeResolver roundTeeResolver,
                                 RoundHandicapService roundHandicapService) {
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.roundTeeResolver = roundTeeResolver;
        this.roundHandicapService = roundHandicapService;
    }

    @Transactional(readOnly = true)
    public ScorecardDetailResponse getScorecardDetail(Long scorecardId) {
        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new EntityNotFoundException("Scorecard not found: " + scorecardId));

        RoundTee effectiveRoundTee = roundTeeResolver.resolve(scorecard);
        List<HoleScore> holeScores = holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(scorecardId);

        ScorecardDetailResponse response = new ScorecardDetailResponse();
        response.setScorecardId(scorecard.getId());
        response.setPlayerId(scorecard.getPlayer().getId());
        response.setPlayerName(scorecard.getPlayer().getDisplayName());
        populateHandicapSnapshot(response, scorecard);
        response.setCourseHandicap(scorecard.getCourseHandicap());
        response.setPlayingHandicap(scorecard.getPlayingHandicap());
        response.setGrossScore(scorecard.getGrossScore());
        response.setAdjustedGrossScore(scorecard.getAdjustedGrossScore());
        response.setNetScore(scorecard.getNetScore());
        response.setTeeName(scorecard.getRound().getDefaultRoundTee() != null
                ? scorecard.getRound().getDefaultRoundTee().getTeeName()
                : null);
        response.setCurrentTeeName(effectiveRoundTee != null ? effectiveRoundTee.getTeeName() : null);
        response.setRoundTeeId(effectiveRoundTee != null ? effectiveRoundTee.getId() : null);

        Map<Integer, HoleScore> holeScoreByNumber = new HashMap<>();
        for (HoleScore holeScore : holeScores) {
            if (holeScore.getHoleNumber() != null) {
                holeScoreByNumber.put(holeScore.getHoleNumber(), holeScore);
            }
        }

        if (effectiveRoundTee != null && effectiveRoundTee.getId() != null) {
            List<RoundTeeHole> roundTeeHoles =
                    roundTeeHoleRepository.findByRoundTee_IdOrderByHoleNumberAsc(effectiveRoundTee.getId());

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

    private void populateHandicapSnapshot(ScorecardDetailResponse response, Scorecard scorecard) {
        if (response == null || scorecard == null) {
            return;
        }

        if (scorecard.getRound() == null || scorecard.getRound().getRoundDate() == null || scorecard.getRound().getTrip() == null) {
            return;
        }

        response.setHandicapAsOfDate(scorecard.getRound().getRoundDate());
        response.setHandicapMethod(scorecard.getPlayer() != null ? scorecard.getPlayer().getHandicapMethod() : null);
        response.setHandicapLabel(buildHandicapLabel(scorecard));
        response.setTripIndex(calculateTripIndexSafely(scorecard));
    }

    private BigDecimal calculateTripIndexSafely(Scorecard scorecard) {
        try {
            if (scorecard.getRound() == null
                    || scorecard.getRound().getTrip() == null
                    || scorecard.getRound().getTrip().getTripCode() == null) {
                return null;
            }

            return roundHandicapService.calculateTripIndex(scorecard, scorecard.getRound().getTrip().getTripCode());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String buildHandicapLabel(Scorecard scorecard) {
        if (scorecard == null || scorecard.getRound() == null || scorecard.getRound().getRoundDate() == null) {
            return null;
        }

        String method = scorecard.getPlayer() != null ? scorecard.getPlayer().getHandicapMethod() : null;
        String displayMethod = displayHandicapMethod(method);
        LocalDate roundDate = scorecard.getRound().getRoundDate();
        return displayMethod + " index as of " + roundDate + " (same-day scores excluded)";
    }

    private String displayHandicapMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            return "Trip";
        }

        String normalized = method.trim().toUpperCase();
        if ("MYRTLE_BEACH".equals(normalized) || "DB_SCORE_HISTORY".equals(normalized)) {
            return "DB Score History";
        }
        if ("GHIN".equals(normalized)) {
            return "GHIN";
        }

        return method.trim();
    }

}
