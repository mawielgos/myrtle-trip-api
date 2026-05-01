package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundTeeCorrectionRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.service.ScoringService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScorecardHandicapService {

    private final ScorecardRepository scorecardRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final RoundHandicapService roundHandicapService;
    private final ScoringService scoringService;
    private final RoundTeeResolver roundTeeResolver;

    public ScorecardHandicapService(ScorecardRepository scorecardRepository,
                                    RoundTeeRepository roundTeeRepository,
                                    RoundHandicapService roundHandicapService,
                                    ScoringService scoringService,
                                    RoundTeeResolver roundTeeResolver) {
        this.scorecardRepository = scorecardRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.roundHandicapService = roundHandicapService;
        this.scoringService = scoringService;
        this.roundTeeResolver = roundTeeResolver;
    }

    @Transactional
    public void setScorecardTee(Long scorecardId, Long roundTeeId) {
        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));

        RoundTee roundTee = roundTeeRepository.findById(roundTeeId)
                .orElseThrow(() -> new IllegalArgumentException("Round tee not found: " + roundTeeId));

        setRoundTeeAndRefreshHandicap(scorecard, roundTee);
        scorecardRepository.save(scorecard);
        scoringService.recalculate(scorecardId);
    }

    /** Legacy endpoint support. False resets to default tee; true is rejected because alternate tee is retired. */
    @Transactional
    public void setAlternateTee(Long scorecardId, boolean useAlternateTee) {
        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));

        if (useAlternateTee) {
            throw new IllegalStateException("Alternate tee is no longer a round-level setting. Select a player tee instead.");
        }

        setRoundTeeAndRefreshHandicap(scorecard, scorecard.getRound().getDefaultRoundTee());
        scorecardRepository.save(scorecard);
        scoringService.recalculate(scorecardId);
    }

    @Transactional
    public void applyTeeCorrections(Long roundId, List<RoundTeeCorrectionRequest> corrections) {
        if (corrections == null || corrections.isEmpty()) {
            return;
        }

        for (RoundTeeCorrectionRequest correction : corrections) {
            if (correction == null || correction.getScorecardId() == null) {
                throw new IllegalArgumentException("Scorecard ID is required for every tee correction");
            }

            Scorecard scorecard = scorecardRepository.findById(correction.getScorecardId())
                    .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + correction.getScorecardId()));

            if (scorecard.getRound() == null || scorecard.getRound().getId() == null
                    || !scorecard.getRound().getId().equals(roundId)) {
                throw new IllegalArgumentException("Scorecard " + correction.getScorecardId() + " does not belong to round " + roundId);
            }

            Long requestedRoundTeeId = correction.getRoundTeeId();
            Long effectiveRoundTeeId = requestedRoundTeeId;
            if (effectiveRoundTeeId == null && scorecard.getRound().getDefaultRoundTee() != null) {
                effectiveRoundTeeId = scorecard.getRound().getDefaultRoundTee().getId();
            }
            if (effectiveRoundTeeId == null) {
                throw new IllegalArgumentException("roundTeeId is required for scorecard " + correction.getScorecardId());
            }

            RoundTee roundTee = roundTeeRepository.findById(effectiveRoundTeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Round tee not found"));
            setRoundTeeAndRefreshHandicap(scorecard, roundTee);
            scorecardRepository.save(scorecard);
        }
    }

    @Transactional
    public void refreshHandicaps(Long scorecardId) {
        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));

        refreshHandicaps(scorecard);
        scorecardRepository.save(scorecard);
        scoringService.recalculate(scorecardId);
    }

    @Transactional
    public void refreshRoundHandicaps(Long roundId) {
        refreshRoundHandicapsInternal(roundId, false);
    }

    @Transactional
    public void refreshRoundHandicapsForCorrection(Long roundId) {
        refreshRoundHandicapsInternal(roundId, true);
    }

    private void refreshRoundHandicapsInternal(Long roundId, boolean allowFinalized) {
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);

        for (Scorecard scorecard : scorecards) {
            if (!allowFinalized && Boolean.TRUE.equals(scorecard.getRound().getFinalized())) {
                throw new IllegalStateException("Cannot refresh handicaps after round is finalized");
            }

            if (scorecard.getRoundTee() == null) {
                scorecard.setRoundTee(scorecard.getRound().getDefaultRoundTee());
            }

            refreshHandicaps(scorecard);
            scorecardRepository.save(scorecard);
            scoringService.recalculate(scorecard.getId());
        }
    }

    private void setRoundTeeAndRefreshHandicap(Scorecard scorecard, RoundTee targetRoundTee) {
        Round round = scorecard.getRound();
        if (round == null) {
            throw new IllegalArgumentException("scorecard.round is required");
        }
        if (targetRoundTee == null) {
            throw new IllegalStateException("Round tee is required");
        }
        if (targetRoundTee.getRound() == null || targetRoundTee.getRound().getId() == null
                || !targetRoundTee.getRound().getId().equals(round.getId())) {
            throw new IllegalArgumentException("Selected tee does not belong to this round");
        }

        validateRoundTeeEligibilityForPlayer(scorecard, targetRoundTee);
        scorecard.setRoundTee(targetRoundTee);
        refreshHandicaps(scorecard);
    }

    private void validateRoundTeeEligibilityForPlayer(Scorecard scorecard, RoundTee targetRoundTee) {
        Player player = scorecard.getPlayer();
        if (player == null) {
            throw new IllegalArgumentException("scorecard.player is required");
        }
        if (targetRoundTee == null) {
            throw new IllegalArgumentException("targetRoundTee is required");
        }
        if (targetRoundTee.getSourceCourseTee() == null) {
            return;
        }
        if (!targetRoundTee.getSourceCourseTee().isEligibleForGender(player.getGender())) {
            throw new IllegalArgumentException(
                    "Tee " + targetRoundTee.getTeeName()
                            + " is not eligible for " + player.getDisplayName()
                            + " (gender " + normalizeGender(player.getGender()) + ")"
            );
        }
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return "M";
        }
        return gender.trim().toUpperCase();
    }

    private void refreshHandicaps(Scorecard scorecard) {
        RoundTee resolvedTee = roundTeeResolver.resolve(scorecard);
        scorecard.setRoundTee(resolvedTee);
        validateRoundTeeEligibilityForPlayer(scorecard, resolvedTee);

        Round round = scorecard.getRound();
        String handicapGroupCode = round.getTrip().getTripCode();
        roundHandicapService.populateCurrentHandicaps(scorecard, handicapGroupCode);
    }
}
