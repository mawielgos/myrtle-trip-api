package com.myrtletrip.handicap.service;

import com.myrtletrip.handicap.dto.PlayerTripIndexResponse;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.trip.model.TripHandicapMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TripHandicapService {

    private final TripHandicapRoutingService routingService;
    private final TripHandicapCalculationService calculationService;

    public TripHandicapService(TripHandicapRoutingService routingService,
                               TripHandicapCalculationService calculationService) {
        this.routingService = routingService;
        this.calculationService = calculationService;
    }

    public List<PlayerTripIndexResponse> calculateTripIndexes(String handicapGroupCode, List<Player> players) {
        return routingService.calculateTripIndexes(handicapGroupCode, players);
    }

    public BigDecimal calculateTripIndex(Player player, String handicapGroupCode, TripHandicapMethod tripHandicapMethod) {
        return routingService.calculateTripIndex(player, handicapGroupCode, tripHandicapMethod);
    }

    public BigDecimal calculateTripIndexAsOf(Player player,
                                             String handicapGroupCode,
                                             LocalDate asOfDate,
                                             TripHandicapMethod tripHandicapMethod) {
        return routingService.calculateTripIndexAsOf(player, handicapGroupCode, asOfDate, tripHandicapMethod);
    }

    public BigDecimal calculateTripIndex(Long playerId, String handicapGroupCode) {
        return routingService.calculateTripIndex(playerId, handicapGroupCode);
    }

    public BigDecimal calculateTripIndex(Player player, String handicapGroupCode) {
        return routingService.calculateTripIndex(player, handicapGroupCode);
    }

    public BigDecimal calculateTripIndexAsOf(Long playerId, String handicapGroupCode, LocalDate asOfDate) {
        return routingService.calculateTripIndexAsOf(playerId, handicapGroupCode, asOfDate);
    }

    public BigDecimal calculateTripIndexAsOf(Player player, String handicapGroupCode, LocalDate asOfDate) {
        return routingService.calculateTripIndexAsOf(player, handicapGroupCode, asOfDate);
    }

    public BigDecimal calculateGhinTripIndex(Player player, String handicapGroupCode) {
        return calculationService.calculateGhinTripIndex(player, handicapGroupCode);
    }

    public BigDecimal calculateGhinTripIndexAsOf(Player player, String handicapGroupCode, LocalDate asOfDate) {
        return calculationService.calculateGhinTripIndexAsOf(player, handicapGroupCode, asOfDate);
    }

    public BigDecimal calculateDbScoreHistoryTripIndex(Player player, String handicapGroupCode) {
        return calculationService.calculateDbScoreHistoryTripIndex(player, handicapGroupCode);
    }

    public BigDecimal calculateDbScoreHistoryTripIndexAsOf(Player player, String handicapGroupCode, LocalDate asOfDate) {
        return calculationService.calculateDbScoreHistoryTripIndexAsOf(player, handicapGroupCode, asOfDate);
    }

    public BigDecimal calculateCombinedGhinAndDbScoreHistoryTripIndex(Player player, String handicapGroupCode) {
        return calculationService.calculateCombinedGhinAndDbScoreHistoryTripIndex(player, handicapGroupCode);
    }

    public BigDecimal calculateCombinedGhinAndDbScoreHistoryTripIndexAsOf(Player player,
                                                                          String handicapGroupCode,
                                                                          LocalDate asOfDate) {
        return calculationService.calculateCombinedGhinAndDbScoreHistoryTripIndexAsOf(player, handicapGroupCode, asOfDate);
    }
}
