package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundScrambleScoreResponse;
import com.myrtletrip.round.dto.SaveRoundScrambleScoresRequest;
import com.myrtletrip.round.service.RoundScrambleScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rounds")
public class RoundScrambleScoreController {

    private final RoundScrambleScoreService roundScrambleScoreService;

    public RoundScrambleScoreController(RoundScrambleScoreService roundScrambleScoreService) {
        this.roundScrambleScoreService = roundScrambleScoreService;
    }

    @GetMapping("/{roundId}/scramble-scores")
    public ResponseEntity<RoundScrambleScoreResponse> getScrambleScores(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundScrambleScoreService.getScrambleScores(roundId));
    }

    @PutMapping("/{roundId}/scramble-scores")
    public ResponseEntity<Void> saveScrambleScores(@PathVariable Long roundId,
                                                   @RequestBody SaveRoundScrambleScoresRequest request) {
        roundScrambleScoreService.saveScrambleScores(roundId, request);
        return ResponseEntity.ok().build();
    }
}
