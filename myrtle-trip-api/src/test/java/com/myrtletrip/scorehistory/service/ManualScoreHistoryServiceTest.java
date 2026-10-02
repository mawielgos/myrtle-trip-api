package com.myrtletrip.scorehistory.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.scorehistory.dto.DbScoreHistoryImportCandidateResponse;
import com.myrtletrip.scorehistory.dto.ManualScoreHistoryEntryResponse;
import com.myrtletrip.scorehistory.dto.SaveManualScoreHistoryEntryRequest;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualScoreHistoryServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;
    @Mock private ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    @Mock private ScorecardRepository scorecardRepository;
    @Mock private HoleScoreRepository holeScoreRepository;
    @Mock private TeamHoleScoreRepository teamHoleScoreRepository;
    @Mock private RoundTeamRepository roundTeamRepository;

    private ManualScoreHistoryService service;

    @BeforeEach
    void setUp() {
        ManualScoreHistoryReadModelService readModelService = new ManualScoreHistoryReadModelService(
                tripRepository,
                tripPlayerRepository,
                scoreHistoryEntryRepository
        );

        ManualScoreHistoryCommandService commandService = new ManualScoreHistoryCommandService(
                tripRepository,
                playerRepository,
                tripPlayerRepository,
                scoreHistoryEntryRepository,
                scorecardRepository,
                holeScoreRepository,
                teamHoleScoreRepository,
                roundTeamRepository
        );

        service = new ManualScoreHistoryService(readModelService, commandService);
    }

    @Test
    void getManualEntries_shouldMapFrozenRowsForTrip() {
        Trip trip = mockTrip(10L, "MB2027");
        Player player = player(5L, "Mike Player");
        ScoreHistoryEntry entry = score(player, LocalDate.of(2026, 8, 1), "Pinehurst No. 2", "72.4", 135, 84, 83, "8.9", 2);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(scoreHistoryEntryRepository.findByHandicapGroupCodeAndSourceTypeOrderByPlayer_DisplayNameAscPostingOrderAscIdAsc(
                "MB2027", ManualScoreHistoryService.SOURCE_TYPE_GHIN_FROZEN))
                .thenReturn(List.of(entry));

        List<ManualScoreHistoryEntryResponse> result = service.getManualEntries(10L);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getPlayerId());
        assertEquals("Mike Player", result.get(0).getPlayerName());
        assertEquals("Pinehurst No. 2", result.get(0).getCourseName());
        assertEquals(new BigDecimal("8.9"), result.get(0).getDifferential());
        assertEquals(2, result.get(0).getPostingOrder());
    }

    @Test
    void getImportableDbScoreHistory_shouldUseTripPlayersAndMapPriorTripRoundRows() {
        Trip trip = mockTrip(10L, "MB2027");
        Player player = player(5L, "Mike Player");
        TripPlayer tripPlayer = new TripPlayer();
        tripPlayer.setTrip(trip);
        tripPlayer.setPlayer(player);
        tripPlayer.setDisplayOrder(1);

        Trip sourceTrip = mockTrip(8L, "MB2026");
        when(sourceTrip.getName()).thenReturn("Myrtle 2026");
        Round sourceRound = new Round();
        sourceRound.setTrip(sourceTrip);
        sourceRound.setRoundNumber(4);

        ScoreHistoryEntry entry = score(player, LocalDate.of(2026, 4, 20), "Caledonia", "71.8", 128, 82, 81, "8.1", 1);
        entry.setRound(sourceRound);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_Id(10L)).thenReturn(List.of(tripPlayer));
        when(scoreHistoryEntryRepository.findImportablePriorTripRoundEntries(
                10L, List.of(5L), RoundScoreHistorySyncService.SOURCE_TRIP_ROUND))
                .thenReturn(List.of(entry));

        List<DbScoreHistoryImportCandidateResponse> result = service.getImportableDbScoreHistory(10L);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getPlayerId());
        assertEquals(8L, result.get(0).getSourceTripId());
        assertEquals("Myrtle 2026", result.get(0).getSourceTripName());
        assertEquals("MB2026", result.get(0).getSourceTripCode());
        assertEquals(4, result.get(0).getSourceRoundNumber());
        assertEquals(new BigDecimal("8.1"), result.get(0).getDifferential());
    }

    @Test
    void createManualEntry_shouldCalculateDifferentialAndApplyDefaults() {
        Trip trip = mockTrip(10L, "MB2027");
        Player player = player(5L, "Mike Player");
        TripPlayer tripPlayer = new TripPlayer();
        tripPlayer.setTrip(trip);
        tripPlayer.setPlayer(player);
        tripPlayer.setDisplayOrder(1);

        SaveManualScoreHistoryEntryRequest request = request(5L, LocalDate.of(2026, 9, 1), "  Pinehurst  ", "72.5", 125, 90);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdAndPlayer_Id(10L, 5L)).thenReturn(Optional.of(tripPlayer));
        when(playerRepository.findById(5L)).thenReturn(Optional.of(player));
        when(scoreHistoryEntryRepository.countByPlayer_IdAndHandicapGroupCodeAndSourceType(
                5L, "MB2027", ManualScoreHistoryService.SOURCE_TYPE_GHIN_FROZEN)).thenReturn(0L);
        when(scoreHistoryEntryRepository.findByPlayer_IdAndHandicapGroupCodeAndSourceTypeOrderByPostingOrderAscIdAsc(
                5L, "MB2027", ManualScoreHistoryService.SOURCE_TYPE_GHIN_FROZEN)).thenReturn(List.of());
        when(scoreHistoryEntryRepository.save(any(ScoreHistoryEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ManualScoreHistoryEntryResponse result = service.createManualEntry(10L, request);

        assertEquals(5L, result.getPlayerId());
        assertEquals("Pinehurst", result.getCourseName());
        assertEquals(new BigDecimal("72.5"), result.getCourseRating());
        assertEquals(90, result.getAdjustedGrossScore());
        assertEquals(new BigDecimal("15.8"), result.getDifferential());
        assertEquals(Boolean.TRUE, result.getIncludedInMyrtleCalc());
        assertEquals(18, result.getHolesPlayed());
        assertEquals(1, result.getPostingOrder());
    }

    @Test
    void createManualEntry_shouldRejectPlayerWhoIsNotOnTrip() {
        Trip trip = mockTrip(10L, "MB2027");
        SaveManualScoreHistoryEntryRequest request = request(99L, LocalDate.of(2026, 9, 1), "Pinehurst", "72.5", 125, 90);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdAndPlayer_Id(10L, 99L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.createManualEntry(10L, request)
        );

        assertTrue(error.getMessage().contains("Player is not on this trip: 99"));
        verify(playerRepository, never()).findById(any());
        verify(scoreHistoryEntryRepository, never()).save(any());
    }

    @Test
    void deleteManualEntry_shouldDeleteAndRenumberRemainingRows() {
        Trip trip = mockTrip(10L, "MB2027");
        Player player = player(5L, "Mike Player");
        ScoreHistoryEntry deleted = score(player, LocalDate.of(2026, 9, 1), "Course A", "72.0", 120, 82, 82, "9.4", 1);
        ScoreHistoryEntry remainingA = score(player, LocalDate.of(2026, 8, 1), "Course B", "72.0", 120, 83, 83, "10.4", 2);
        ScoreHistoryEntry remainingB = score(player, LocalDate.of(2026, 7, 1), "Course C", "72.0", 120, 84, 84, "11.3", 4);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(scoreHistoryEntryRepository.findByIdAndHandicapGroupCodeAndSourceType(
                77L, "MB2027", ManualScoreHistoryService.SOURCE_TYPE_GHIN_FROZEN))
                .thenReturn(Optional.of(deleted));
        when(scoreHistoryEntryRepository.findByPlayer_IdAndHandicapGroupCodeAndSourceTypeOrderByPostingOrderAscIdAsc(
                5L, "MB2027", ManualScoreHistoryService.SOURCE_TYPE_GHIN_FROZEN))
                .thenReturn(List.of(remainingA, remainingB));

        service.deleteManualEntry(10L, 77L);

        verify(scoreHistoryEntryRepository).delete(deleted);
        verify(scoreHistoryEntryRepository).flush();
        assertEquals(1, remainingA.getPostingOrder());
        assertEquals(2, remainingB.getPostingOrder());
        verify(scoreHistoryEntryRepository).save(remainingA);
        verify(scoreHistoryEntryRepository).save(remainingB);
    }

    private Trip mockTrip(Long id, String tripCode) {
        Trip trip = org.mockito.Mockito.mock(Trip.class);
        org.mockito.Mockito.lenient().when(trip.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(trip.getTripCode()).thenReturn(tripCode);
        return trip;
    }

    private Player player(Long id, String displayName) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName(displayName);
        return player;
    }

    private ScoreHistoryEntry score(Player player,
                                    LocalDate date,
                                    String courseName,
                                    String rating,
                                    int slope,
                                    int gross,
                                    int adjusted,
                                    String differential,
                                    int postingOrder) {
        ScoreHistoryEntry entry = new ScoreHistoryEntry();
        entry.setPlayer(player);
        entry.setScoreDate(date);
        entry.setCourseName(courseName);
        entry.setCourseRating(new BigDecimal(rating));
        entry.setSlope(slope);
        entry.setGrossScore(gross);
        entry.setAdjustedGrossScore(adjusted);
        entry.setDifferential(new BigDecimal(differential));
        entry.setIncludedInMyrtleCalc(Boolean.TRUE);
        entry.setHolesPlayed(18);
        entry.setPostingOrder(postingOrder);
        entry.setSourceType(ManualScoreHistoryService.SOURCE_TYPE_GHIN_FROZEN);
        entry.setManualDifferentialRequired(Boolean.FALSE);
        return entry;
    }

    private SaveManualScoreHistoryEntryRequest request(Long playerId,
                                                       LocalDate date,
                                                       String courseName,
                                                       String rating,
                                                       int slope,
                                                       int gross) {
        SaveManualScoreHistoryEntryRequest request = new SaveManualScoreHistoryEntryRequest();
        request.setPlayerId(playerId);
        request.setScoreDate(date);
        request.setCourseName(courseName);
        request.setCourseRating(new BigDecimal(rating));
        request.setSlope(slope);
        request.setGrossScore(gross);
        return request;
    }
}
