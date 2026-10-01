package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripParticipationServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private ScorecardRepository scorecardRepository;

    @InjectMocks
    private TripParticipationService service;

    @Test
    void updateParticipation_noShowClearsTeamAndWithdrawalAcrossEditableRounds() {
        Trip trip = trip();
        TripPlayer tripPlayer = tripPlayer(trip, 20L);
        Round round = round(101L, false);
        Scorecard scorecard = new Scorecard();
        scorecard.setParticipationStatus(ScorecardParticipationStatus.ACTIVE);
        scorecard.setWithdrawalHoleNumber(9);
        scorecard.setTeam(new RoundTeam());

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdAndPlayer_Id(10L, 20L)).thenReturn(Optional.of(tripPlayer));
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(10L)).thenReturn(List.of(round));
        when(scorecardRepository.findByRound_IdAndPlayer_Id(101L, 20L)).thenReturn(Optional.of(scorecard));

        service.updateParticipation(10L, 20L, "NO_SHOW");

        assertEquals(ScorecardParticipationStatus.NO_SHOW, tripPlayer.getParticipationStatus());
        assertEquals(ScorecardParticipationStatus.NO_SHOW, scorecard.getParticipationStatus());
        assertNull(scorecard.getWithdrawalHoleNumber());
        assertNull(scorecard.getTeam());
        verify(scorecardRepository).save(scorecard);
    }

    @Test
    void updateParticipation_skipsFinalizedRoundOutsideCorrectionMode() {
        Trip trip = trip();
        trip.setCorrectionMode(Boolean.FALSE);
        TripPlayer tripPlayer = tripPlayer(trip, 20L);
        Round round = round(101L, true);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdAndPlayer_Id(10L, 20L)).thenReturn(Optional.of(tripPlayer));
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(10L)).thenReturn(List.of(round));

        service.updateParticipation(10L, 20L, "WITHDRAWN");

        verify(scorecardRepository, never()).findByRound_IdAndPlayer_Id(101L, 20L);
    }

    @Test
    void updateParticipation_updatesFinalizedRoundInCorrectionMode() {
        Trip trip = trip();
        trip.setCorrectionMode(Boolean.TRUE);
        TripPlayer tripPlayer = tripPlayer(trip, 20L);
        Round round = round(101L, true);
        Scorecard scorecard = new Scorecard();
        scorecard.setTeam(new RoundTeam());

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdAndPlayer_Id(10L, 20L)).thenReturn(Optional.of(tripPlayer));
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(10L)).thenReturn(List.of(round));
        when(scorecardRepository.findByRound_IdAndPlayer_Id(101L, 20L)).thenReturn(Optional.of(scorecard));

        service.updateParticipation(10L, 20L, "WITHDRAWN");

        assertEquals(ScorecardParticipationStatus.WITHDRAWN, scorecard.getParticipationStatus());
        assertNull(scorecard.getTeam());
        verify(scorecardRepository).save(scorecard);
    }

    @Test
    void updateParticipation_rejectsUnsupportedStatusBeforeChangingRoster() {
        Trip trip = trip();
        TripPlayer tripPlayer = tripPlayer(trip, 20L);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdAndPlayer_Id(10L, 20L)).thenReturn(Optional.of(tripPlayer));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateParticipation(10L, 20L, "MAYBE"));

        assertTrue(error.getMessage().contains("Unsupported participation status"));
        verify(tripPlayerRepository, never()).save(tripPlayer);
    }

    @Test
    void countUnavailableRounds_countsOnlyNonActiveExistingScorecards() {
        Round round1 = roundIdOnly(101L);
        Round round2 = roundIdOnly(102L);
        Round round3 = roundIdOnly(103L);
        Scorecard active = new Scorecard();
        active.setParticipationStatus(ScorecardParticipationStatus.ACTIVE);
        Scorecard noShow = new Scorecard();
        noShow.setParticipationStatus(ScorecardParticipationStatus.NO_SHOW);

        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(10L)).thenReturn(List.of(round1, round2, round3));
        when(scorecardRepository.findByRound_IdAndPlayer_Id(101L, 20L)).thenReturn(Optional.of(active));
        when(scorecardRepository.findByRound_IdAndPlayer_Id(102L, 20L)).thenReturn(Optional.of(noShow));
        when(scorecardRepository.findByRound_IdAndPlayer_Id(103L, 20L)).thenReturn(Optional.empty());

        long count = service.countUnavailableRounds(10L, 20L);

        assertEquals(1L, count);
    }

    private Trip trip() {
        Trip trip = new Trip();
        trip.setCorrectionMode(Boolean.FALSE);
        return trip;
    }

    private TripPlayer tripPlayer(Trip trip, Long playerId) {
        Player player = new Player();
        player.setId(playerId);
        player.setDisplayName("Player " + playerId);
        player.setActive(true);
        TripPlayer tripPlayer = new TripPlayer();
        tripPlayer.setTrip(trip);
        tripPlayer.setPlayer(player);
        tripPlayer.setDisplayOrder(1);
        return tripPlayer;
    }

    private Round round(Long roundId, boolean finalized) {
        Round round = mock(Round.class);
        when(round.getId()).thenReturn(roundId);
        when(round.getFinalized()).thenReturn(finalized);
        return round;
    }

    private Round roundIdOnly(Long roundId) {
        Round round = mock(Round.class);
        when(round.getId()).thenReturn(roundId);
        return round;
    }
}
