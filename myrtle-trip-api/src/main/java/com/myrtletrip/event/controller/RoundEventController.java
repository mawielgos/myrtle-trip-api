package com.myrtletrip.event.controller;

import com.myrtletrip.event.dto.RoundEventResponse;
import com.myrtletrip.event.dto.RoundEventsResultResponse;
import com.myrtletrip.event.dto.SaveRoundEventsRequest;
import com.myrtletrip.event.service.RoundEventResultService;
import com.myrtletrip.event.service.RoundEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/round-events")
@CrossOrigin
public class RoundEventController {

    private final RoundEventService roundEventService;
    private final RoundEventResultService roundEventResultService;

    public RoundEventController(RoundEventService roundEventService,
                                RoundEventResultService roundEventResultService) {
        this.roundEventService = roundEventService;
        this.roundEventResultService = roundEventResultService;
    }

    @GetMapping("/round/{roundId}")
    public ResponseEntity<List<RoundEventResponse>> getRoundEvents(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundEventService.getRoundEvents(roundId));
    }

    @PutMapping("/round/{roundId}")
    public ResponseEntity<List<RoundEventResponse>> saveRoundEvents(@PathVariable Long roundId,
                                                                    @RequestBody SaveRoundEventsRequest request) {
        return ResponseEntity.ok(roundEventService.saveRoundEvents(roundId, request));
    }

    @GetMapping("/round/{roundId}/results")
    public ResponseEntity<RoundEventsResultResponse> getRoundEventResults(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundEventResultService.getRoundEventResults(roundId));
    }
}
