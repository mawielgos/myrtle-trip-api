package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.BulkRoundScoreRequest;
import com.myrtletrip.round.dto.RoundStatusResponse;
import com.myrtletrip.round.service.BulkScoreEntryService;
import com.myrtletrip.round.service.RoundCompletionService;
import com.myrtletrip.round.service.RoundQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rounds")
public class RoundController {

    private final RoundCompletionService roundCompletionService;
    private final RoundQueryService roundQueryService;
    private final BulkScoreEntryService bulkScoreEntryService;

    public RoundController(RoundCompletionService roundCompletionService,
                           RoundQueryService roundQueryService,
                           BulkScoreEntryService bulkScoreEntryService) {
        this.roundCompletionService = roundCompletionService;
        this.roundQueryService = roundQueryService;
        this.bulkScoreEntryService = bulkScoreEntryService;
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
    @PutMapping("/{roundId}/scores/bulk")
    public ResponseEntity<Void> saveBulkScores(@PathVariable Long roundId,
                                               @RequestBody BulkRoundScoreRequest request) {
        bulkScoreEntryService.saveBulkScores(roundId, request);
        return ResponseEntity.ok().build();
    }
}