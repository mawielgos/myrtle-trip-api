package com.myrtletrip.trip.service;

import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.event.repository.RoundEventRepository;
import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.prize.repository.PrizeWinningRepository;
import com.myrtletrip.round.repository.RoundCorrectionLogRepository;
import com.myrtletrip.round.repository.RoundGroupPlayerRepository;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripInitializationServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;
    @Mock private TripPlannedRoundRepository tripPlannedRoundRepository;
    @Mock private TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private RoundEventRepository roundEventRepository;
    @Mock private RoundGroupRepository roundGroupRepository;
    @Mock private RoundGroupPlayerRepository roundGroupPlayerRepository;
    @Mock private RoundTeamRepository roundTeamRepository;
    @Mock private RoundTeamPlayerRepository roundTeamPlayerRepository;
    @Mock private RoundCorrectionLogRepository roundCorrectionLogRepository;
    @Mock private RoundTeeRepository roundTeeRepository;
    @Mock private RoundTeeHoleRepository roundTeeHoleRepository;
    @Mock private ScorecardRepository scorecardRepository;
    @Mock private HoleScoreRepository holeScoreRepository;
    @Mock private TeamHoleScoreRepository teamHoleScoreRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseTeeRepository courseTeeRepository;
    @Mock private CourseHoleRepository courseHoleRepository;
    @Mock private CourseTeeComboHoleRepository courseTeeComboHoleRepository;
    @Mock private RoundHandicapService roundHandicapService;
    @Mock private PrizeScheduleRepository prizeScheduleRepository;
    @Mock private PrizeWinningRepository prizeWinningRepository;
    @Mock private TripService tripService;

    private TripInitializationService service;

    @BeforeEach
    void setUp() {
        TripStartInitializationService tripStartInitializationService = new TripStartInitializationService(
                tripRepository,
                tripPlayerRepository,
                tripPlannedRoundRepository,
                tripPlannedRoundEventRepository,
                roundRepository,
                roundEventRepository,
                roundTeeRepository,
                roundTeeHoleRepository,
                scorecardRepository,
                courseRepository,
                courseTeeRepository,
                courseHoleRepository,
                courseTeeComboHoleRepository,
                roundHandicapService,
                tripService
        );

        TripStartResetService tripStartResetService = new TripStartResetService(
                tripRepository,
                roundRepository,
                roundGroupRepository,
                roundGroupPlayerRepository,
                roundTeamRepository,
                roundTeamPlayerRepository,
                roundCorrectionLogRepository,
                roundTeeRepository,
                roundTeeHoleRepository,
                scorecardRepository,
                holeScoreRepository,
                teamHoleScoreRepository,
                prizeScheduleRepository,
                prizeWinningRepository
        );

        service = new TripInitializationService(
                tripStartInitializationService,
                tripStartResetService
        );
    }

    @Test
    void initializeTrip_shouldRejectMissingTrip() {
        when(tripRepository.findById(41L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeTrip(41L)
        );

        assertEquals("Trip not found: 41", ex.getMessage());
    }

    @Test
    void initializeTrip_shouldRejectAlreadyInitializedTrip() {
        Trip trip = trip(false, TripStatus.PLANNING);
        trip.setInitialized(true);
        when(tripRepository.findById(42L)).thenReturn(Optional.of(trip));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.initializeTrip(42L)
        );

        assertEquals("Trip is already initialized.", ex.getMessage());
        verify(tripPlayerRepository, never()).findByTrip(trip);
    }

    @Test
    void initializeTrip_shouldRequirePlayersBeforeValidationAndRoundCreation() {
        Trip trip = trip(false, TripStatus.PLANNING);
        when(tripRepository.findById(43L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip(trip)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.initializeTrip(43L)
        );

        assertEquals("Trip must have players before initialization.", ex.getMessage());
        verify(tripService, never()).validateTripCanStart(43L);
    }

    @Test
    void initializeTrip_shouldRequirePlannedRoundsAfterTripStartValidation() throws Exception {
        Trip trip = trip(false, TripStatus.PLANNING);
        TripPlayer player = new TripPlayer();
        when(tripRepository.findById(44L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip(trip)).thenReturn(List.of(player));
        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.initializeTrip(44L)
        );

        assertEquals("Trip must have planned rounds before initialization.", ex.getMessage());
        verify(tripService).validateTripCanStart(44L);
    }

    @Test
    void resetTripStart_shouldRejectTripThatHasNotStarted() {
        Trip trip = trip(false, TripStatus.PLANNING);
        when(tripRepository.findById(45L)).thenReturn(Optional.of(trip));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.resetTripStart(45L)
        );

        assertEquals("Trip has not been started.", ex.getMessage());
        verify(roundRepository, never()).deleteByTrip_Id(45L);
    }

    @Test
    void resetTripStart_shouldDeleteInitializedArtifactsAndReturnTripToPlanning() {
        Trip trip = trip(true, TripStatus.IN_PROGRESS);
        when(tripRepository.findById(46L)).thenReturn(Optional.of(trip));

        service.resetTripStart(46L);

        verify(prizeWinningRepository).deleteByTrip_Id(46L);
        verify(prizeScheduleRepository).clearRoundReferencesByTripId(46L);
        verify(teamHoleScoreRepository).deleteByRoundTeam_Round_Trip_Id(46L);
        verify(holeScoreRepository).deleteByScorecard_Round_Trip_Id(46L);
        verify(scorecardRepository).deleteByRound_Trip_Id(46L);
        verify(roundGroupPlayerRepository).deleteByRoundGroup_Round_Trip_Id(46L);
        verify(roundGroupRepository).deleteByRound_Trip_Id(46L);
        verify(roundTeamPlayerRepository).deleteByRoundTeam_Round_Trip_Id(46L);
        verify(roundTeamRepository).deleteByRound_Trip_Id(46L);
        verify(roundCorrectionLogRepository).deleteByRound_Trip_Id(46L);
        verify(roundRepository).clearDefaultRoundTeeByTripId(46L);
        verify(roundTeeHoleRepository).deleteByRoundTee_Round_Trip_Id(46L);
        verify(roundTeeRepository).deleteByRound_Trip_Id(46L);
        verify(roundRepository).deleteByTrip_Id(46L);

        assertFalse(trip.getInitialized());
        assertEquals(TripStatus.PLANNING, trip.getStatus());
        verify(tripRepository).save(trip);
    }

    private Trip trip(boolean initialized, TripStatus status) {
        Trip trip = new Trip();
        trip.setName("Myrtle Test");
        trip.setTripYear(2027);
        trip.setTripCode("MT27");
        trip.setInitialized(initialized);
        trip.setStatus(status);
        return trip;
    }
}
