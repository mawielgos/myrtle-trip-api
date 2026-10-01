package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
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
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.prize.repository.PrizeWinningRepository;
import com.myrtletrip.prize.repository.TripPlayerPayoutStatusRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.service.RoundEventCapabilityService;
import com.myrtletrip.round.service.RoundTeamAutoAssignmentService;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.tournament.repository.TripTournamentRoundRepository;
import com.myrtletrip.trip.dto.SaveTripPlannedRoundsRequest;
import com.myrtletrip.trip.dto.TripPlannedRoundResponse;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.entity.TripPlayer;
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
    @Mock private PrizeWinningRepository prizeWinningRepository;
    @Mock private PrizeScheduleRepository prizeScheduleRepository;
    @Mock private TripPlayerPayoutStatusRepository tripPlayerPayoutStatusRepository;
    @Mock private TripBillInventoryRepository tripBillInventoryRepository;
    @Mock private TripTournamentRepository tripTournamentRepository;
    @Mock private TripTournamentRoundRepository tripTournamentRoundRepository;
    @Mock private RoundEventCapabilityService roundEventCapabilityService;
    @Mock private TripParticipationService tripParticipationService;
    @Mock private TripLifecycleService tripLifecycleService;
    @Mock private TripGhinInitializationService tripGhinInitializationService;
    @Mock private TripStatusService tripStatusService;
    @Mock private TripPlannedRoundService tripPlannedRoundService;

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
    void archiveTrip_delegatesToLifecycleService() {
        tripService.archiveTrip(10L);
        verify(tripLifecycleService).archiveTrip(10L);
    }

    @Test
    void restoreTrip_delegatesToLifecycleService() {
        tripService.restoreTrip(10L);
        verify(tripLifecycleService).restoreTrip(10L);
    }

    @Test
    void deleteTrip_delegatesToLifecycleService() {
        tripService.deleteTrip(10L);
        verify(tripLifecycleService).deleteTrip(10L);
    }

    @Test
    void initializeTripGhin_delegatesToGhinInitializationService() throws Exception {
        tripService.initializeTripGhin(10L);

        verify(tripGhinInitializationService).initializeTripGhin(10L);
    }


    @Test
    void updateTripPlayerParticipation_delegatesPropagationThenReturnsCurrentRoster() {
        Trip trip = trip("MYR26");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip)).thenReturn(Collections.emptyList());

        tripService.updateTripPlayerParticipation(10L, 20L, "NO_SHOW");

        verify(tripParticipationService).updateParticipation(10L, 20L, "NO_SHOW");
        verify(tripPlayerRepository).findByTripOrderByDisplayOrderAsc(trip);
    }

    @Test
    void refreshTripStatusFromRounds_delegatesToStatusService() {
        tripService.refreshTripStatusFromRounds(10L);

        verify(tripStatusService).refreshTripStatusFromRounds(10L);
    }

    @Test
    void findCurrentRoundEntity_delegatesToStatusService() {
        Round currentRound = mock(Round.class);
        when(tripStatusService.findCurrentRoundEntity(10L)).thenReturn(currentRound);

        Round result = tripService.findCurrentRoundEntity(10L);

        assertTrue(result == currentRound);
        verify(tripStatusService).findCurrentRoundEntity(10L);
    }

    @Test
    void getPlannedRounds_delegatesToPlannedRoundService() {
        List<TripPlannedRoundResponse> expected = Collections.emptyList();
        when(tripPlannedRoundService.getPlannedRounds(10L)).thenReturn(expected);

        List<TripPlannedRoundResponse> result = tripService.getPlannedRounds(10L);

        assertTrue(result == expected);
        verify(tripPlannedRoundService).getPlannedRounds(10L);
    }

    @Test
    void savePlannedRounds_delegatesToPlannedRoundService() {
        SaveTripPlannedRoundsRequest request = new SaveTripPlannedRoundsRequest();
        List<TripPlannedRoundResponse> expected = Collections.emptyList();
        when(tripPlannedRoundService.savePlannedRounds(10L, request)).thenReturn(expected);

        List<TripPlannedRoundResponse> result = tripService.savePlannedRounds(10L, request);

        assertTrue(result == expected);
        verify(tripPlannedRoundService).savePlannedRounds(10L, request);
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
