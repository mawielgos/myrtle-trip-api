package com.myrtletrip.handicap.controller;

import com.myrtletrip.handicap.service.TripInitializationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/handicap")
public class HandicapController {

    private final TripInitializationService tripInitializationService;

    public HandicapController(TripInitializationService tripInitializationService) {
        this.tripInitializationService = tripInitializationService;
    }

    @PostMapping("/trips/{tripId}/initialize")
    public String initializeTrip(@PathVariable Long tripId) throws Exception {
        tripInitializationService.initializeTrip(tripId);
        return "Trip initialized for tripId=" + tripId;
    }
}