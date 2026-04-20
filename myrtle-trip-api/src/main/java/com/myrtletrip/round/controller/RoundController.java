package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.BulkRoundScoreRequest;
import com.myrtletrip.round.dto.RoundScorecardSummaryResponse;
import com.myrtletrip.round.dto.RoundSetupRequest;
import com.myrtletrip.round.dto.RoundStatusResponse;
import com.myrtletrip.round.service.BulkScoreEntryService;
import com.myrtletrip.round.service.RoundCompletionService;
import com.myrtletrip.round.service.RoundQueryService;
import com.myrtletrip.round.service.RoundSetupService;
import com.myrtletrip.round.service.ScorecardHandicapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rounds")
public class RoundController {

    private final RoundCompletionService roundCompletionService;
    private final RoundQueryService roundQueryService;
    private final BulkScoreEntryService bulkScoreEntryService;
    private final RoundSetupService roundSetupService;
    private final ScorecardHandicapService scorecardHandicapService;

    public RoundController(RoundCompletionService roundCompletionService,
                           RoundQueryService roundQueryService,
                           BulkScoreEntryService bulkScoreEntryService,
                           RoundSetupService roundSetupService,
                           ScorecardHandicapService scorecardHandicapService) {
        this.roundCompletionService = roundCompletionService;
        this.roundQueryService = roundQueryService;
        this.bulkScoreEntryService = bulkScoreEntryService;
        this.roundSetupService = roundSetupService;
        this.scorecardHandicapService = scorecardHandicapService;
    }

    @PostMapping
    public ResponseEntity<Long> startRound(@RequestBody RoundSetupRequest request) {
        Long roundId = roundSetupService.startRound(request);
        return ResponseEntity.ok(roundId);
    }

    @PostMapping("/{roundId}/refresh-handicaps")
    public ResponseEntity<Void> refreshRoundHandicaps(@PathVariable Long roundId) {
        scorecardHandicapService.refreshRoundHandicaps(roundId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roundId}/finalize")
    public ResponseEntity<Void> finalizeRound(@PathVariable Long roundId) {
        roundCompletionService.finalizeRound(roundId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roundId}/status")
    public ResponseEntity<RoundStatusResponse> getStatus(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundQueryService.getRoundStatus(roundId));
    }

    @GetMapping("/{roundId}/scorecards")
    public ResponseEntity<List<RoundScorecardSummaryResponse>> getRoundScorecards(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundQueryService.getRoundScorecards(roundId));
    }

    @PutMapping("/{roundId}/scores/bulk")
    public ResponseEntity<Void> saveBulkScores(@PathVariable Long roundId,
                                               @RequestBody BulkRoundScoreRequest request) {
        bulkScoreEntryService.saveBulkScores(roundId, request);
        return ResponseEntity.ok().build();
    }
}
