package com.myrtletrip.tournament.service;

import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.tournament.dto.SaveTripTournamentSetupRequest;
import com.myrtletrip.tournament.dto.TripTournamentSetupResponse;
import com.myrtletrip.tournament.entity.TripTournament;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.tournament.repository.TripTournamentRoundRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripTournamentServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlannedRoundRepository tripPlannedRoundRepository;
    @Mock private TripTournamentRepository tripTournamentRepository;
    @Mock private TripTournamentRoundRepository tripTournamentRoundRepository;
    @Mock private CourseRepository courseRepository;

    private TripTournamentService service;

    @BeforeEach
    void setUp() {
        TripTournamentReadModelService readModelService = new TripTournamentReadModelService(
                tripRepository,
                tripPlannedRoundRepository,
                tripTournamentRepository,
                tripTournamentRoundRepository,
                courseRepository
        );
        TripTournamentCommandService commandService = new TripTournamentCommandService(
                readModelService,
                tripRepository,
                tripPlannedRoundRepository,
                tripTournamentRepository,
                tripTournamentRoundRepository
        );
        service = new TripTournamentService(readModelService, commandService);
    }

    @Test
    void namingMethods_shouldReturnDefaultsWhenTournamentDoesNotExist() {
        when(tripTournamentRepository.findByTrip_Id(10L)).thenReturn(Optional.empty());

        assertEquals(TripTournamentService.DEFAULT_TOURNAMENT_NAME, service.getTournamentName(10L));
        assertEquals(TripTournamentService.DEFAULT_STANDINGS_LABEL, service.getStandingsLabel(10L));
        assertEquals(TripTournamentService.DEFAULT_LOW_NET_NAME,
                service.getCompetitionName(10L, com.myrtletrip.tournament.model.TournamentCompetitionType.LOW_NET));
        assertEquals(TripTournamentService.DEFAULT_LOW_GROSS_NAME,
                service.getCompetitionName(10L, com.myrtletrip.tournament.model.TournamentCompetitionType.LOW_GROSS));
    }

    @Test
    void getIncludedTournamentPlannedRounds_shouldUseLegacyFlagsWhenNoTournamentExists() {
        Trip trip = trip(10L, TripStatus.PLANNING, false, 3);
        TripPlannedRound included = plannedRound(101L, trip, 1, true, RoundFormat.STROKE_PLAY);
        TripPlannedRound excluded = plannedRound(102L, trip, 2, false, RoundFormat.STROKE_PLAY);
        TripPlannedRound scramble = plannedRound(103L, trip, 3, true, RoundFormat.TEAM_SCRAMBLE);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripTournamentRepository.findByTrip_Id(10L)).thenReturn(Optional.empty());
        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip))
                .thenReturn(List.of(included, excluded, scramble));

        List<TripPlannedRound> result = service.getIncludedTournamentPlannedRounds(10L);

        assertEquals(List.of(included), result);
    }

    @Test
    void getIncludedTournamentPlannedRounds_shouldReturnEmptyWhenTournamentIsDisabled() {
        Trip trip = trip(10L, TripStatus.PLANNING, false, 5);
        TripTournament tournament = new TripTournament();
        tournament.setTrip(trip);
        tournament.setEnabled(false);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripTournamentRepository.findByTrip_Id(10L)).thenReturn(Optional.of(tournament));

        List<TripPlannedRound> result = service.getIncludedTournamentPlannedRounds(10L);

        assertTrue(result.isEmpty());
        verify(tripTournamentRoundRepository, never()).findByTournament_IdOrderBySortOrderAsc(any());
    }

    @Test
    void saveTournamentSetup_shouldRejectCompletedTripOutsideCorrectionMode() {
        Trip trip = trip(10L, TripStatus.COMPLETE, false, 5);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.saveTournamentSetup(10L, new SaveTripTournamentSetupRequest())
        );

        assertEquals(
                "Tournament setup cannot be changed after the trip is complete unless Correction Mode is enabled.",
                error.getMessage()
        );
        verify(tripTournamentRepository, never()).save(any());
    }

    @Test
    void saveTournamentSetup_shouldPersistIncludedRoundOrderAndSyncLegacyFlags() {
        Trip trip = trip(10L, TripStatus.PLANNING, false, 3);
        TripPlannedRound round1 = configuredRound(101L, trip, 1);
        TripPlannedRound round2 = configuredRound(102L, trip, 2);
        TripPlannedRound round3 = configuredRound(103L, trip, 3);
        round3.setIncludeInFourDayStandings(true);

        SaveTripTournamentSetupRequest request = new SaveTripTournamentSetupRequest();
        request.setEnabled(true);
        request.setName("  Myrtle Cup  ");
        request.setStandingsLabel("  Cup Standings  ");
        request.setLowNetEnabled(true);
        request.setLowGrossEnabled(true);
        request.setLowNetName(" Net Cup ");
        request.setLowGrossName(" Gross Cup ");
        request.setIncludedPlannedRoundIds(List.of(102L, 101L));

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip))
                .thenReturn(List.of(round1, round2, round3));
        when(tripTournamentRepository.findByTrip_Id(10L)).thenReturn(Optional.empty());
        when(tripTournamentRepository.save(any(TripTournament.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TripTournamentSetupResponse response = service.saveTournamentSetup(10L, request);

        ArgumentCaptor<TripTournament> tournamentCaptor = ArgumentCaptor.forClass(TripTournament.class);
        verify(tripTournamentRepository, org.mockito.Mockito.atLeastOnce()).save(tournamentCaptor.capture());
        TripTournament saved = tournamentCaptor.getValue();

        assertTrue(saved.getEnabled());
        assertEquals("Myrtle Cup", saved.getName());
        assertEquals("Cup Standings", saved.getStandingsLabel());
        assertTrue(saved.getLowNetEnabled());
        assertTrue(saved.getLowGrossEnabled());
        assertEquals("Net Cup", saved.getLowNetName());
        assertEquals("Gross Cup", saved.getLowGrossName());
        assertEquals(2, saved.getRounds().size());
        assertEquals(round2, saved.getRounds().get(0).getPlannedRound());
        assertEquals(1, saved.getRounds().get(0).getSortOrder());
        assertEquals(round1, saved.getRounds().get(1).getPlannedRound());
        assertEquals(2, saved.getRounds().get(1).getSortOrder());

        assertTrue(round1.getIncludeInFourDayStandings());
        assertTrue(round2.getIncludeInFourDayStandings());
        assertFalse(round3.getIncludeInFourDayStandings());
        verify(tripPlannedRoundRepository).saveAll(List.of(round1, round2, round3));

        assertTrue(response.getEnabled());
        assertEquals("Myrtle Cup", response.getName());
        assertEquals(2, response.getRounds().stream().filter(r -> Boolean.TRUE.equals(r.getIncluded())).count());
    }

    private Trip trip(Long id, TripStatus status, boolean correctionMode, int plannedRoundCount) {
        Trip trip = new Trip();
        setId(trip, id);
        trip.setStatus(status);
        trip.setCorrectionMode(correctionMode);
        trip.setPlannedRoundCount(plannedRoundCount);
        return trip;
    }

    private TripPlannedRound configuredRound(Long id, Trip trip, int roundNumber) {
        TripPlannedRound round = plannedRound(id, trip, roundNumber, false, RoundFormat.STROKE_PLAY);
        round.setRoundDate(LocalDate.of(2027, 3, 10).plusDays(roundNumber - 1L));
        round.setCourseId(1000L + roundNumber);
        round.setStandardTeeId(2000L + roundNumber);
        return round;
    }

    private TripPlannedRound plannedRound(
            Long id,
            Trip trip,
            int roundNumber,
            boolean included,
            RoundFormat format) {
        TripPlannedRound round = new TripPlannedRound();
        setId(round, id);
        round.setTrip(trip);
        round.setRoundNumber(roundNumber);
        round.setIncludeInFourDayStandings(included);
        round.setFormat(format);
        return round;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
