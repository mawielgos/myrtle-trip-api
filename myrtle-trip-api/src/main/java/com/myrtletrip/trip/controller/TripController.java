package com.myrtletrip.trip.controller;

import com.myrtletrip.trip.dto.GhinFixRowResponse;
import com.myrtletrip.trip.dto.SaveGhinFixRequest;
import com.myrtletrip.trip.dto.SaveTripPlannedRoundsRequest;
import com.myrtletrip.trip.dto.TripDetailResponse;
import com.myrtletrip.trip.dto.SaveTripBillInventoryRequest;
import com.myrtletrip.trip.dto.TripBillInventoryResponse;
import com.myrtletrip.trip.dto.TripListResponse;
import com.myrtletrip.trip.dto.TripPlannedRoundResponse;
import com.myrtletrip.trip.dto.TripPlayerResponse;
import com.myrtletrip.trip.dto.TripRoundListResponse;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.scorehistory.dto.ManualScoreHistoryEntryResponse;
import com.myrtletrip.scorehistory.dto.SaveManualScoreHistoryEntryRequest;
import com.myrtletrip.scorehistory.service.ManualScoreHistoryService;
import com.myrtletrip.standings.dto.FourDayStandingsResponse;
import com.myrtletrip.standings.service.FourDayStandingsService;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.service.TripGhinFixService;
import com.myrtletrip.trip.service.TripBillInventoryService;
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
    private final TripBillInventoryService tripBillInventoryService;
    private final ManualScoreHistoryService manualScoreHistoryService;
    
    public TripController(TripService tripService,
            TripInitializationService tripInitializationService,
            TripGhinFixService tripGhinFixService,
            FourDayStandingsService fourDayStandingsService,
            TripBillInventoryService tripBillInventoryService,
            ManualScoreHistoryService manualScoreHistoryService) {
    	
		this.tripService = tripService;
		this.tripInitializationService = tripInitializationService;
		this.tripGhinFixService = tripGhinFixService;
		this.fourDayStandingsService = fourDayStandingsService;
        this.tripBillInventoryService = tripBillInventoryService;
        this.manualScoreHistoryService = manualScoreHistoryService;
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

    @GetMapping("/{tripId}/bill-inventory")
    public TripBillInventoryResponse getTripBillInventory(@PathVariable Long tripId) {
        return tripBillInventoryService.getBillInventory(tripId);
    }

    @PutMapping("/{tripId}/bill-inventory")
    public TripBillInventoryResponse saveTripBillInventory(@PathVariable Long tripId,
                                                           @RequestBody SaveTripBillInventoryRequest request) {
        return tripBillInventoryService.saveBillInventory(tripId, request);
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

    @GetMapping("/{tripId}/manual-score-history")
    public List<ManualScoreHistoryEntryResponse> getManualScoreHistory(@PathVariable Long tripId) {
        return manualScoreHistoryService.getManualEntries(tripId);
    }

    @PostMapping("/{tripId}/manual-score-history")
    public ManualScoreHistoryEntryResponse createManualScoreHistory(@PathVariable Long tripId,
                                                                    @RequestBody SaveManualScoreHistoryEntryRequest request) {
        return manualScoreHistoryService.createManualEntry(tripId, request);
    }

    @PostMapping("/{tripId}/manual-score-history/bulk")
    public List<ManualScoreHistoryEntryResponse> createManualScoreHistoryBulk(@PathVariable Long tripId,
                                                                              @RequestBody List<SaveManualScoreHistoryEntryRequest> requests) {
        return manualScoreHistoryService.createManualEntries(tripId, requests);
    }

    @PutMapping("/{tripId}/manual-score-history/{scoreHistoryEntryId}")
    public ManualScoreHistoryEntryResponse updateManualScoreHistory(@PathVariable Long tripId,
                                                                    @PathVariable Long scoreHistoryEntryId,
                                                                    @RequestBody SaveManualScoreHistoryEntryRequest request) {
        return manualScoreHistoryService.updateManualEntry(tripId, scoreHistoryEntryId, request);
    }

    @DeleteMapping("/{tripId}/manual-score-history/{scoreHistoryEntryId}")
    public void deleteManualScoreHistory(@PathVariable Long tripId,
                                         @PathVariable Long scoreHistoryEntryId) {
        manualScoreHistoryService.deleteManualEntry(tripId, scoreHistoryEntryId);
    }

    @DeleteMapping("/{tripId}")
    public void deleteTrip(@PathVariable Long tripId) {
        tripService.deleteTrip(tripId);
    }
}
