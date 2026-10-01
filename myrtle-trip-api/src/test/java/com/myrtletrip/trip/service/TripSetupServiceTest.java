package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.model.TripHandicapMethod;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripSetupServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    @Mock private TripPlannedRoundService tripPlannedRoundService;

    @InjectMocks
    private TripSetupService tripSetupService;

    @Test
    void createOrUpdateTripRoster_requiresTripCode() {
        TripSetupRequest request = validSetupRequest();
        request.setTripCode(" ");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tripSetupService.createOrUpdateTripRoster(request));

        assertTrue(error.getMessage().contains("tripCode is required"));
    }

    @Test
    void createOrUpdateTripRoster_requiresName() {
        TripSetupRequest request = validSetupRequest();
        request.setName(null);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tripSetupService.createOrUpdateTripRoster(request));

        assertTrue(error.getMessage().contains("name is required"));
    }

    @Test
    void createOrUpdateTripRoster_requiresTripYear() {
        TripSetupRequest request = validSetupRequest();
        request.setTripYear(null);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tripSetupService.createOrUpdateTripRoster(request));

        assertTrue(error.getMessage().contains("tripYear is required"));
    }

    @Test
    void createOrUpdateTripRoster_rejectsDuplicatePlayerIds() {
        TripSetupRequest request = validSetupRequest();
        request.setPlayerIds(List.of(7L, 7L));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tripSetupService.createOrUpdateTripRoster(request));

        assertTrue(error.getMessage().contains("Duplicate playerId: 7"));
    }

    @Test
    void createOrUpdateTripRoster_createsNewTripRosterAndDefaultRounds() {
        TripSetupRequest request = validSetupRequest();
        request.setPlayerIds(List.of(7L));
        when(tripPlannedRoundService.resolvePlannedRoundCount(null)).thenReturn(5);
        when(tripRepository.findByTripCode("MYR26")).thenReturn(Optional.empty());

        Trip savedTrip = new Trip();
        savedTrip.setTripCode("MYR26");
        savedTrip.setName("Myrtle 2026");
        savedTrip.setTripYear(2026);
        when(tripRepository.saveAndFlush(any(Trip.class))).thenReturn(savedTrip);
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(savedTrip))
                .thenReturn(Collections.emptyList());

        Player player = mock(Player.class);
        when(player.isActive()).thenReturn(true);
        when(playerRepository.findById(7L)).thenReturn(Optional.of(player));

        Trip result = tripSetupService.createOrUpdateTripRoster(request);

        assertTrue(result == savedTrip);
        verify(tripPlannedRoundService).createDefaultPlannedRounds(savedTrip);

        ArgumentCaptor<TripPlayer> captor = ArgumentCaptor.forClass(TripPlayer.class);
        verify(tripPlayerRepository).save(captor.capture());
        assertTrue(captor.getValue().getPlayer() == player);
        assertEquals(1, captor.getValue().getDisplayOrder());
    }

    @Test
    void createOrUpdateTripRoster_requiresFrozenIndexForFrozenMethod() {
        TripSetupRequest request = validSetupRequest();
        request.setHandicapMethod(TripHandicapMethod.FROZEN_GHIN_INDEX.name());
        request.setPlayerIds(List.of(7L));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tripSetupService.createOrUpdateTripRoster(request));

        assertTrue(error.getMessage().contains("Frozen GHIN Index is required"));
    }

    private TripSetupRequest validSetupRequest() {
        TripSetupRequest request = new TripSetupRequest();
        request.setTripCode("MYR26");
        request.setName("Myrtle 2026");
        request.setTripYear(2026);
        request.setTripStartDate(LocalDate.of(2026, 3, 1));
        request.setTripEndDate(LocalDate.of(2026, 3, 7));
        request.setPlayerIds(Collections.emptyList());
        return request;
    }
}
