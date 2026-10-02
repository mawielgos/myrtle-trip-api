package com.myrtletrip.handicap.card.service;

import com.myrtletrip.handicap.card.dto.HandicapCardListResponse;
import com.myrtletrip.handicap.card.dto.HandicapCardPlayerResponse;
import com.myrtletrip.handicap.card.dto.HandicapCardScoreResponse;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.model.TripHandicapMethod;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandicapCardServiceTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripPlayerRepository tripPlayerRepository;
    @Mock
    private ScoreHistoryEntryRepository scoreHistoryEntryRepository;

    private HandicapCardService service;

    @BeforeEach
    void setUp() {
        HandicapCardCalculationService calculationService = new HandicapCardCalculationService(scoreHistoryEntryRepository);
        HandicapCardReadModelService readModelService = new HandicapCardReadModelService(
                tripRepository, tripPlayerRepository, calculationService);
        service = new HandicapCardService(readModelService);
    }

    @Test
    void getTripHandicapCards_shouldUseFrozenTripIndexWithoutScoreHistoryLookup() {
        Trip trip = mockTrip(10L, TripHandicapMethod.FROZEN_GHIN_INDEX);
        Player player = player(101L, "Frozen Player", "GHIN");
        TripPlayer tripPlayer = tripPlayer(trip, player, 1, new BigDecimal("8.4"));
        LocalDate asOf = LocalDate.of(2026, 10, 2);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(tripPlayer));

        HandicapCardListResponse response = service.getTripHandicapCards(10L, asOf);

        assertEquals(10L, response.getTripId());
        assertEquals("Test Trip", response.getTripName());
        assertEquals(asOf, response.getAsOfDate());
        assertEquals(1, response.getPlayers().size());
        assertEquals(new BigDecimal("8.4"), response.getPlayers().get(0).getTripIndex());
        assertEquals("FROZEN_GHIN_INDEX", response.getPlayers().get(0).getHandicapMethod());
        assertEquals("READY", response.getPlayers().get(0).getStatusCode());
        verifyNoInteractions(scoreHistoryEntryRepository);
    }

    @Test
    void getPlayerHandicapCard_shouldRequirePlayerId() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.getPlayerHandicapCard(10L, null, LocalDate.of(2026, 10, 2))
        );

        assertEquals("playerId is required", error.getMessage());
        verifyNoInteractions(tripRepository, tripPlayerRepository, scoreHistoryEntryRepository);
    }

    @Test
    void getPlayerHandicapCard_shouldRejectPlayerNotOnTrip() {
        Trip trip = mockTrip(10L, TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY);
        Player other = player(202L, "Other Player", "GHIN");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(10L))
                .thenReturn(List.of(tripPlayer(trip, other, 1, null)));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.getPlayerHandicapCard(10L, 101L, LocalDate.of(2026, 10, 2))
        );

        assertEquals("Player 101 is not on trip 10", error.getMessage());
        verifyNoInteractions(scoreHistoryEntryRepository);
    }

    @Test
    void getPlayerHandicapCard_shouldCalculateGhinCurrentAndPendingIndexes() {
        Trip trip = mockTrip(10L, TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY);
        Player player = player(101L, "GHIN Player", "GHIN");
        TripPlayer tripPlayer = tripPlayer(trip, player, 1, null);
        LocalDate asOf = LocalDate.of(2026, 10, 2);

        ScoreHistoryEntry current1 = score(1L, LocalDate.of(2026, 9, 29), "GHIN_FROZEN", new BigDecimal("12.0"), 1, false);
        ScoreHistoryEntry current2 = score(2L, LocalDate.of(2026, 9, 20), "GHIN_FROZEN", new BigDecimal("10.0"), 2, false);
        ScoreHistoryEntry current3 = score(3L, LocalDate.of(2026, 9, 10), "GHIN_FROZEN", new BigDecimal("8.0"), 3, false);
        ScoreHistoryEntry pending = score(4L, asOf, "TRIP_ROUND", new BigDecimal("6.0"), null, false);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(tripPlayer));
        when(scoreHistoryEntryRepository.findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                eq(player), eq("T26"), eq(Set.of("GHIN_FROZEN", "TRIP_ROUND"))))
                .thenReturn(new ArrayList<>(List.of(current1, current2, current3, pending)));

        HandicapCardPlayerResponse response = service.getPlayerHandicapCard(10L, 101L, asOf);

        assertEquals(new BigDecimal("8.0"), response.getTripIndex());
        assertEquals(new BigDecimal("6.0"), response.getPendingTripIndex());
        assertEquals(1, response.getPendingScoreCount());
        assertEquals(3, response.getEligibleScoreCount());
        assertEquals(3, response.getWindowScoreCount());
        assertEquals(1, response.getUsedScoreCount());
        assertEquals(4, response.getScores().size());

        HandicapCardScoreResponse pendingResponse = response.getScores().get(0);
        assertEquals(4L, pendingResponse.getScoreHistoryEntryId());
        assertEquals("PENDING", pendingResponse.getScoreSection());
        assertTrue(pendingResponse.getPendingForCalculationDate());
        assertTrue(pendingResponse.getUsedInPendingIndex());
        assertFalse(pendingResponse.getEligibleForWindow());
        assertEquals("Posted on calculation date; pending next index", pendingResponse.getExclusionReason());
    }

    @Test
    void getTripHandicapCards_shouldCalculateDbHistoryAndFlagManualReview() {
        Trip trip = mockTrip(10L, TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY);
        Player player = player(101L, "History Player", "MYRTLE_BEACH");
        TripPlayer tripPlayer = tripPlayer(trip, player, 1, null);
        LocalDate asOf = LocalDate.of(2026, 10, 2);

        ScoreHistoryEntry s1 = score(11L, LocalDate.of(2026, 9, 30), "DB_HISTORY_FROZEN", new BigDecimal("11.0"), null, false);
        ScoreHistoryEntry s2 = score(12L, LocalDate.of(2026, 9, 29), "DB_HISTORY_FROZEN", new BigDecimal("7.0"), null, false);
        ScoreHistoryEntry s3 = score(13L, LocalDate.of(2026, 9, 28), "DB_HISTORY_FROZEN", new BigDecimal("9.0"), null, false);
        ScoreHistoryEntry review = score(14L, LocalDate.of(2026, 9, 27), "DB_HISTORY_FROZEN", null, null, true);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(tripPlayer));
        when(scoreHistoryEntryRepository.findByPlayerAndSourceTypeIn(eq(player), eq(Set.of("DB_HISTORY_FROZEN"))))
                .thenReturn(List.of(s1, s2, s3, review));
        when(scoreHistoryEntryRepository.findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                eq(player), eq("T26"), eq(Set.of("TRIP_ROUND"))))
                .thenReturn(List.of());

        HandicapCardListResponse response = service.getTripHandicapCards(10L, asOf);

        assertEquals(1, response.getPlayers().size());
        assertEquals("DB_SCORE_HISTORY", response.getPlayers().get(0).getHandicapMethod());
        assertEquals(new BigDecimal("7.0"), response.getPlayers().get(0).getTripIndex());
        assertEquals(3, response.getPlayers().get(0).getEligibleScoreCount());
        assertEquals(3, response.getPlayers().get(0).getWindowScoreCount());
        assertEquals(1, response.getPlayers().get(0).getUsedScoreCount());
        assertEquals("REVIEW", response.getPlayers().get(0).getStatusCode());
        assertEquals("1 score(s) need review", response.getPlayers().get(0).getStatusLabel());
    }

    private Trip mockTrip(Long id, TripHandicapMethod method) {
        Trip trip = mock(Trip.class);
        lenient().when(trip.getId()).thenReturn(id);
        lenient().when(trip.getName()).thenReturn("Test Trip");
        lenient().when(trip.getTripCode()).thenReturn("T26");
        lenient().when(trip.getTripYear()).thenReturn(2026);
        lenient().when(trip.getHandicapMethod()).thenReturn(method);
        return trip;
    }

    private Player player(Long id, String displayName, String handicapMethod) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName(displayName);
        player.setHandicapMethod(handicapMethod);
        return player;
    }

    private TripPlayer tripPlayer(Trip trip, Player player, int displayOrder, BigDecimal frozenIndex) {
        TripPlayer tripPlayer = new TripPlayer();
        tripPlayer.setTrip(trip);
        tripPlayer.setPlayer(player);
        tripPlayer.setDisplayOrder(displayOrder);
        tripPlayer.setFrozenHandicapIndex(frozenIndex);
        return tripPlayer;
    }

    private ScoreHistoryEntry score(Long id,
                                    LocalDate date,
                                    String sourceType,
                                    BigDecimal differential,
                                    Integer postingOrder,
                                    boolean manualReview) {
        ScoreHistoryEntry entry = mock(ScoreHistoryEntry.class);
        lenient().when(entry.getId()).thenReturn(id);
        lenient().when(entry.getScoreDate()).thenReturn(date);
        lenient().when(entry.getSourceType()).thenReturn(sourceType);
        lenient().when(entry.getDifferential()).thenReturn(differential);
        lenient().when(entry.getPostingOrder()).thenReturn(postingOrder);
        lenient().when(entry.getManualDifferentialRequired()).thenReturn(manualReview);
        lenient().when(entry.getCourseName()).thenReturn("Test Course");
        lenient().when(entry.getCourseRating()).thenReturn(new BigDecimal("72.0"));
        lenient().when(entry.getSlope()).thenReturn(113);
        lenient().when(entry.getGrossScore()).thenReturn(80);
        lenient().when(entry.getAdjustedGrossScore()).thenReturn(80);
        lenient().when(entry.getScoreType()).thenReturn("18H");
        lenient().when(entry.getHolesPlayed()).thenReturn(18);
        return entry;
    }
}
