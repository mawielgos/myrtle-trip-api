package com.myrtletrip.trip.controller;

import com.myrtletrip.trip.dto.GhinFixRowResponse;
import com.myrtletrip.trip.dto.SaveGhinFixRequest;
import com.myrtletrip.trip.dto.SaveTripPlannedRoundsRequest;
import com.myrtletrip.trip.dto.TripDetailResponse;
import com.myrtletrip.trip.dto.TripListResponse;
import com.myrtletrip.trip.dto.TripPlannedRoundResponse;
import com.myrtletrip.trip.dto.TripPlayerResponse;
import com.myrtletrip.trip.dto.TripRoundListResponse;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.standings.dto.FourDayStandingsResponse;
import com.myrtletrip.standings.service.FourDayStandingsService;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.service.TripGhinFixService;
import com.myrtletrip.trip.service.TripInitializationService;
import com.myrtletrip.trip.service.TripService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final TripInitializationService tripInitializationService;
    private final TripGhinFixService tripGhinFixService;
    private final FourDayStandingsService fourDayStandingsService;
    
    public TripController(TripService tripService,
            TripInitializationService tripInitializationService,
            TripGhinFixService tripGhinFixService,
            FourDayStandingsService fourDayStandingsService) {
    	
		this.tripService = tripService;
		this.tripInitializationService = tripInitializationService;
		this.tripGhinFixService = tripGhinFixService;
		this.fourDayStandingsService = fourDayStandingsService;
    }
    @PostMapping
    public Long createTrip(@RequestBody TripSetupRequest request) {
        request.setTripId(null);
        Trip trip = tripService.createOrUpdateTripRoster(request);
        return trip.getId();
    }

    @PutMapping("/{tripId}")
    public Long updateTrip(@PathVariable Long tripId,
                           @RequestBody TripSetupRequest request) {
        request.setTripId(tripId);
        Trip trip = tripService.createOrUpdateTripRoster(request);
        return trip.getId();
    }

    @PostMapping("/setup")
    public Long createOrUpdateTripRoster(@RequestBody TripSetupRequest request) {
        Trip trip = tripService.createOrUpdateTripRoster(request);
        return trip.getId();
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

    @GetMapping("/{tripId}/planned-rounds")
    public List<TripPlannedRoundResponse> getPlannedRounds(@PathVariable Long tripId) {
        return tripService.getPlannedRounds(tripId);
    }

    @PutMapping("/{tripId}/planned-rounds")
    public List<TripPlannedRoundResponse> savePlannedRounds(@PathVariable Long tripId,
                                                            @RequestBody SaveTripPlannedRoundsRequest request) {
        return tripService.savePlannedRounds(tripId, request);
    }

    @GetMapping("/{tripId}/rounds")
    public List<TripRoundListResponse> getTripRounds(@PathVariable Long tripId) {
        return tripService.getTripRounds(tripId);
    }

    @GetMapping("/{tripId}/four-day-standings")
    public FourDayStandingsResponse getFourDayStandings(@PathVariable Long tripId) {
        return fourDayStandingsService.getFourDayStandings(tripId);
    }
    
    @PostMapping("/{tripId}/initialize-ghin")
    public void initializeTripGhin(@PathVariable Long tripId) throws Exception {
        tripService.initializeTripGhin(tripId);
    }

    @GetMapping("/{tripId}/ghin-fixes")
    public List<GhinFixRowResponse> getOutstandingGhinFixes(@PathVariable Long tripId) {
        return tripGhinFixService.getOutstandingFixes(tripId);
    }

    @PutMapping("/{tripId}/ghin-fixes/{scoreHistoryEntryId}")
    public GhinFixRowResponse saveGhinFix(@PathVariable Long tripId,
                                          @PathVariable Long scoreHistoryEntryId,
                                          @RequestBody SaveGhinFixRequest request) {
        return tripGhinFixService.saveFix(tripId, scoreHistoryEntryId, request);
    }

    @PostMapping("/{tripId}/initialize")
    public void initializeTrip(@PathVariable Long tripId) throws Exception {
        tripInitializationService.initializeTrip(tripId);
    }

    @DeleteMapping("/{tripId}")
    public void deleteTrip(@PathVariable Long tripId) {
        tripService.deleteTrip(tripId);
    }
}
