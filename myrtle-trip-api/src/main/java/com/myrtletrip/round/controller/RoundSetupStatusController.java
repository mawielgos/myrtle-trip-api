package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundSetupStatusResponse;
import com.myrtletrip.round.service.RoundSetupStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rounds")
public class RoundSetupStatusController {

    private final RoundSetupStatusService roundSetupStatusService;

    public RoundSetupStatusController(RoundSetupStatusService roundSetupStatusService) {
        this.roundSetupStatusService = roundSetupStatusService;
    }

    @GetMapping("/{roundId}/setup-status")
    public ResponseEntity<RoundSetupStatusResponse> getSetupStatus(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundSetupStatusService.getSetupStatus(roundId));
    }
}
