package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.ScorecardDetailResponse;
import com.myrtletrip.round.dto.UpdateScorecardTeeRequest;
import com.myrtletrip.round.service.RoundRecalculationOrchestrationService;
import com.myrtletrip.round.service.ScorecardHandicapService;
import com.myrtletrip.round.service.ScorecardQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scorecards")
public class ScorecardController {

    private final ScorecardQueryService scorecardQueryService;
    private final ScorecardHandicapService scorecardHandicapService;
    private final RoundRecalculationOrchestrationService roundRecalculationOrchestrationService;

    public ScorecardController(ScorecardQueryService scorecardQueryService,
                               ScorecardHandicapService scorecardHandicapService,
                               RoundRecalculationOrchestrationService roundRecalculationOrchestrationService) {
        this.scorecardQueryService = scorecardQueryService;
        this.scorecardHandicapService = scorecardHandicapService;
        this.roundRecalculationOrchestrationService = roundRecalculationOrchestrationService;
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

    @PutMapping("/{scorecardId}/alternate-tee")
    public ResponseEntity<Void> setAlternateTee(
            @PathVariable Long scorecardId,
            @RequestParam boolean useAlternateTee) {

        scorecardHandicapService.setAlternateTee(scorecardId, useAlternateTee);
        roundRecalculationOrchestrationService.handlePostScorecardChange(scorecardId);
        return ResponseEntity.ok().build();
    }
}
