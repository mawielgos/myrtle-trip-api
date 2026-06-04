package com.myrtletrip.readiness.controller;

import com.myrtletrip.readiness.dto.TripWorkflowReadinessResponse;
import com.myrtletrip.readiness.service.TripWorkflowReadinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/workflow-readiness")
public class TripWorkflowReadinessController {

    private final TripWorkflowReadinessService tripWorkflowReadinessService;

    public TripWorkflowReadinessController(TripWorkflowReadinessService tripWorkflowReadinessService) {
        this.tripWorkflowReadinessService = tripWorkflowReadinessService;
    }

    @GetMapping
    public TripWorkflowReadinessResponse getWorkflowReadiness(@PathVariable Long tripId) {
        return tripWorkflowReadinessService.getReadiness(tripId);
    }
}
