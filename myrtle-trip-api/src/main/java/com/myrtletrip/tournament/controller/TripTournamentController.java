package com.myrtletrip.tournament.controller;

import com.myrtletrip.tournament.dto.SaveTripTournamentSetupRequest;
import com.myrtletrip.tournament.dto.TripTournamentSetupResponse;
import com.myrtletrip.tournament.service.TripTournamentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips/{tripId}/tournament")
public class TripTournamentController {

    private final TripTournamentService tripTournamentService;

    public TripTournamentController(TripTournamentService tripTournamentService) {
        this.tripTournamentService = tripTournamentService;
    }

    @GetMapping
    public TripTournamentSetupResponse getTournamentSetup(@PathVariable Long tripId) {
        return tripTournamentService.getTournamentSetup(tripId);
    }

    @PutMapping
    public TripTournamentSetupResponse saveTournamentSetup(@PathVariable Long tripId,
                                                           @RequestBody SaveTripTournamentSetupRequest request) {
        return tripTournamentService.saveTournamentSetup(tripId, request);
    }
}
