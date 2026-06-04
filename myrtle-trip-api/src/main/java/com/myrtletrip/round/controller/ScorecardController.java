package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.ScorecardDetailResponse;
import com.myrtletrip.round.dto.UpdateScorecardTeeRequest;
import com.myrtletrip.round.dto.ScorecardParticipationRequest;
import com.myrtletrip.round.service.RoundRecalculationOrchestrationService;
import com.myrtletrip.round.service.ScorecardHandicapService;
import com.myrtletrip.round.service.ScorecardQueryService;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scorecards")
public class ScorecardController {

    private final ScorecardQueryService scorecardQueryService;
    private final ScorecardHandicapService scorecardHandicapService;
    private final RoundRecalculationOrchestrationService roundRecalculationOrchestrationService;
    private final ScorecardRepository scorecardRepository;
    private final TripEditingGuardService tripEditingGuardService;

    public ScorecardController(ScorecardQueryService scorecardQueryService,
                               ScorecardHandicapService scorecardHandicapService,
                               RoundRecalculationOrchestrationService roundRecalculationOrchestrationService,
                               ScorecardRepository scorecardRepository,
                               TripEditingGuardService tripEditingGuardService) {
        this.scorecardQueryService = scorecardQueryService;
        this.scorecardHandicapService = scorecardHandicapService;
        this.roundRecalculationOrchestrationService = roundRecalculationOrchestrationService;
        this.scorecardRepository = scorecardRepository;
        this.tripEditingGuardService = tripEditingGuardService;
    }

    @GetMapping("/{scorecardId}")
    public ResponseEntity<ScorecardDetailResponse> getScorecardDetail(@PathVariable Long scorecardId) {
        return ResponseEntity.ok(scorecardQueryService.getScorecardDetail(scorecardId));
    }

    @PutMapping("/{scorecardId}/tee")
    public ResponseEntity<Void> setScorecardTee(
            @PathVariable Long scorecardId,
            @RequestBody UpdateScorecardTeeRequest request) {

        if (request == null || request.getRoundTeeId() == null) {
            throw new IllegalArgumentException("roundTeeId is required");
        }

        scorecardHandicapService.setScorecardTee(scorecardId, request.getRoundTeeId());
        roundRecalculationOrchestrationService.handlePostScorecardChange(scorecardId);
        return ResponseEntity.ok().build();
    }


    @PatchMapping("/{scorecardId}/participation")
    public ResponseEntity<Void> setScorecardParticipation(
            @PathVariable Long scorecardId,
            @RequestBody ScorecardParticipationRequest request) {

        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));

        if (Boolean.TRUE.equals(scorecard.getRound().getFinalized())) {
            tripEditingGuardService.assertCorrectionAllowedForRound(scorecard.getRound());
        } else {
            tripEditingGuardService.assertStructureEditable(scorecard.getRound().getTrip());
        }

        ScorecardParticipationStatus nextStatus = parseParticipationStatus(
                request == null ? null : request.getParticipationStatus());
        scorecard.setParticipationStatus(nextStatus);

        if (nextStatus == ScorecardParticipationStatus.WITHDRAWN) {
            scorecard.setWithdrawalHoleNumber(
                    normalizeWithdrawalHoleNumber(request == null ? null : request.getWithdrawalHoleNumber()));
        } else {
            scorecard.setWithdrawalHoleNumber(null);
        }

        scorecardRepository.save(scorecard);
        roundRecalculationOrchestrationService.handlePostScorecardChange(scorecardId);
        return ResponseEntity.ok().build();
    }

    private ScorecardParticipationStatus parseParticipationStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ScorecardParticipationStatus.ACTIVE;
        }
        try {
            return ScorecardParticipationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid participation status: " + value);
        }
    }

    private Integer normalizeWithdrawalHoleNumber(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            return 0;
        }
        if (value > 18) {
            return 18;
        }
        return value;
    }

}
