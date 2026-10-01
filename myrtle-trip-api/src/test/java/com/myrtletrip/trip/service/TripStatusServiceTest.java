package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripStatusServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private RoundRepository roundRepository;

    @InjectMocks
    private TripStatusService tripStatusService;

    @Test
    void findCurrentRoundEntity_returnsFirstNonFinalizedRound() {
        Round finalized = mock(Round.class);
        Round current = mock(Round.class);
        Round later = mock(Round.class);
        when(finalized.getFinalized()).thenReturn(Boolean.TRUE);
        when(current.getFinalized()).thenReturn(Boolean.FALSE);
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(10L))
                .thenReturn(List.of(finalized, current, later));

        Round result = tripStatusService.findCurrentRoundEntity(10L);

        assertSame(current, result);
    }

    @Test
    void findCurrentRoundEntity_returnsNullWhenAllRoundsFinalized() {
        Round first = mock(Round.class);
        Round second = mock(Round.class);
        when(first.getFinalized()).thenReturn(Boolean.TRUE);
        when(second.getFinalized()).thenReturn(Boolean.TRUE);
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(10L))
                .thenReturn(List.of(first, second));

        assertNull(tripStatusService.findCurrentRoundEntity(10L));
    }

    @Test
    void refreshTripStatusFromRounds_initializedTripBecomesInProgress() {
        Trip trip = trip(TripStatus.PLANNING, true);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        tripStatusService.refreshTripStatusFromRounds(10L);

        assertTrue(trip.getStatus() == TripStatus.IN_PROGRESS);
        verify(tripRepository).save(trip);
    }

    @Test
    void refreshTripStatusFromRounds_completeTripRemainsComplete() {
        Trip trip = trip(TripStatus.COMPLETE, true);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        tripStatusService.refreshTripStatusFromRounds(10L);

        assertTrue(trip.getStatus() == TripStatus.COMPLETE);
        verify(tripRepository).save(trip);
    }

    @Test
    void refreshTripStatusFromRounds_rejectsUnknownTrip() {
        when(tripRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tripStatusService.refreshTripStatusFromRounds(10L));

        assertTrue(error.getMessage().contains("Trip not found: 10"));
    }

    private Trip trip(TripStatus status, boolean initialized) {
        Trip trip = new Trip();
        trip.setStatus(status);
        trip.setInitialized(initialized);
        return trip;
    }
}
