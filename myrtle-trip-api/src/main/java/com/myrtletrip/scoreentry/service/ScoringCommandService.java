package com.myrtletrip.scoreentry.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.entity.RoundTeeHole;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.round.service.RoundEventCapabilityService;
import com.myrtletrip.round.service.RoundTeeResolver;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scorehistory.service.RoundScoreHistorySyncService;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScoringCommandService {

    private final HoleScoreRepository holeRepo;
    private final ScorecardRepository scorecardRepo;
    private final RoundTeeHoleRepository roundTeeHoleRepository;
    private final RoundScoreHistorySyncService roundScoreHistorySyncService;
    private final RoundTeeResolver roundTeeResolver;
    private final TripEditingGuardService tripEditingGuardService;
    private final RoundEventCapabilityService roundEventCapabilityService;

    public ScoringCommandService(HoleScoreRepository holeRepo,
                                 ScorecardRepository scorecardRepo,
                                 RoundTeeHoleRepository roundTeeHoleRepository,
                                 RoundScoreHistorySyncService roundScoreHistorySyncService,
                                 RoundTeeResolver roundTeeResolver,
                                 TripEditingGuardService tripEditingGuardService,
                                 RoundEventCapabilityService roundEventCapabilityService) {
        this.holeRepo = holeRepo;
        this.scorecardRepo = scorecardRepo;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.roundScoreHistorySyncService = roundScoreHistorySyncService;
        this.roundTeeResolver = roundTeeResolver;
        this.tripEditingGuardService = tripEditingGuardService;
        this.roundEventCapabilityService = roundEventCapabilityService;
    }

    @Transactional
    public void updateHoleScore(Long scorecardId, int holeNumber, int strokes) {
        if (holeNumber < 1 || holeNumber > 18) {
            throw new IllegalArgumentException("holeNumber must be between 1 and 18");
        }

        if (strokes < 1 || strokes > 20) {
            throw new IllegalArgumentException("strokes must be between 1 and 20");
        }

        Scorecard scorecard = scorecardRepo.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found"));
        tripEditingGuardService.assertCorrectionAllowedForRound(scorecard.getRound());

        HoleScore hole = holeRepo.findByScorecard_IdAndHoleNumber(scorecardId, holeNumber)
                .orElseGet(() -> {
                    HoleScore newHole = new HoleScore();
                    newHole.setHoleNumber(holeNumber);
                    newHole.setScorecard(scorecard);
                    return newHole;
                });

        hole.setStrokes(strokes);
        holeRepo.save(hole);

        recalculateScorecard(scorecardId);
    }

    @Transactional
    public void recalculate(Long scorecardId) {
        Scorecard scorecard = scorecardRepo.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found"));
        tripEditingGuardService.assertCorrectionAllowedForRound(scorecard.getRound());

        recalculateScorecard(scorecardId);
    }

    private void recalculateScorecard(Long scorecardId) {
        Scorecard scorecard = scorecardRepo.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found"));

        Round round = scorecard.getRound();
        if (isNoShowOrWithdrawnBeforeAnyHole(scorecard)) {
            clearScorecardTotals(scorecard);
            scorecardRepo.save(scorecard);
            if (Boolean.TRUE.equals(round.getFinalized())) {
                roundScoreHistorySyncService.syncFinalizedScorecard(scorecard);
            }
            return;
        }
        RoundTee roundTee = roundTeeResolver.resolve(scorecard);
        if (scorecard.getRoundTee() == null) {
            scorecard.setRoundTee(roundTee);
            scorecardRepo.save(scorecard);
        }

        List<RoundTeeHole> roundTeeHoles = roundTeeHoleRepository.findByRoundTee_IdOrderByHoleNumberAsc(roundTee.getId());
        if (roundTeeHoles.size() != 18) {
            throw new IllegalStateException("Expected 18 holes for round tee " + roundTee.getId() + " but found " + roundTeeHoles.size());
        }

        List<HoleScore> holeScores = holeRepo.findByScorecard_IdOrderByHoleNumberAsc(scorecardId);

        Map<Integer, HoleScore> holeScoreByHoleNumber = new HashMap<>();
        for (HoleScore holeScore : holeScores) {
            holeScoreByHoleNumber.put(holeScore.getHoleNumber(), holeScore);
        }

        int grossTotal = 0;
        int adjustedGrossTotal = 0;
        int netTotal = 0;
        int thruHole = 0;
        boolean hasAnyPlayedHoles = false;

        int playingHandicap = scorecard.getPlayingHandicap() == null ? 0 : scorecard.getPlayingHandicap();
        int courseHandicap = scorecard.getCourseHandicap() == null ? 0 : scorecard.getCourseHandicap();
        boolean scramble = isScramble(round);

        for (RoundTeeHole roundTeeHole : roundTeeHoles) {
            int holeNumber = roundTeeHole.getHoleNumber();
            int par = roundTeeHole.getPar();
            int strokeIndex = roundTeeHole.getHandicap();

            HoleScore holeScore = holeScoreByHoleNumber.get(holeNumber);
            if (holeScore == null) {
                holeScore = new HoleScore();
                holeScore.setScorecard(scorecard);
                holeScore.setHoleNumber(holeNumber);
            }

            Integer strokes = holeScore.getStrokes();

            if (!isHoleEligibleForScoring(scorecard, holeNumber)) {
                holeScore.setNetStrokes(null);
                holeScore.setAdjustedStrokes(null);
                holeRepo.save(holeScore);
                continue;
            }

            if (strokes == null) {
                holeScore.setNetStrokes(null);
                holeScore.setAdjustedStrokes(null);
                holeRepo.save(holeScore);
                continue;
            }

            hasAnyPlayedHoles = true;
            thruHole = Math.max(thruHole, holeNumber);
            grossTotal += strokes;

            if (scramble) {
                holeScore.setNetStrokes(null);
                holeScore.setAdjustedStrokes(null);
            } else {
                int playingHandicapStrokes = getStrokesForHole(playingHandicap, strokeIndex);
                int courseHandicapStrokes = getStrokesForHole(courseHandicap, strokeIndex);

                int netStrokes = strokes - playingHandicapStrokes;
                int adjustedStrokes = Math.min(strokes, par + 2 + courseHandicapStrokes);

                holeScore.setNetStrokes(netStrokes);
                holeScore.setAdjustedStrokes(adjustedStrokes);

                netTotal += netStrokes;
                adjustedGrossTotal += adjustedStrokes;
            }

            holeRepo.save(holeScore);
        }

        if (!hasAnyPlayedHoles) {
            scorecard.setGrossScore(null);
            scorecard.setAdjustedGrossScore(null);
            scorecard.setNetScore(null);
            scorecard.setThruHole(null);
        } else {
            scorecard.setGrossScore(grossTotal);
            scorecard.setThruHole(thruHole);

            if (scramble) {
                scorecard.setAdjustedGrossScore(null);
                scorecard.setNetScore(null);
            } else {
                scorecard.setAdjustedGrossScore(adjustedGrossTotal);
                scorecard.setNetScore(netTotal);
            }
        }

        scorecardRepo.save(scorecard);

        if (Boolean.TRUE.equals(round.getFinalized())) {
            roundScoreHistorySyncService.syncFinalizedScorecard(scorecard);
        }
    }

    private boolean isNoShowOrWithdrawnBeforeAnyHole(Scorecard scorecard) {
        if (scorecard == null || scorecard.getParticipationStatus() == null) {
            return false;
        }
        if (ScorecardParticipationStatus.NO_SHOW.equals(scorecard.getParticipationStatus())) {
            return true;
        }
        return ScorecardParticipationStatus.WITHDRAWN.equals(scorecard.getParticipationStatus())
                && (scorecard.getWithdrawalHoleNumber() == null || scorecard.getWithdrawalHoleNumber() <= 0);
    }

    private boolean isHoleEligibleForScoring(Scorecard scorecard, int holeNumber) {
        if (scorecard == null || scorecard.getParticipationStatus() == null) {
            return true;
        }
        if (ScorecardParticipationStatus.ACTIVE.equals(scorecard.getParticipationStatus())) {
            return true;
        }
        if (ScorecardParticipationStatus.WITHDRAWN.equals(scorecard.getParticipationStatus())) {
            Integer withdrawalHoleNumber = scorecard.getWithdrawalHoleNumber();
            return withdrawalHoleNumber != null && withdrawalHoleNumber > 0 && holeNumber <= withdrawalHoleNumber;
        }
        return false;
    }

    private void clearScorecardTotals(Scorecard scorecard) {
        List<HoleScore> holeScores = holeRepo.findByScorecard_IdOrderByHoleNumberAsc(scorecard.getId());
        for (HoleScore holeScore : holeScores) {
            holeScore.setStrokes(null);
            holeScore.setNetStrokes(null);
            holeScore.setAdjustedStrokes(null);
            holeRepo.save(holeScore);
        }

        scorecard.setGrossScore(null);
        scorecard.setAdjustedGrossScore(null);
        scorecard.setNetScore(null);
        scorecard.setThruHole(null);
    }

    private boolean isScramble(Round round) {
        return roundEventCapabilityService.isScrambleRound(round);
    }

    private int getStrokesForHole(int handicap, int holeHandicap) {
        if (handicap <= 0) {
            return 0;
        }

        int base = handicap / 18;
        int remainder = handicap % 18;
        int extra = holeHandicap <= remainder ? 1 : 0;

        return base + extra;
    }
}
