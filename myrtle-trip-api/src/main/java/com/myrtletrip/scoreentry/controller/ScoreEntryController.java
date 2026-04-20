package com.myrtletrip.scoreentry.controller;

import com.myrtletrip.scoreentry.dto.RoundScorecardResponse;
import com.myrtletrip.scoreentry.service.ScoringService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scorecards")
public class ScoreEntryController {

    private final ScoringService scoringService;

    public ScoreEntryController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @PutMapping("/{scorecardId}/holes/{holeNumber}")
    public ResponseEntity<Void> updateHoleScore(
            @PathVariable Long scorecardId,
            @Min(1) @Max(18) @PathVariable int holeNumber,
            @Valid @RequestBody HoleRequest request) {
    	
        scoringService.updateHoleScore(scorecardId, holeNumber, request.strokes());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{scorecardId}/recalculate")
    public ResponseEntity<Void> recalculateScorecard(@PathVariable Long scorecardId) {
        scoringService.recalculate(scorecardId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/round/{roundId}")
    public ResponseEntity<List<RoundScorecardResponse>> getRoundScorecards(@PathVariable Long roundId) {
        return ResponseEntity.ok(scoringService.getRoundScorecards(roundId));
    }

    public record HoleRequest(
            @Min(value = 1, message = "Strokes must be at least 1")
            @Max(value = 15, message = "Strokes must be <= 15")
            int strokes
    ) {}

    public record AlternateTeeRequest(
            boolean useAlternateTee
    ) {}
}