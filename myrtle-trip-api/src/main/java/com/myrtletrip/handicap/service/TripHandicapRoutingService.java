package com.myrtletrip.handicap.service;

import com.myrtletrip.handicap.dto.PlayerTripIndexResponse;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.trip.model.TripHandicapMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TripHandicapRoutingService {

    private static final String HANDICAP_METHOD_GHIN = "GHIN";
    private static final String HANDICAP_METHOD_MYRTLE_BEACH = "MYRTLE_BEACH";
    private static final String HANDICAP_METHOD_DB_SCORE_HISTORY = "DB_SCORE_HISTORY";

    private final PlayerRepository playerRepository;
    private final TripHandicapCalculationService calculationService;

    public TripHandicapRoutingService(PlayerRepository playerRepository,
                                      TripHandicapCalculationService calculationService) {
        this.playerRepository = playerRepository;
        this.calculationService = calculationService;
    }

    public List<PlayerTripIndexResponse> calculateTripIndexes(String handicapGroupCode, List<Player> players) {
        if (handicapGroupCode == null || handicapGroupCode.isBlank()) {
            throw new IllegalArgumentException("handicapGroupCode is required");
        }
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("players are required");
        }

        return players.stream().map(player -> {
            PlayerTripIndexResponse response = new PlayerTripIndexResponse();
            response.setPlayerId(player.getId());
            response.setPlayerName(player.getDisplayName());
            response.setHandicapMethod(player.getHandicapMethod());
            response.setTripIndex(calculateTripIndex(player, handicapGroupCode));
            return response;
        }).toList();
    }

    public BigDecimal calculateTripIndex(Player player,
                                         String handicapGroupCode,
                                         TripHandicapMethod tripHandicapMethod) {
        validateInputs(player, handicapGroupCode);

        TripHandicapMethod effectiveMethod = tripHandicapMethod == null
                ? TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY
                : tripHandicapMethod;

        if (TripHandicapMethod.GHIN_HISTORY.equals(effectiveMethod)) {
            return calculationService.calculateGhinTripIndex(player, handicapGroupCode);
        }
        if (TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY.equals(effectiveMethod)) {
            return calculationService.calculateCombinedGhinAndDbScoreHistoryTripIndex(player, handicapGroupCode);
        }
        throw new IllegalStateException("Unsupported non-frozen trip handicap method: " + effectiveMethod);
    }

    public BigDecimal calculateTripIndexAsOf(Player player,
                                             String handicapGroupCode,
                                             LocalDate asOfDate,
                                             TripHandicapMethod tripHandicapMethod) {
        validateInputs(player, handicapGroupCode);
        validateAsOfDate(asOfDate);

        TripHandicapMethod effectiveMethod = tripHandicapMethod == null
                ? TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY
                : tripHandicapMethod;

        if (TripHandicapMethod.GHIN_HISTORY.equals(effectiveMethod)) {
            return calculationService.calculateGhinTripIndexAsOf(player, handicapGroupCode, asOfDate);
        }
        if (TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY.equals(effectiveMethod)) {
            return calculationService.calculateCombinedGhinAndDbScoreHistoryTripIndexAsOf(player, handicapGroupCode, asOfDate);
        }
        throw new IllegalStateException("Unsupported non-frozen trip handicap method: " + effectiveMethod);
    }

    public BigDecimal calculateTripIndex(Long playerId, String handicapGroupCode) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
        return calculateTripIndex(player, handicapGroupCode);
    }

    public BigDecimal calculateTripIndex(Player player, String handicapGroupCode) {
        validateInputs(player, handicapGroupCode);
        String handicapMethod = normalize(player.getHandicapMethod());

        if (HANDICAP_METHOD_GHIN.equals(handicapMethod)) {
            return calculationService.calculateGhinTripIndex(player, handicapGroupCode);
        }
        if (isDbScoreHistoryMethod(handicapMethod)) {
            return calculationService.calculateDbScoreHistoryTripIndex(player, handicapGroupCode);
        }
        throw new IllegalStateException("Unsupported handicap method for player "
                + player.getId() + ": " + player.getHandicapMethod());
    }

    public BigDecimal calculateTripIndexAsOf(Long playerId,
                                             String handicapGroupCode,
                                             LocalDate asOfDate) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
        return calculateTripIndexAsOf(player, handicapGroupCode, asOfDate);
    }

    public BigDecimal calculateTripIndexAsOf(Player player,
                                             String handicapGroupCode,
                                             LocalDate asOfDate) {
        validateInputs(player, handicapGroupCode);
        validateAsOfDate(asOfDate);
        String handicapMethod = normalize(player.getHandicapMethod());

        if (HANDICAP_METHOD_GHIN.equals(handicapMethod)) {
            return calculationService.calculateGhinTripIndexAsOf(player, handicapGroupCode, asOfDate);
        }
        if (isDbScoreHistoryMethod(handicapMethod)) {
            return calculationService.calculateDbScoreHistoryTripIndexAsOf(player, handicapGroupCode, asOfDate);
        }
        throw new IllegalStateException("Unsupported handicap method for player "
                + player.getId() + ": " + player.getHandicapMethod());
    }

    private boolean isDbScoreHistoryMethod(String handicapMethod) {
        return HANDICAP_METHOD_DB_SCORE_HISTORY.equals(handicapMethod)
                || HANDICAP_METHOD_MYRTLE_BEACH.equals(handicapMethod);
    }

    private void validateInputs(Player player, String handicapGroupCode) {
        if (player == null) {
            throw new IllegalArgumentException("Player is required");
        }
        if (handicapGroupCode == null || handicapGroupCode.isBlank()) {
            throw new IllegalArgumentException("handicapGroupCode is required");
        }
    }

    private void validateAsOfDate(LocalDate asOfDate) {
        if (asOfDate == null) {
            throw new IllegalArgumentException("asOfDate is required");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
