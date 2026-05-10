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
import com.myrtletrip.trip.service.TripEditingGuardService;
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
    private final TripEditingGuardService tripEditingGuardService;

    public ScorecardHandicapService(ScorecardRepository scorecardRepository,
                                    RoundTeeRepository roundTeeRepository,
                                    RoundHandicapService roundHandicapService,
                                    ScoringService scoringService,
                                    RoundTeeResolver roundTeeResolver,
                                    TripEditingGuardService tripEditingGuardService) {
        this.scorecardRepository = scorecardRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.roundHandicapService = roundHandicapService;
        this.scoringService = scoringService;
        this.roundTeeResolver = roundTeeResolver;
        this.tripEditingGuardService = tripEditingGuardService;
    }

    @Transactional
    public void setScorecardTee(Long scorecardId, Long roundTeeId) {
        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));

        tripEditingGuardService.assertCorrectionAllowedForRound(scorecard.getRound());

        RoundTee roundTee = roundTeeRepository.findById(roundTeeId)
                .orElseThrow(() -> new IllegalArgumentException("Round tee not found: " + roundTeeId));

        setRoundTeeAndRefreshHandicap(scorecard, roundTee);
        scorecardRepository.save(scorecard);
        scoringService.recalculate(scorecardId);
    }

    @Transactional
    public void applyTeeCorrections(Long roundId, List<RoundTeeCorrectionRequest> corrections) {
        if (corrections == null || corrections.isEmpty()) {
            return;
        }

        List<Scorecard> roundScorecards = scorecardRepository.findByRound_Id(roundId);
        if (roundScorecards.isEmpty()) {
            throw new IllegalArgumentException("Round not found or has no scorecards: " + roundId);
        }
        tripEditingGuardService.assertCorrectionAllowedForRound(roundScorecards.get(0).getRound());

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

            // A null tee in a correction payload means "no tee change".  Do not silently
            // reset the player to the round default tee; that can corrupt finalized-round
            // corrections when the UI is only saving score changes.
            if (requestedRoundTeeId == null) {
                continue;
            }

            Long currentRoundTeeId = scorecard.getRoundTee() == null ? null : scorecard.getRoundTee().getId();
            if (requestedRoundTeeId.equals(currentRoundTeeId)) {
                continue;
            }

            RoundTee roundTee = roundTeeRepository.findById(requestedRoundTeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Round tee not found: " + requestedRoundTeeId));
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

            refreshHandicaps(scorecard, !allowFinalized);
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
        if (targetRoundTee.getSourceCourseTee() != null
                && targetRoundTee.getSourceCourseTee().getCourse() != null
                && targetRoundTee.getSourceCourseTee().getCourse().getId() != null
                && round.getCourse() != null
                && round.getCourse().getId() != null
                && !targetRoundTee.getSourceCourseTee().getCourse().getId().equals(round.getCourse().getId())) {
            throw new IllegalArgumentException(
                    "Selected tee " + targetRoundTee.getTeeName()
                            + " is linked to a different course than this round"
            );
        }

        validateRoundTeeEligibilityForPlayer(scorecard, targetRoundTee);
        scorecard.setRoundTee(targetRoundTee);
        refreshHandicaps(scorecard, false);
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
        String normalizedGender = normalizeGender(player.getGender());
        // The database stores one round_tee row per source course tee. A row may carry
        // the men's snapshot values while the source CourseTee also has women's
        // rating/slope/par. Do not require the round_tee snapshot to match the player's
        // gender; validate against the source CourseTee eligibility instead.
        if (!targetRoundTee.getSourceCourseTee().isEligibleForGender(normalizedGender)) {
            throw new IllegalArgumentException(
                    "Tee " + targetRoundTee.getTeeName()
                            + " is not eligible for " + player.getDisplayName()
                            + " (gender " + normalizedGender + ")"
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
        refreshHandicaps(scorecard, true);
    }

    private void refreshHandicaps(Scorecard scorecard, boolean validateExistingAssignedTee) {
        RoundTee resolvedTee = roundTeeResolver.resolve(scorecard);
        scorecard.setRoundTee(resolvedTee);

        // For a tee CHANGE, validation is done before this method is called.
        // For a finalized-round score-only correction, the existing round_tee snapshot
        // is authoritative and should not be re-validated against current Course Master
        // gender eligibility/rating data. Re-validating existing snapshots can reject
        // old finalized scorecards after Course Master tee data changes.
        if (validateExistingAssignedTee) {
            validateRoundTeeEligibilityForPlayer(scorecard, resolvedTee);
        }

        Round round = scorecard.getRound();
        String handicapGroupCode = round.getTrip().getTripCode();
        roundHandicapService.populateCurrentHandicaps(scorecard, handicapGroupCode);
    }
}
