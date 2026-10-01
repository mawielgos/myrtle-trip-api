package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.dto.TripReadinessResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.model.TripHandicapMethod;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripReadinessServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;
    @Mock private TripPlannedRoundService tripPlannedRoundService;
    @Mock private ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    @Mock private TripHandicapService tripHandicapService;

    @InjectMocks
    private TripReadinessService service;

    @Test
    void getTripReadiness_missingTripThrows() {
        when(tripRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> service.getTripReadiness(10L));

        assertTrue(ex.getMessage().contains("Trip not found"));
    }

    @Test
    void getTripReadiness_noActivePlayersAndNoRoundsIsBlocked() {
        Trip trip = trip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip))
                .thenReturn(Collections.emptyList());
        when(tripPlannedRoundService.loadActivePlannedRounds(trip))
                .thenReturn(Collections.emptyList());
        when(scoreHistoryEntryRepository
                .countByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrue(
                        "MYR26", "GHIN_FROZEN"))
                .thenReturn(0L);

        TripReadinessResponse response = service.getTripReadiness(10L);

        assertFalse(response.getRosterReady());
        assertFalse(response.getPlannedRoundsReady());
        assertFalse(response.getCanStartTrip());
        assertEquals(2, response.getBlockingItems().size());
    }

    @Test
    void getTripReadiness_completeRosterRoundsAndHandicapCanStart() throws Exception {
        Trip trip = trip();
        Player player = activePlayer();
        TripPlayer tripPlayer = tripPlayer(player);

        TripPlannedRound plannedRound = completePlannedRound();

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip))
                .thenReturn(List.of(tripPlayer));
        when(tripPlannedRoundService.loadActivePlannedRounds(trip))
                .thenReturn(List.of(plannedRound));
        when(tripPlannedRoundService.hasPlannedRoundEventConfiguration(plannedRound))
                .thenReturn(true);
        when(scoreHistoryEntryRepository
                .countByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrue(
                        "MYR26", "GHIN_FROZEN"))
                .thenReturn(0L);
        when(tripHandicapService.calculateTripIndex(
                player, "MYR26", TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY))
                .thenReturn(new BigDecimal("10.2"));

        TripReadinessResponse response = service.getTripReadiness(10L);

        assertEquals(1, response.getActivePlayerCount());
        assertEquals(1, response.getPlannedRoundCount());
        assertEquals(1, response.getCompletedPlannedRoundCount());
        assertTrue(response.getRosterReady());
        assertTrue(response.getPlannedRoundsReady());
        assertTrue(response.getGhinFixesReady());
        assertTrue(response.getHandicapIndexesReady());
        assertTrue(response.getCanStartTrip());
        assertTrue(response.getBlockingItems().isEmpty());
    }

    @Test
    void getTripReadiness_frozenGhinRequiresFrozenIndex() {
        Trip trip = trip();
        trip.setHandicapMethod(TripHandicapMethod.FROZEN_GHIN_INDEX);

        Player player = activePlayer();
        TripPlayer tripPlayer = tripPlayer(player);
        tripPlayer.setFrozenHandicapIndex(null);

        TripPlannedRound plannedRound = completePlannedRound();

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip))
                .thenReturn(List.of(tripPlayer));
        when(tripPlannedRoundService.loadActivePlannedRounds(trip))
                .thenReturn(List.of(plannedRound));
        when(tripPlannedRoundService.hasPlannedRoundEventConfiguration(plannedRound))
                .thenReturn(true);

        TripReadinessResponse response = service.getTripReadiness(10L);

        assertFalse(response.getHandicapIndexesReady());
        assertFalse(response.getCanStartTrip());
        assertTrue(response.getBlockingItems().stream()
                .anyMatch(message -> message.contains("frozen GHIN handicap index")));
    }

    @Test
    void validateTripCanStart_reportsBlockingItems() {
        Trip trip = trip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip))
                .thenReturn(Collections.emptyList());
        when(tripPlannedRoundService.loadActivePlannedRounds(trip))
                .thenReturn(Collections.emptyList());
        when(scoreHistoryEntryRepository
                .countByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrue(
                        "MYR26", "GHIN_FROZEN"))
                .thenReturn(0L);

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> service.validateTripCanStart(10L));

        assertTrue(ex.getMessage().contains("Add at least one active player"));
        assertTrue(ex.getMessage().contains("at least one planned round"));
    }

    @Test
    void getTripReadiness_startedTripCannotStartAgain() throws Exception {
        Trip trip = trip();
        trip.setInitialized(Boolean.TRUE);
        trip.setStatus(TripStatus.IN_PROGRESS);

        Player player = activePlayer();
        TripPlayer tripPlayer = tripPlayer(player);
        TripPlannedRound plannedRound = completePlannedRound();

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip))
                .thenReturn(List.of(tripPlayer));
        when(tripPlannedRoundService.loadActivePlannedRounds(trip))
                .thenReturn(List.of(plannedRound));
        when(tripPlannedRoundService.hasPlannedRoundEventConfiguration(plannedRound))
                .thenReturn(true);
        when(scoreHistoryEntryRepository
                .countByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrue(
                        "MYR26", "GHIN_FROZEN"))
                .thenReturn(0L);
        when(tripHandicapService.calculateTripIndex(
                player, "MYR26", TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY))
                .thenReturn(new BigDecimal("10.2"));

        TripReadinessResponse response = service.getTripReadiness(10L);

        assertFalse(response.getCanStartTrip());
        assertTrue(response.getBlockingItems().contains("Trip has already been started."));
    }

    private Trip trip() {
        Trip trip = new Trip();
        trip.setTripCode("MYR26");
        trip.setName("Myrtle 2026");
        trip.setTripYear(2026);
        trip.setInitialized(Boolean.FALSE);
        trip.setStatus(TripStatus.PLANNING);
        trip.setHandicapsEnabled(Boolean.TRUE);
        trip.setHandicapMethod(TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY);
        return trip;
    }

    private Player activePlayer() {
        Player player = new Player();
        player.setId(20L);
        player.setDisplayName("Player One");
        player.setActive(true);
        return player;
    }

    private TripPlayer tripPlayer(Player player) {
        TripPlayer tripPlayer = new TripPlayer();
        tripPlayer.setPlayer(player);
        tripPlayer.setDisplayOrder(1);
        return tripPlayer;
    }

    private TripPlannedRound completePlannedRound() {
        TripPlannedRound plannedRound = new TripPlannedRound();
        plannedRound.setRoundNumber(1);
        plannedRound.setRoundDate(LocalDate.of(2026, 10, 1));
        plannedRound.setCourseId(100L);
        plannedRound.setStandardTeeId(200L);
        return plannedRound;
    }
}
