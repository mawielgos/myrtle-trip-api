package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.handicap.source.frozen.FrozenGhinImportService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripGhinInitializationServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;
    @Mock private FrozenGhinImportService frozenGhinImportService;

    @InjectMocks
    private TripGhinInitializationService service;

    @Test
    void initializeTripGhin_rejectsTripThatHasAlreadyStarted() {
        Trip trip = trip("MYR26");
        trip.setStatus(TripStatus.IN_PROGRESS);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.initializeTripGhin(10L));

        assertTrue(error.getMessage().contains("cannot be loaded after the trip has started"));
    }

    @Test
    void initializeTripGhin_requiresPlayersBeforeImport() {
        Trip trip = trip("MYR26");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip))
                .thenReturn(Collections.emptyList());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeTripGhin(10L));

        assertTrue(error.getMessage().contains("must have players"));
    }

    @Test
    void initializeTripGhin_rejectsRosterWithNoActiveGhinPlayers() {
        Trip trip = trip("MYR26");
        TripPlayer tripPlayer = mock(TripPlayer.class);
        Player player = mock(Player.class);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip))
                .thenReturn(List.of(tripPlayer));
        when(tripPlayer.getPlayer()).thenReturn(player);
        when(player.isActive()).thenReturn(true);
        when(player.getHandicapMethod()).thenReturn("MANUAL");
        when(player.getGhinNumber()).thenReturn(null);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeTripGhin(10L));

        assertTrue(error.getMessage().contains("No active GHIN players"));
    }

    @Test
    void initializeTripGhin_importsActiveGhinPlayersAndNormalizesMethod() throws Exception {
        Trip trip = trip("MYR26");

        TripPlayer withNumberTripPlayer = mock(TripPlayer.class);
        Player withNumber = mock(Player.class);
        when(withNumberTripPlayer.getPlayer()).thenReturn(withNumber);
        when(withNumber.isActive()).thenReturn(true);
        when(withNumber.getHandicapMethod()).thenReturn("MANUAL");
        when(withNumber.getGhinNumber()).thenReturn("1234567");

        TripPlayer withMethodTripPlayer = mock(TripPlayer.class);
        Player withMethod = mock(Player.class);
        when(withMethodTripPlayer.getPlayer()).thenReturn(withMethod);
        when(withMethod.isActive()).thenReturn(true);
        when(withMethod.getHandicapMethod()).thenReturn("GHIN");
        when(withMethod.getGhinNumber()).thenReturn(null);

        TripPlayer inactiveTripPlayer = mock(TripPlayer.class);
        Player inactive = mock(Player.class);
        when(inactiveTripPlayer.getPlayer()).thenReturn(inactive);
        when(inactive.isActive()).thenReturn(false);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip))
                .thenReturn(List.of(withNumberTripPlayer, withMethodTripPlayer, inactiveTripPlayer));

        service.initializeTripGhin(10L);

        verify(withNumber).setHandicapMethod("GHIN");
        verify(frozenGhinImportService).initializeFrozenGhinForPlayers(
                argThat(players -> players.size() == 2
                        && players.get(0) == withNumber
                        && players.get(1) == withMethod),
                eq("MYR26"));
    }

    private Trip trip(String tripCode) {
        Trip trip = new Trip();
        trip.setTripCode(tripCode);
        trip.setStatus(TripStatus.PLANNING);
        trip.setInitialized(Boolean.FALSE);
        return trip;
    }
}
