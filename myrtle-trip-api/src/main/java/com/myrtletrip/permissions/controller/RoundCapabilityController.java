package com.myrtletrip.permissions.controller;

import com.myrtletrip.permissions.dto.RoundCapabilityResponse;
import com.myrtletrip.permissions.service.RoundCapabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rounds")
public class RoundCapabilityController {

    private final RoundCapabilityService roundCapabilityService;

    public RoundCapabilityController(RoundCapabilityService roundCapabilityService) {
        this.roundCapabilityService = roundCapabilityService;
    }

    @GetMapping("/{roundId}/capabilities")
    public ResponseEntity<RoundCapabilityResponse> getRoundCapabilities(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundCapabilityService.getCapabilities(roundId));
    }
}
