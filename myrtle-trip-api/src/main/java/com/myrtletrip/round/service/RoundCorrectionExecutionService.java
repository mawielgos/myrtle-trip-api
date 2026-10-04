package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.BulkRoundScoreRequest;
import com.myrtletrip.round.dto.PlayerBulkScoreDto;
import com.myrtletrip.round.dto.RoundCorrectionRequest;
import com.myrtletrip.round.dto.RoundCorrectionResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoundCorrectionExecutionService {

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final BulkScoreEntryService bulkScoreEntryService;
    private final ScorecardHandicapService scorecardHandicapService;
    private final RoundRecalculationOrchestrationService roundRecalculationOrchestrationService;
    private final RoundCorrectionAuditService roundCorrectionAuditService;
    private final TripEditingGuardService tripEditingGuardService;

    public RoundCorrectionExecutionService(RoundRepository roundRepository,
                                           ScorecardRepository scorecardRepository,
                                           BulkScoreEntryService bulkScoreEntryService,
                                           ScorecardHandicapService scorecardHandicapService,
                                           RoundRecalculationOrchestrationService roundRecalculationOrchestrationService,
                                           RoundCorrectionAuditService roundCorrectionAuditService,
                                           TripEditingGuardService tripEditingGuardService) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.bulkScoreEntryService = bulkScoreEntryService;
        this.scorecardHandicapService = scorecardHandicapService;
        this.roundRecalculationOrchestrationService = roundRecalculationOrchestrationService;
        this.roundCorrectionAuditService = roundCorrectionAuditService;
        this.tripEditingGuardService = tripEditingGuardService;
    }

    @Transactional
    public RoundCorrectionResponse applyCorrection(Long roundId, RoundCorrectionRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        tripEditingGuardService.assertCorrectionAllowedForRound(round);
        validateRequest(request);

        Boolean wasFinalized = round.getFinalized();
        boolean changedWithoutBulkScoreSave = false;

        List<RoundCorrectionAuditService.ScorecardSnapshot> teeSnapshots = roundCorrectionAuditService.snapshotTeeCorrections(roundId, request.getTeeCorrections());
        List<RoundCorrectionAuditService.ScorecardSnapshot> handicapSnapshots = Boolean.TRUE.equals(request.getRefreshHandicaps())
                ? roundCorrectionAuditService.snapshotAllRoundScorecards(roundId)
                : new ArrayList<RoundCorrectionAuditService.ScorecardSnapshot>();
        List<RoundCorrectionAuditService.ScorecardSnapshot> scoreSnapshots = roundCorrectionAuditService.snapshotPlayerCorrections(roundId, request.getPlayerCorrections());
        List<RoundCorrectionAuditService.ScorecardSnapshot> participationSnapshots = roundCorrectionAuditService.snapshotParticipationCorrections(roundId, request.getParticipationCorrections());

        if (request.getParticipationCorrections() != null && !request.getParticipationCorrections().isEmpty()) {
            applyParticipationCorrections(roundId, request.getParticipationCorrections());
            changedWithoutBulkScoreSave = true;
            roundCorrectionAuditService.logParticipationChanges(round, participationSnapshots);
        }

        if (request.getTeeCorrections() != null && !request.getTeeCorrections().isEmpty()) {
            scorecardHandicapService.applyTeeCorrections(roundId, request.getTeeCorrections());
            changedWithoutBulkScoreSave = true;
            roundCorrectionAuditService.logTeeChanges(round, teeSnapshots);
        }

        if (Boolean.TRUE.equals(request.getRefreshHandicaps())) {
            scorecardHandicapService.refreshRoundHandicapsForCorrection(roundId);
            changedWithoutBulkScoreSave = true;
            roundCorrectionAuditService.logHandicapRefreshes(round, handicapSnapshots);
        }

        if (request.getPlayerCorrections() != null && !request.getPlayerCorrections().isEmpty()) {
            BulkRoundScoreRequest bulkRequest = new BulkRoundScoreRequest();
            List<PlayerBulkScoreDto> scorecards = new ArrayList<PlayerBulkScoreDto>();

            for (RoundCorrectionRequest.PlayerCorrectionDto correction : request.getPlayerCorrections()) {
                if (correction == null) {
                    continue;
                }

                PlayerBulkScoreDto dto = new PlayerBulkScoreDto();
                dto.setPlayerId(correction.getPlayerId());
                dto.setHoles(correction.getHoles());
                scorecards.add(dto);
            }

            bulkRequest.setScorecards(scorecards);

            // This path saves hole scores on finalized rounds and then runs the full correction cascade once.
            // Do not use the normal bulk score save gate here; finalized rounds are expected in correction mode.
            bulkScoreEntryService.saveBulkScoreCorrections(roundId, bulkRequest);
            roundCorrectionAuditService.logScoreChanges(round, scoreSnapshots);
        } else if (changedWithoutBulkScoreSave) {
            // Tee/handicap-only corrections still affect net scores, game results, history, standings, and payouts.
            roundRecalculationOrchestrationService.handlePostRoundChange(roundId);
        }

        // Keep the finalized flag stable for post-finalization corrections.
        Round refreshedRound = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found after correction: " + roundId));

        if (Boolean.TRUE.equals(wasFinalized) && !Boolean.TRUE.equals(refreshedRound.getFinalized())) {
            refreshedRound.setFinalized(true);
            roundRepository.save(refreshedRound);
            roundRecalculationOrchestrationService.handlePostRoundChange(roundId);
        }

        return new RoundCorrectionResponse(
                true,
                "Correction applied and recalculated successfully"
        );
    }

    private void validateRequest(RoundCorrectionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Correction request is required");
        }

        boolean hasPlayerCorrections = request.getPlayerCorrections() != null && !request.getPlayerCorrections().isEmpty();
        boolean hasTeeCorrections = request.getTeeCorrections() != null && !request.getTeeCorrections().isEmpty();
        boolean hasParticipationCorrections = request.getParticipationCorrections() != null && !request.getParticipationCorrections().isEmpty();
        boolean refreshHandicaps = Boolean.TRUE.equals(request.getRefreshHandicaps());

        if (!hasPlayerCorrections && !hasTeeCorrections && !hasParticipationCorrections && !refreshHandicaps) {
            throw new IllegalArgumentException("At least one score, tee, participation, or handicap correction is required");
        }
    }

    private void applyParticipationCorrections(Long roundId, List<RoundCorrectionRequest.ParticipationCorrectionDto> corrections) {
        if (corrections == null) {
            return;
        }
        for (RoundCorrectionRequest.ParticipationCorrectionDto correction : corrections) {
            if (correction == null || correction.getScorecardId() == null) {
                continue;
            }
            Scorecard scorecard = scorecardRepository.findById(correction.getScorecardId())
                    .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + correction.getScorecardId()));
            if (scorecard.getRound() == null || scorecard.getRound().getId() == null
                    || !scorecard.getRound().getId().equals(roundId)) {
                throw new IllegalArgumentException("Scorecard " + correction.getScorecardId() + " does not belong to round " + roundId);
            }

            ScorecardParticipationStatus status = parseParticipationStatus(correction.getParticipationStatus());
            scorecard.setParticipationStatus(status);
            if (status == ScorecardParticipationStatus.WITHDRAWN) {
                scorecard.setWithdrawalHoleNumber(normalizeWithdrawalHoleNumber(correction.getWithdrawalHoleNumber()));
            } else {
                scorecard.setWithdrawalHoleNumber(null);
            }
            scorecardRepository.save(scorecard);
        }
    }

    private ScorecardParticipationStatus parseParticipationStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ScorecardParticipationStatus.ACTIVE;
        }
        try {
            return ScorecardParticipationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported participation status: " + value);
        }
    }

    private Integer normalizeWithdrawalHoleNumber(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > 18) {
            throw new IllegalArgumentException("Withdrawal hole must be between 0 and 18.");
        }
        return value;
    }
}
