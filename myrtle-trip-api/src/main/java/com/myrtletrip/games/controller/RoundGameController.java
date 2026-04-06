package com.myrtletrip.games.controller;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.service.RoundGameScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/round-games")
public class RoundGameController {

    private final RoundGameScoringService roundGameScoringService;

    public RoundGameController(RoundGameScoringService roundGameScoringService) {
        this.roundGameScoringService = roundGameScoringService;
    }

    @GetMapping("/{roundId}")
    public ResponseEntity<RoundGameResult> scoreRound(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundGameScoringService.scoreRound(roundId));
    }
}