package com.myrtletrip.permissions.service;

import com.myrtletrip.permissions.dto.RoundCapabilityResponse;
import com.myrtletrip.round.dto.RoundReadinessResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.service.RoundReadinessService;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoundCapabilityService {

    private static final String TRIP_LOCKED_REASON = "Trip is complete and locked.";
    private static final String ROUND_FINALIZED_REASON = "Round is finalized.";
    private static final String CORRECTION_MODE_REQUIRED_REASON = "Enable trip correction mode before changing finalized scores or tees.";

    private final RoundRepository roundRepository;
    private final RoundReadinessService roundReadinessService;

    public RoundCapabilityService(RoundRepository roundRepository, RoundReadinessService roundReadinessService) {
        this.roundRepository = roundRepository;
        this.roundReadinessService = roundReadinessService;
    }

    @Transactional(readOnly = true)
    public RoundCapabilityResponse getCapabilities(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
        return build(round);
    }

    public RoundCapabilityResponse build(Round round) {
        if (round == null) {
            throw new IllegalArgumentException("Round is required.");
        }

        Trip trip = round.getTrip();
        boolean roundFinalized = Boolean.TRUE.equals(round.getFinalized());
        boolean tripComplete = trip != null && TripStatus.COMPLETE.equals(trip.getStatus());
        boolean tripCorrectionMode = trip != null && Boolean.TRUE.equals(trip.getCorrectionMode());
        boolean tripLocked = tripComplete && !tripCorrectionMode;

        RoundCapabilityResponse response = new RoundCapabilityResponse();
        response.setRoundId(round.getId());
        response.setTripId(trip == null ? null : trip.getId());
        response.setRoundFinalized(roundFinalized);
        response.setTripComplete(tripComplete);
        response.setTripCorrectionMode(tripCorrectionMode);
        response.setTripLocked(tripLocked);

        RoundReadinessResponse readiness = round.getId() == null ? null : roundReadinessService.getReadiness(round.getId());
        boolean readinessReadyForScoring = readiness != null && Boolean.TRUE.equals(readiness.isReadyForScoring());
        String readinessReason = firstBlockingIssue(readiness);
        response.setReadinessReadyForScoring(readinessReadyForScoring);
        response.setReadinessReason(readinessReason);

        boolean structureEditable = !tripLocked && !roundFinalized;
        response.setCanAssignTeams(structureEditable);
        response.setAssignTeamsReason(reasonForStructureEdit(tripLocked, roundFinalized));

        response.setCanEditScrambleSetup(structureEditable);
        response.setEditScrambleSetupReason(reasonForStructureEdit(tripLocked, roundFinalized));

        response.setCanChangeTeeBeforeFinalization(structureEditable);
        response.setChangeTeeBeforeFinalizationReason(reasonForStructureEdit(tripLocked, roundFinalized));

        boolean correctionAllowed = roundFinalized && !tripLocked;
        response.setCanCorrectTeeAfterFinalization(correctionAllowed);
        if (!roundFinalized) {
            response.setCorrectTeeAfterFinalizationReason("Round is not finalized. Use normal tee assignment instead.");
        } else if (tripLocked) {
            response.setCorrectTeeAfterFinalizationReason(CORRECTION_MODE_REQUIRED_REASON);
        } else {
            response.setCorrectTeeAfterFinalizationReason(null);
        }

        boolean canSaveScores = !tripLocked && !roundFinalized && readinessReadyForScoring;
        response.setCanSaveScores(canSaveScores);
        response.setSaveScoresReason(reasonForScoreSave(tripLocked, roundFinalized, readinessReadyForScoring, readinessReason));

        boolean canEditCorrections = roundFinalized && !tripLocked;
        response.setCanEditCorrections(canEditCorrections);
        if (!roundFinalized) {
            response.setEditCorrectionsReason("Round is not finalized. Use normal scoring instead.");
        } else if (tripLocked) {
            response.setEditCorrectionsReason(CORRECTION_MODE_REQUIRED_REASON);
        } else {
            response.setEditCorrectionsReason(null);
        }

        boolean canViewScoring = roundFinalized || readinessReadyForScoring;
        response.setCanViewScoring(canViewScoring);
        response.setViewScoringReason(canViewScoring ? null : readinessReason);
        response.setCanViewResults(true);
        response.setViewResultsReason(null);

        return response;
    }

    public void assertCanAssignTeams(Round round) {
        RoundCapabilityResponse capabilities = build(round);
        if (!Boolean.TRUE.equals(capabilities.getCanAssignTeams())) {
            throw new IllegalStateException(capabilities.getAssignTeamsReason());
        }
    }

    public void assertCanEditScrambleSetup(Round round) {
        RoundCapabilityResponse capabilities = build(round);
        if (!Boolean.TRUE.equals(capabilities.getCanEditScrambleSetup())) {
            throw new IllegalStateException(capabilities.getEditScrambleSetupReason());
        }
    }

    public void assertCanSaveScores(Round round) {
        RoundCapabilityResponse capabilities = build(round);
        if (!Boolean.TRUE.equals(capabilities.getCanSaveScores())) {
            throw new IllegalStateException(capabilities.getSaveScoresReason());
        }
    }

    public void assertCanCorrectTeeAfterFinalization(Round round) {
        RoundCapabilityResponse capabilities = build(round);
        if (!Boolean.TRUE.equals(capabilities.getCanCorrectTeeAfterFinalization())) {
            throw new IllegalStateException(capabilities.getCorrectTeeAfterFinalizationReason());
        }
    }

    private String firstBlockingIssue(RoundReadinessResponse readiness) {
        if (readiness == null || readiness.getBlockingIssues() == null || readiness.getBlockingIssues().isEmpty()) {
            return null;
        }
        return readiness.getBlockingIssues().get(0);
    }

    private String reasonForStructureEdit(boolean tripLocked, boolean roundFinalized) {
        if (tripLocked) {
            return TRIP_LOCKED_REASON;
        }
        if (roundFinalized) {
            return ROUND_FINALIZED_REASON;
        }
        return null;
    }

    private String reasonForScoreSave(boolean tripLocked, boolean roundFinalized, boolean readinessReadyForScoring, String readinessReason) {
        if (tripLocked) {
            return TRIP_LOCKED_REASON;
        }
        if (roundFinalized) {
            return "Round is finalized. Use corrections instead of normal score entry.";
        }
        if (!readinessReadyForScoring) {
            return readinessReason == null ? "Round is not ready for scoring." : readinessReason;
        }
        return null;
    }
}
