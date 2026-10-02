package com.myrtletrip.handicap.card.service;

import com.myrtletrip.handicap.card.dto.HandicapCardListResponse;
import com.myrtletrip.handicap.card.dto.HandicapCardPlayerResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class HandicapCardService {

    private final HandicapCardReadModelService readModelService;

    public HandicapCardService(HandicapCardReadModelService readModelService) {
        this.readModelService = readModelService;
    }

    public HandicapCardListResponse getTripHandicapCards(Long tripId, LocalDate asOfDate) {
        return readModelService.getTripHandicapCards(tripId, asOfDate);
    }

    public HandicapCardPlayerResponse getPlayerHandicapCard(Long tripId, Long playerId, LocalDate asOfDate) {
        return readModelService.getPlayerHandicapCard(tripId, playerId, asOfDate);
    }
}
