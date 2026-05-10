package com.myrtletrip.handicap.card.controller;

import com.myrtletrip.handicap.card.dto.HandicapCardListResponse;
import com.myrtletrip.handicap.card.dto.HandicapCardPlayerResponse;
import com.myrtletrip.handicap.card.service.HandicapCardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/trips/{tripId}/handicap-cards")
public class HandicapCardController {

    private final HandicapCardService handicapCardService;

    public HandicapCardController(HandicapCardService handicapCardService) {
        this.handicapCardService = handicapCardService;
    }

    @GetMapping
    public HandicapCardListResponse getTripHandicapCards(
            @PathVariable Long tripId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate
    ) {
        return handicapCardService.getTripHandicapCards(tripId, asOfDate);
    }

    @GetMapping("/players/{playerId}")
    public HandicapCardPlayerResponse getPlayerHandicapCard(
            @PathVariable Long tripId,
            @PathVariable Long playerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate
    ) {
        return handicapCardService.getPlayerHandicapCard(tripId, playerId, asOfDate);
    }
}
