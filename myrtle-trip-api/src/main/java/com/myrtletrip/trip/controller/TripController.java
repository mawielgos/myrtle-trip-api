package com.myrtletrip.trip.controller;

import com.myrtletrip.trip.dto.TripDetailResponse;
import com.myrtletrip.trip.dto.TripListResponse;
import com.myrtletrip.trip.dto.TripPlayerResponse;
import com.myrtletrip.trip.dto.TripRoundListResponse;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.service.TripService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/setup")
    public Trip createOrUpdateTripRoster(@RequestBody TripSetupRequest request) {
        return tripService.createOrUpdateTripRoster(request);
    }

    @GetMapping
    public List<TripListResponse> getTrips() {
        return tripService.getTrips();
    }

    @GetMapping("/{tripId}")
    public TripDetailResponse getTrip(@PathVariable Long tripId) {
        return tripService.getTrip(tripId);
    }

    @GetMapping("/{tripId}/players")
    public List<TripPlayerResponse> getTripPlayers(@PathVariable Long tripId) {
        return tripService.getTripPlayers(tripId);
    }

    @GetMapping("/{tripId}/rounds")
    public List<TripRoundListResponse> getTripRounds(@PathVariable Long tripId) {
        return tripService.getTripRounds(tripId);
    }
}