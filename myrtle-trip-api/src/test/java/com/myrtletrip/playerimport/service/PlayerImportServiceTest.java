package com.myrtletrip.playerimport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.playerimport.dto.PlayerImportRow;
import com.myrtletrip.playerimport.model.ImportMatchStatus;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class PlayerImportServiceTest {

    @Mock private PlayerRepository playerRepository;
    @Mock private TripRepository tripRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;

    private PlayerImportService service;

    @BeforeEach
    void setUp() {
        PlayerImportPreviewService previewService = new PlayerImportPreviewService(
                playerRepository, tripRepository, tripPlayerRepository);
        PlayerImportCommitService commitService = new PlayerImportCommitService(
                playerRepository, tripRepository, tripPlayerRepository);
        service = new PlayerImportService(previewService, commitService);
    }

    @Test
    void previewCsv_shouldMatchByGhinAndNormalizeImportedValues() throws Exception {
        Trip trip = trip(false);
        Player matched = player(7L, "Jane", "Golfer", "Jane Golfer");

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(playerRepository.findByGhinNumberIgnoreCase("1234567")).thenReturn(Optional.of(matched));
        when(tripPlayerRepository.existsByTrip_IdAndPlayer_Id(10L, 7L)).thenReturn(false);

        MockMultipartFile file = csv(
                "First Name,Last Name,Email,GHIN Number,Gender,Handicap Index\n"
                        + " Jane , Golfer ,Jane Golfer <MAILTO:jane@example.com>;ignored,1234567,Female,8.24\n");

        List<PlayerImportRow> rows = service.previewCsv(10L, file);

        assertEquals(1, rows.size());
        PlayerImportRow row = rows.get(0);
        assertEquals(1, row.getRowNumber());
        assertEquals("Jane", row.getFirstName());
        assertEquals("Golfer", row.getLastName());
        assertEquals("MAILTO:jane@example.com", row.getEmail());
        assertEquals("1234567", row.getGhinNumber());
        assertEquals("Female", row.getGender());
        assertEquals(new BigDecimal("8.2"), row.getHandicapIndex());
        assertEquals(ImportMatchStatus.MATCHED_BY_GHIN, row.getMatchStatus());
        assertEquals(7L, row.getMatchedPlayerId());
        assertEquals("Jane Golfer", row.getMatchedPlayerName());
        assertFalse(row.getAlreadyInTrip());
    }

    @Test
    void previewCsv_shouldMarkMissingRequiredNameInvalid() throws Exception {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(false)));

        MockMultipartFile file = csv(
                "First Name,Last Name,Email,GHIN Number,Gender,Handicap Index\n"
                        + ",Golfer,jane@example.com,,F,12.0\n");

        PlayerImportRow row = service.previewCsv(10L, file).get(0);

        assertEquals(ImportMatchStatus.INVALID, row.getMatchStatus());
        assertEquals("Missing required first name or last name.", row.getValidationMessage());
        verify(playerRepository, never()).findByGhinNumberIgnoreCase(any());
        verify(playerRepository, never()).findByNormalizedEmail(any());
    }

    @Test
    void previewCsv_shouldRejectInitializedTripBeforeReadingFile() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(true)));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.previewCsv(10L, csv("First Name,Last Name\nJane,Golfer\n")));

        assertEquals("Players cannot be imported after the event has started.", error.getMessage());
    }

    @Test
    void commitImport_shouldSynchronizeRosterOrderIndexesAndRemoveOmittedPlayers() {
        Trip trip = trip(false);
        Player player1 = player(1L, "Ann", "One", null);
        Player player2 = player(2L, "Bob", "Two", "Bob Two");
        Player stalePlayer = player(3L, "Old", "Player", "Old Player");

        TripPlayer existing1 = tripPlayer(trip, player1, 9, new BigDecimal("20.0"));
        TripPlayer stale = tripPlayer(trip, stalePlayer, 3, new BigDecimal("15.0"));

        PlayerImportRow row1 = matchedRow(1, 1L, ImportMatchStatus.ALREADY_IN_TRIP, new BigDecimal("7.4"));
        row1.setEmail("ann.new@example.com");
        row1.setGhinNumber("1111111");
        row1.setGender("Female");

        PlayerImportRow row2 = matchedRow(2, 2L, ImportMatchStatus.MATCHED_BY_EMAIL, new BigDecimal("10.1"));

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(player2));
        when(tripPlayerRepository.findByTrip_IdAndPlayer_Id(10L, 1L)).thenReturn(Optional.of(existing1));
        when(tripPlayerRepository.findByTrip_IdAndPlayer_Id(10L, 2L)).thenReturn(Optional.empty());
        when(tripPlayerRepository.findByTrip_Id(10L)).thenReturn(List.of(existing1, stale));

        service.commitImport(10L, List.of(row1, row2));

        assertEquals("ann.new@example.com", player1.getEmail());
        assertEquals("1111111", player1.getGhinNumber());
        assertEquals("GHIN", player1.getHandicapMethod());
        assertEquals("F", player1.getGender());
        assertEquals("Ann One", player1.getDisplayName());

        ArgumentCaptor<TripPlayer> tripPlayerCaptor = ArgumentCaptor.forClass(TripPlayer.class);
        verify(tripPlayerRepository, times(2)).save(tripPlayerCaptor.capture());
        List<TripPlayer> saved = tripPlayerCaptor.getAllValues();
        assertEquals(1, saved.get(0).getDisplayOrder());
        assertEquals(new BigDecimal("7.4"), saved.get(0).getFrozenHandicapIndex());
        assertEquals(2, saved.get(1).getDisplayOrder());
        assertEquals(new BigDecimal("10.1"), saved.get(1).getFrozenHandicapIndex());
        assertEquals(player2, saved.get(1).getPlayer());
        assertEquals(trip, saved.get(1).getTrip());
        verify(tripPlayerRepository).delete(stale);
    }

    @Test
    void commitImport_shouldRejectDuplicateResolvedPlayer() {
        Trip trip = trip(false);
        Player player = player(1L, "Ann", "One", "Ann One");
        TripPlayer existing = tripPlayer(trip, player, 1, new BigDecimal("8.0"));
        PlayerImportRow row1 = matchedRow(1, 1L, ImportMatchStatus.MATCHED_BY_GHIN, new BigDecimal("7.5"));
        PlayerImportRow row2 = matchedRow(2, 1L, ImportMatchStatus.MATCHED_BY_EMAIL, new BigDecimal("7.5"));

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(tripPlayerRepository.findByTrip_IdAndPlayer_Id(10L, 1L)).thenReturn(Optional.of(existing));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.commitImport(10L, List.of(row1, row2)));

        assertEquals("The CSV contains the same player more than once: Ann One", error.getMessage());
        verify(tripPlayerRepository).save(existing);
        verify(tripPlayerRepository, never()).findByTrip_Id(10L);
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile(
                "file",
                "players.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private Trip trip(boolean initialized) {
        Trip trip = new Trip();
        trip.setInitialized(initialized);
        return trip;
    }

    private Player player(Long id, String firstName, String lastName, String displayName) {
        Player player = new Player();
        player.setId(id);
        player.setFirstName(firstName);
        player.setLastName(lastName);
        player.setDisplayName(displayName);
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

    private PlayerImportRow matchedRow(
            int rowNumber,
            Long playerId,
            ImportMatchStatus status,
            BigDecimal index) {
        PlayerImportRow row = new PlayerImportRow();
        row.setRowNumber(rowNumber);
        row.setMatchedPlayerId(playerId);
        row.setMatchStatus(status);
        row.setHandicapIndex(index);
        return row;
    }
}
