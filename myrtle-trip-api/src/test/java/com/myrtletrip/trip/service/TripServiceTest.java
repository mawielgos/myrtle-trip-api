package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.event.repository.RoundEventRepository;
import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.handicap.source.frozen.FrozenGhinImportService;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.prize.repository.PrizeWinningRepository;
import com.myrtletrip.prize.repository.TripPlayerPayoutStatusRepository;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.service.RoundEventCapabilityService;
import com.myrtletrip.round.service.RoundTeamAutoAssignmentService;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.tournament.repository.TripTournamentRoundRepository;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripBillInventoryRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private ScorecardRepository scorecardRepository;
    @Mock private RoundGroupRepository roundGroupRepository;
    @Mock private RoundTeamRepository roundTeamRepository;
    @Mock private RoundTeamAutoAssignmentService roundTeamAutoAssignmentService;
    @Mock private RoundEventRepository roundEventRepository;
    @Mock private TripPlannedRoundRepository tripPlannedRoundRepository;
    @Mock private TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseTeeRepository courseTeeRepository;
    @Mock private CourseHoleRepository courseHoleRepository;
    @Mock private CourseTeeComboHoleRepository courseTeeComboHoleRepository;
    @Mock private TripHandicapService tripHandicapService;
    @Mock private ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    @Mock private FrozenGhinImportService frozenGhinImportService;
    @Mock private PrizeWinningRepository prizeWinningRepository;
    @Mock private PrizeScheduleRepository prizeScheduleRepository;
    @Mock private TripPlayerPayoutStatusRepository tripPlayerPayoutStatusRepository;
    @Mock private TripBillInventoryRepository tripBillInventoryRepository;
    @Mock private TripTournamentRepository tripTournamentRepository;
    @Mock private TripTournamentRoundRepository tripTournamentRoundRepository;
    @Mock private RoundEventCapabilityService roundEventCapabilityService;

    @InjectMocks
    private TripService tripService;

    @Test
    void createOrUpdateTripRoster_requiresTripCode() {
        TripSetupRequest request = validSetupRequest();
        request.setTripCode(" ");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tripService.createOrUpdateTripRoster(request));

        assertTrue(error.getMessage().contains("tripCode is required"));
    }

    @Test
    void createOrUpdateTripRoster_requiresName() {
        TripSetupRequest request = validSetupRequest();
        request.setName(null);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tripService.createOrUpdateTripRoster(request));

        assertTrue(error.getMessage().contains("name is required"));
    }

    @Test
    void createOrUpdateTripRoster_requiresTripYear() {
        TripSetupRequest request = validSetupRequest();
        request.setTripYear(null);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tripService.createOrUpdateTripRoster(request));

        assertTrue(error.getMessage().contains("tripYear is required"));
    }

    @Test
    void archiveTrip_marksTripArchivedAndPersistsIt() {
        Trip trip = trip("MYR26");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        tripService.archiveTrip(10L);

        assertTrue(Boolean.TRUE.equals(trip.getArchived()));
        assertNotNull(trip.getArchivedAt());
        verify(tripRepository).save(trip);
    }

    @Test
    void archiveTrip_isNoOpWhenAlreadyArchived() {
        Trip trip = trip("MYR26");
        trip.setArchived(Boolean.TRUE);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        tripService.archiveTrip(10L);

        verify(tripRepository, never()).save(trip);
    }

    @Test
    void restoreTrip_clearsArchivedStateAndPersistsIt() {
        Trip trip = trip("MYR26");
        trip.setArchived(Boolean.TRUE);
        trip.setArchivedAt(java.time.LocalDateTime.now());
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        tripService.restoreTrip(10L);

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
                () -> tripService.deleteTrip(10L));

        assertTrue(error.getMessage().contains("no started rounds"));
        verify(tripRepository, never()).delete(trip);
    }

    @Test
    void initializeTripGhin_rejectsTripThatHasAlreadyStarted() {
        Trip trip = trip("MYR26");
        trip.setStatus(TripStatus.IN_PROGRESS);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> tripService.initializeTripGhin(10L));

        assertTrue(error.getMessage().contains("cannot be loaded after the trip has started"));
    }

    @Test
    void initializeTripGhin_requiresPlayersBeforeImport() {
        Trip trip = trip("MYR26");
        trip.setStatus(TripStatus.PLANNING);
        trip.setInitialized(Boolean.FALSE);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip)).thenReturn(Collections.emptyList());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> tripService.initializeTripGhin(10L));

        assertTrue(error.getMessage().contains("must have players"));
    }

    private TripSetupRequest validSetupRequest() {
        TripSetupRequest request = new TripSetupRequest();
        request.setTripCode("MYR26");
        request.setName("Myrtle 2026");
        request.setTripYear(2026);
        request.setPlayerIds(Collections.emptyList());
        return request;
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
