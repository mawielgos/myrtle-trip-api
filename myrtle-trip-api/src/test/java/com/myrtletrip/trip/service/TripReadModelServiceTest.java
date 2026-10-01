package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.service.RoundEventCapabilityService;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.dto.TripDetailResponse;
import com.myrtletrip.trip.dto.TripListResponse;
import com.myrtletrip.trip.dto.TripPlayerResponse;
import com.myrtletrip.trip.dto.TripReadinessResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.model.TripHandicapMethod;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripReadModelServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TripHandicapService tripHandicapService;
    @Mock private ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    @Mock private RoundEventCapabilityService roundEventCapabilityService;
    @Mock private TripParticipationService tripParticipationService;
    @Mock private TripStatusService tripStatusService;
    @Mock private TripPlannedRoundService tripPlannedRoundService;
    @Mock private TripReadinessService tripReadinessService;

    @InjectMocks
    private TripReadModelService service;

    @Test
    void getTrips_mapsCountsCapabilitiesAndPlannedRoundDateRange() {
        Trip trip = mock(Trip.class);
        when(trip.getId()).thenReturn(10L);
        when(trip.getName()).thenReturn("Myrtle 2026");
        when(trip.getTripCode()).thenReturn("MYR26");
        when(trip.getTripYear()).thenReturn(2026);
        when(trip.getStatus()).thenReturn(TripStatus.PLANNING);
        when(trip.getArchived()).thenReturn(false);
        when(trip.getPlannedRoundCount()).thenReturn(5);

        TripPlannedRound first = mock(TripPlannedRound.class);
        TripPlannedRound last = mock(TripPlannedRound.class);
        when(first.getRoundDate()).thenReturn(LocalDate.of(2026, 4, 20));
        when(last.getRoundDate()).thenReturn(LocalDate.of(2026, 4, 24));

        when(tripRepository.findByArchivedFalseOrArchivedIsNull()).thenReturn(List.of(trip));
        when(tripPlayerRepository.countByTrip(trip)).thenReturn(16L);
        when(roundRepository.countByTrip_Id(10L)).thenReturn(0L);
        when(tripPlannedRoundService.resolvePlannedRoundCount(5)).thenReturn(5);
        when(tripPlannedRoundService.findAllPlannedRounds(trip)).thenReturn(List.of(last, first));

        List<TripListResponse> responses = service.getTrips(false);

        assertEquals(1, responses.size());
        TripListResponse response = responses.get(0);
        assertEquals(10L, response.getTripId());
        assertEquals(16L, response.getPlayerCount());
        assertEquals(0L, response.getRoundCount());
        assertEquals(5, response.getPlannedRoundCount());
        assertEquals(LocalDate.of(2026, 4, 20), response.getStartDate());
        assertEquals(LocalDate.of(2026, 4, 24), response.getEndDate());
        assertTrue(response.getCanDelete());
        assertTrue(response.getCanArchive());
        assertFalse(response.getCanRestore());
    }

    @Test
    void getTrip_missingTripIsRejected() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> service.getTrip(99L));

        assertTrue(ex.getMessage().contains("Trip not found"));
    }

    @Test
    void getTrip_mapsReadinessAndNoCurrentRound() {
        Trip trip = mock(Trip.class);
        when(trip.getId()).thenReturn(10L);
        when(trip.getName()).thenReturn("Myrtle 2026");
        when(trip.getTripCode()).thenReturn("MYR26");
        when(trip.getTripYear()).thenReturn(2026);
        when(trip.getStatus()).thenReturn(TripStatus.PLANNING);
        when(trip.getPlannedRoundCount()).thenReturn(5);

        TripReadinessResponse readiness = new TripReadinessResponse();
        readiness.setUnresolvedGhinFixCount(2L);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlannedRoundService.resolvePlannedRoundCount(5)).thenReturn(5);
        when(tripPlayerRepository.findByTrip(trip)).thenReturn(List.of());
        when(tripReadinessService.getTripReadiness(trip)).thenReturn(readiness);

        TripDetailResponse response = service.getTrip(10L);

        assertEquals("Myrtle 2026", response.getTripName());
        assertEquals("MYR26", response.getTripCode());
        assertEquals(2L, response.getUnresolvedGhinFixCount());
        assertSame(readiness, response.getReadiness());
        assertNull(response.getCurrentRound());
        assertFalse(response.getHasFemalePlayers());
    }

    @Test
    void getTripPlayers_usesFrozenIndexAndParticipationSummary() {
        Trip trip = mock(Trip.class);
        Player player = mock(Player.class);
        TripPlayer tripPlayer = mock(TripPlayer.class);

        when(trip.getTripCode()).thenReturn("MYR26");
        when(trip.getHandicapMethod()).thenReturn(TripHandicapMethod.FROZEN_GHIN_INDEX);
        when(player.getId()).thenReturn(20L);
        when(player.getDisplayName()).thenReturn("Player One");
        when(player.getGhinNumber()).thenReturn("12345");
        when(player.isActive()).thenReturn(true);
        when(tripPlayer.getPlayer()).thenReturn(player);
        when(tripPlayer.getFrozenHandicapIndex()).thenReturn(new BigDecimal("8.4"));
        when(tripPlayer.getParticipationStatus()).thenReturn(ScorecardParticipationStatus.NO_SHOW);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip)).thenReturn(List.of(tripPlayer));
        when(tripParticipationService.countUnavailableRounds(10L, 20L)).thenReturn(2L);

        List<TripPlayerResponse> responses = service.getTripPlayers(10L);

        assertEquals(1, responses.size());
        TripPlayerResponse response = responses.get(0);
        assertEquals(20L, response.getPlayerId());
        assertEquals("Player One", response.getDisplayName());
        assertEquals("NO_SHOW", response.getParticipationStatus());
        assertEquals(2L, response.getUnavailableRoundCount());
        assertEquals(new BigDecimal("8.4"), response.getHandicapIndex());
        assertTrue(response.getUsableHandicapIndex());
    }
}
