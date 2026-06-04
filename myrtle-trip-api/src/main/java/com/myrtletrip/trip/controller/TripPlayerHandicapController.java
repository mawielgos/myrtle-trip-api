package com.myrtletrip.trip.controller;

import com.myrtletrip.trip.dto.SaveTripPlayerFrozenIndexRequest;
import com.myrtletrip.trip.service.TripPlayerHandicapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips/{tripId}/players")
@CrossOrigin
public class TripPlayerHandicapController {

    private final TripPlayerHandicapService tripPlayerHandicapService;

    public TripPlayerHandicapController(TripPlayerHandicapService tripPlayerHandicapService) {
        this.tripPlayerHandicapService = tripPlayerHandicapService;
    }

    @PutMapping("/{playerId}/frozen-index")
    public ResponseEntity<Void> saveFrozenHandicapIndex(
            @PathVariable Long tripId,
            @PathVariable Long playerId,
            @RequestBody SaveTripPlayerFrozenIndexRequest request
    ) {
        tripPlayerHandicapService.saveFrozenHandicapIndex(
                tripId,
                playerId,
                request.getFrozenHandicapIndex()
        );

        return ResponseEntity.ok().build();
    }
}
