package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundReadinessResponse;
import com.myrtletrip.round.service.RoundReadinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rounds")
public class RoundReadinessController {

    private final RoundReadinessService roundReadinessService;

    public RoundReadinessController(RoundReadinessService roundReadinessService) {
        this.roundReadinessService = roundReadinessService;
    }

    @GetMapping("/{roundId}/readiness")
    public RoundReadinessResponse getReadiness(@PathVariable Long roundId) {
        return roundReadinessService.getReadiness(roundId);
    }
}
