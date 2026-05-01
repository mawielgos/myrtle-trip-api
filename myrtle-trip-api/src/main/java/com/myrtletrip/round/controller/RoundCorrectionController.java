package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundCorrectionLogResponse;
import com.myrtletrip.round.dto.RoundCorrectionRequest;
import com.myrtletrip.round.dto.RoundCorrectionResponse;
import com.myrtletrip.round.service.RoundCorrectionLogService;
import com.myrtletrip.round.service.RoundCorrectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rounds")
public class RoundCorrectionController {

    private final RoundCorrectionService roundCorrectionService;
    private final RoundCorrectionLogService roundCorrectionLogService;

    public RoundCorrectionController(RoundCorrectionService roundCorrectionService,
                                     RoundCorrectionLogService roundCorrectionLogService) {
        this.roundCorrectionService = roundCorrectionService;
        this.roundCorrectionLogService = roundCorrectionLogService;
    }

    @PutMapping("/{roundId}/corrections")
    public ResponseEntity<RoundCorrectionResponse> applyCorrection(@PathVariable Long roundId,
                                                                   @RequestBody RoundCorrectionRequest request) {
        return ResponseEntity.ok(roundCorrectionService.applyCorrection(roundId, request));
    }

    @GetMapping("/{roundId}/corrections")
    public ResponseEntity<List<RoundCorrectionLogResponse>> getCorrections(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundCorrectionLogService.getCorrectionsForRound(roundId));
    }
}
