package com.myrtletrip.handicap.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.model.TripHandicapMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripHandicapServiceTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private ScoreHistoryEntryRepository scoreHistoryEntryRepository;

    private TripHandicapService service;

    @BeforeEach
    void setUp() {
        TripHandicapCalculationService calculationService =
                new TripHandicapCalculationService(scoreHistoryEntryRepository);
        service = new TripHandicapService(
                new TripHandicapRoutingService(playerRepository, calculationService),
                calculationService
        );
    }

    @Test
    void calculateGhinTripIndex_shouldUseLowestDifferentialWhenFiveScoresAreAvailable() {
        Player player = player(101L, "GHIN");
        List<ScoreHistoryEntry> entries = List.of(
                score(1L, LocalDate.of(2026, 9, 30), "GHIN_FROZEN", "9.0", 1),
                score(2L, LocalDate.of(2026, 9, 29), "GHIN_FROZEN", "8.0", 2),
                score(3L, LocalDate.of(2026, 9, 28), "GHIN_FROZEN", "7.0", 3),
                score(4L, LocalDate.of(2026, 9, 27), "GHIN_FROZEN", "6.0", 4),
                score(5L, LocalDate.of(2026, 9, 26), "GHIN_FROZEN", "5.0", 5)
        );
        when(scoreHistoryEntryRepository.findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                eq(player), eq("T26"), eq(Set.of("GHIN_FROZEN", "TRIP_ROUND"))))
                .thenReturn(entries);

        BigDecimal result = service.calculateGhinTripIndex(player, "T26");

        assertEquals(new BigDecimal("5.0"), result);
    }

    @Test
    void calculateDbScoreHistoryTripIndex_shouldAverageLowestThreeOfNewestSix() {
        Player player = player(102L, "MYRTLE_BEACH");
        List<ScoreHistoryEntry> frozen = List.of(
                score(11L, LocalDate.of(2026, 9, 30), "DB_HISTORY_FROZEN", "12.0", null),
                score(12L, LocalDate.of(2026, 9, 29), "DB_HISTORY_FROZEN", "10.0", null),
                score(13L, LocalDate.of(2026, 9, 28), "DB_HISTORY_FROZEN", "8.0", null),
                score(14L, LocalDate.of(2026, 9, 27), "DB_HISTORY_FROZEN", "6.0", null),
                score(15L, LocalDate.of(2026, 9, 26), "DB_HISTORY_FROZEN", "4.0", null),
                score(16L, LocalDate.of(2026, 9, 25), "DB_HISTORY_FROZEN", "2.0", null)
        );
        when(scoreHistoryEntryRepository.findByPlayerAndSourceTypeIn(
                eq(player), eq(Set.of("DB_HISTORY_FROZEN"))))
                .thenReturn(frozen);
        when(scoreHistoryEntryRepository.findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                eq(player), eq("T26"), eq(Set.of("TRIP_ROUND"))))
                .thenReturn(List.of());

        BigDecimal result = service.calculateDbScoreHistoryTripIndex(player, "T26");

        assertEquals(new BigDecimal("4.0"), result);
    }

    @Test
    void calculateCombinedTripIndex_shouldIncludeGhinDbHistoryAndTripRoundSources() {
        Player player = player(103L, "GHIN");
        List<ScoreHistoryEntry> ghinAndTrip = List.of(
                score(21L, LocalDate.of(2026, 10, 1), "TRIP_ROUND", "7.0", null),
                score(22L, LocalDate.of(2026, 9, 20), "GHIN_FROZEN", "9.0", 1)
        );
        List<ScoreHistoryEntry> db = List.of(
                score(23L, LocalDate.of(2026, 9, 15), "DB_HISTORY_FROZEN", "4.0", null)
        );
        when(scoreHistoryEntryRepository.findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                eq(player), eq("T26"), eq(Set.of("GHIN_FROZEN", "TRIP_ROUND"))))
                .thenReturn(ghinAndTrip);
        when(scoreHistoryEntryRepository.findByPlayerAndSourceTypeIn(
                eq(player), eq(Set.of("DB_HISTORY_FROZEN"))))
                .thenReturn(db);

        BigDecimal result = service.calculateTripIndex(
                player, "T26", TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY);

        assertEquals(new BigDecimal("4.0"), result);
    }

    @Test
    void calculateTripIndexAsOf_shouldExcludeScoresOnOrAfterCalculationDate() {
        Player player = player(104L, "GHIN");
        LocalDate asOf = LocalDate.of(2026, 10, 3);
        List<ScoreHistoryEntry> entries = List.of(
                score(31L, LocalDate.of(2026, 10, 4), "TRIP_ROUND", "1.0", null),
                score(32L, LocalDate.of(2026, 10, 3), "TRIP_ROUND", "2.0", null),
                score(33L, LocalDate.of(2026, 10, 2), "TRIP_ROUND", "8.0", null),
                score(34L, LocalDate.of(2026, 10, 1), "GHIN_FROZEN", "6.0", 1),
                score(35L, LocalDate.of(2026, 9, 30), "GHIN_FROZEN", "4.0", 2)
        );
        when(scoreHistoryEntryRepository.findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                eq(player), eq("T26"), eq(Set.of("GHIN_FROZEN", "TRIP_ROUND"))))
                .thenReturn(entries);

        BigDecimal result = service.calculateTripIndexAsOf(
                player, "T26", asOf, TripHandicapMethod.GHIN_HISTORY);

        assertEquals(new BigDecimal("4.0"), result);
    }

    @Test
    void calculateTripIndexByPlayerId_shouldLoadPlayerAndRouteByHandicapMethod() {
        Player player = player(105L, "DB_SCORE_HISTORY");
        when(playerRepository.findById(105L)).thenReturn(Optional.of(player));
        ScoreHistoryEntry score1 = score(41L, LocalDate.of(2026, 9, 30), "DB_HISTORY_FROZEN", "5.0", null);
        ScoreHistoryEntry score2 = score(42L, LocalDate.of(2026, 9, 29), "DB_HISTORY_FROZEN", "7.0", null);
        ScoreHistoryEntry score3 = score(43L, LocalDate.of(2026, 9, 28), "DB_HISTORY_FROZEN", "9.0", null);
        when(scoreHistoryEntryRepository.findByPlayerAndSourceTypeIn(
                eq(player), eq(Set.of("DB_HISTORY_FROZEN"))))
                .thenReturn(List.of(score1, score2, score3));
        when(scoreHistoryEntryRepository.findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                eq(player), eq("T26"), eq(Set.of("TRIP_ROUND"))))
                .thenReturn(List.of());

        BigDecimal result = service.calculateTripIndex(105L, "T26");

        assertEquals(new BigDecimal("5.0"), result);
        verify(playerRepository).findById(105L);

        IllegalArgumentException missing = assertThrows(
                IllegalArgumentException.class,
                () -> service.calculateTripIndex(999L, "T26")
        );
        assertEquals("Player not found: 999", missing.getMessage());
    }

    private Player player(Long id, String handicapMethod) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName("Player " + id);
        player.setHandicapMethod(handicapMethod);
        return player;
    }

    private ScoreHistoryEntry score(Long id,
                                    LocalDate date,
                                    String sourceType,
                                    String differential,
                                    Integer postingOrder) {
        ScoreHistoryEntry entry = mock(ScoreHistoryEntry.class);
        lenient().when(entry.getId()).thenReturn(id);
        lenient().when(entry.getScoreDate()).thenReturn(date);
        lenient().when(entry.getSourceType()).thenReturn(sourceType);
        lenient().when(entry.getDifferential()).thenReturn(new BigDecimal(differential));
        lenient().when(entry.getPostingOrder()).thenReturn(postingOrder);
        return entry;
    }
}
