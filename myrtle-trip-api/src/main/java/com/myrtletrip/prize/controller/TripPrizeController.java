package com.myrtletrip.prize.controller;

import com.myrtletrip.prize.dto.PrizeRecalculationResponse;
import com.myrtletrip.prize.dto.PrizeScheduleResponse;
import com.myrtletrip.prize.dto.SaveTripPrizeSchedulesRequest;
import com.myrtletrip.prize.dto.UpdatePlayerPayoutStatusRequest;
import com.myrtletrip.prize.service.TripPrizeRecalculationService;
import com.myrtletrip.prize.service.TripPrizeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/prizes")
public class TripPrizeController {

    private final TripPrizeService tripPrizeService;
    private final TripPrizeRecalculationService tripPrizeRecalculationService;

    public TripPrizeController(TripPrizeService tripPrizeService,
                               TripPrizeRecalculationService tripPrizeRecalculationService) {
        this.tripPrizeService = tripPrizeService;
        this.tripPrizeRecalculationService = tripPrizeRecalculationService;
    }

    @GetMapping
    public List<PrizeScheduleResponse> getPrizeSchedules(@PathVariable Long tripId) {
        return tripPrizeService.getPrizeSchedules(tripId);
    }

    @PutMapping
    public List<PrizeScheduleResponse> savePrizeSchedules(@PathVariable Long tripId,
                                                          @RequestBody SaveTripPrizeSchedulesRequest request) {
        return tripPrizeService.savePrizeSchedules(tripId, request);
    }

    @GetMapping("/winnings")
    public PrizeRecalculationResponse getCurrentWinnings(@PathVariable Long tripId) {
        return tripPrizeRecalculationService.getCurrentWinnings(tripId);
    }

    @PostMapping("/recalculate")
    public PrizeRecalculationResponse recalculatePrizeWinnings(@PathVariable Long tripId) {
        return tripPrizeRecalculationService.recalculate(tripId);
    }

    @PutMapping("/players/{playerId}/paid")
    public PrizeRecalculationResponse updatePlayerPayoutStatus(@PathVariable Long tripId,
                                                               @PathVariable Long playerId,
                                                               @RequestBody UpdatePlayerPayoutStatusRequest request) {
        Boolean paid = request == null ? Boolean.FALSE : request.getPaid();
        return tripPrizeRecalculationService.updatePlayerPayoutStatus(tripId, playerId, paid);
    }

}
