package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.prize.repository.PrizeWinningRepository;
import com.myrtletrip.prize.repository.TripPlayerPayoutStatusRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.tournament.repository.TripTournamentRoundRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripBillInventoryRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripLifecycleServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    @Mock private PrizeWinningRepository prizeWinningRepository;
    @Mock private PrizeScheduleRepository prizeScheduleRepository;
    @Mock private TripPlayerPayoutStatusRepository tripPlayerPayoutStatusRepository;
    @Mock private TripBillInventoryRepository tripBillInventoryRepository;
    @Mock private TripTournamentRepository tripTournamentRepository;
    @Mock private TripTournamentRoundRepository tripTournamentRoundRepository;
    @Mock private TripPlannedRoundRepository tripPlannedRoundRepository;

    @InjectMocks
    private TripLifecycleService tripLifecycleService;

    @Test
    void archiveTrip_marksTripArchivedAndPersistsIt() {
        Trip trip = trip("MYR26");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        tripLifecycleService.archiveTrip(10L);

        assertTrue(Boolean.TRUE.equals(trip.getArchived()));
        assertNotNull(trip.getArchivedAt());
        verify(tripRepository).save(trip);
    }

    @Test
    void archiveTrip_isNoOpWhenAlreadyArchived() {
        Trip trip = trip("MYR26");
        trip.setArchived(Boolean.TRUE);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        tripLifecycleService.archiveTrip(10L);

        verify(tripRepository, never()).save(trip);
    }

    @Test
    void restoreTrip_clearsArchivedStateAndPersistsIt() {
        Trip trip = trip("MYR26");
        trip.setArchived(Boolean.TRUE);
        trip.setArchivedAt(LocalDateTime.now());
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        tripLifecycleService.restoreTrip(10L);

        assertFalse(Boolean.TRUE.equals(trip.getArchived()));
        assertNull(trip.getArchivedAt());
        verify(tripRepository).save(trip);
    }

    @Test
    void deleteTrip_rejectsTripAfterAnyRoundHasStarted() {
        Trip trip = trip("MYR26");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(roundRepository.countByTrip_Id(10L)).thenReturn(1L);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> tripLifecycleService.deleteTrip(10L));

        assertTrue(error.getMessage().contains("no started rounds"));
        verify(tripRepository, never()).delete(trip);
    }

    @Test
    void deleteTrip_removesTripOwnedDataBeforeDeletingTrip() {
        Trip trip = trip("MYR26");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(roundRepository.countByTrip_Id(10L)).thenReturn(0L);
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip)).thenReturn(Collections.emptyList());
        when(tripTournamentRepository.findByTrip_Id(10L)).thenReturn(Optional.empty());
        when(prizeScheduleRepository.findByTrip_IdOrderByIdAsc(10L)).thenReturn(Collections.emptyList());
        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip)).thenReturn(Collections.emptyList());

        tripLifecycleService.deleteTrip(10L);

        verify(prizeWinningRepository).deleteByTrip_Id(10L);
        verify(tripPlayerPayoutStatusRepository).deleteByTrip_Id(10L);
        verify(tripBillInventoryRepository).deleteByTripId(10L);
        verify(prizeScheduleRepository).flush();
        verify(tripRepository).delete(trip);
        verify(tripRepository).flush();
    }

    private Trip trip(String tripCode) {
        Trip trip = new Trip();
        trip.setTripCode(tripCode);
        trip.setName("Myrtle 2026");
        trip.setTripYear(2026);
        trip.setStatus(TripStatus.PLANNING);
        trip.setInitialized(Boolean.FALSE);
        trip.setArchived(Boolean.FALSE);
        return trip;
    }
}
