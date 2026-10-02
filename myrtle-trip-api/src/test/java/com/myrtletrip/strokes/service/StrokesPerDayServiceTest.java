package com.myrtletrip.strokes.service;

import com.myrtletrip.handicap.service.CourseHandicapService;
import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.round.service.RoundEventCapabilityService;
import com.myrtletrip.round.service.RoundTeeProvisioningService;
import com.myrtletrip.round.service.ScorecardHandicapService;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.strokes.dto.StrokesPerDayResponse;
import com.myrtletrip.strokes.dto.StrokesPerDayTeePlanItemRequest;
import com.myrtletrip.strokes.dto.StrokesPerDayTeePlanSaveRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrokesPerDayServiceTest {

    private TripRepository tripRepository;
    private TripPlayerRepository tripPlayerRepository;
    private RoundRepository roundRepository;
    private RoundTeeHoleRepository roundTeeHoleRepository;
    private RoundTeeRepository roundTeeRepository;
    private RoundTeeProvisioningService roundTeeProvisioningService;
    private TripHandicapService tripHandicapService;
    private CourseHandicapService courseHandicapService;
    private ScorecardRepository scorecardRepository;
    private ScorecardHandicapService scorecardHandicapService;
    private RoundEventCapabilityService roundEventCapabilityService;
    private StrokesPerDayTeePlanCommandService teePlanCommandService;
    private StrokesPerDayReadModelService readModelService;
    private StrokesPerDayService service;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepository.class);
        tripPlayerRepository = mock(TripPlayerRepository.class);
        roundRepository = mock(RoundRepository.class);
        roundTeeHoleRepository = mock(RoundTeeHoleRepository.class);
        roundTeeRepository = mock(RoundTeeRepository.class);
        roundTeeProvisioningService = mock(RoundTeeProvisioningService.class);
        tripHandicapService = mock(TripHandicapService.class);
        courseHandicapService = mock(CourseHandicapService.class);
        scorecardRepository = mock(ScorecardRepository.class);
        scorecardHandicapService = mock(ScorecardHandicapService.class);
        roundEventCapabilityService = mock(RoundEventCapabilityService.class);
        teePlanCommandService = new StrokesPerDayTeePlanCommandService(
                tripRepository,
                roundRepository,
                roundTeeRepository,
                scorecardRepository,
                scorecardHandicapService
        );

        readModelService = new StrokesPerDayReadModelService(
                tripRepository,
                tripPlayerRepository,
                roundRepository,
                roundTeeHoleRepository,
                roundTeeRepository,
                roundTeeProvisioningService,
                tripHandicapService,
                courseHandicapService,
                scorecardRepository,
                roundEventCapabilityService
        );

        service = new StrokesPerDayService(
                readModelService,
                teePlanCommandService
        );
    }

    @Test
    void getStrokesPerDay_shouldReturnTripMetadataWhenNoRoundsOrPlayersExist() {
        Trip trip = mock(Trip.class);
        when(trip.getId()).thenReturn(11L);
        when(trip.getName()).thenReturn("Myrtle 2027");
        when(trip.getTripCode()).thenReturn("MYR27");
        when(trip.getTripYear()).thenReturn(2027);
        when(tripRepository.findById(11L)).thenReturn(Optional.of(trip));
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(11L)).thenReturn(List.of());
        when(tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(11L)).thenReturn(List.of());

        StrokesPerDayResponse response = service.getStrokesPerDay(11L);

        assertEquals(11L, response.getTripId());
        assertEquals("Myrtle 2027", response.getTripName());
        assertEquals("MYR27", response.getTripCode());
        assertEquals(2027, response.getTripYear());
        assertEquals(0, response.getRounds().size());
        assertEquals(0, response.getPlayers().size());
    }

    @Test
    void getStrokesPerDay_shouldExcludeRoundsThatDoNotRequirePlayerScorecards() {
        Trip trip = mock(Trip.class);
        Round round = mock(Round.class);
        when(trip.getId()).thenReturn(11L);
        when(tripRepository.findById(11L)).thenReturn(Optional.of(trip));
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(11L)).thenReturn(List.of(round));
        when(tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(11L)).thenReturn(List.of());
        when(roundEventCapabilityService.getCapabilities(round)).thenReturn(
                new RoundEventCapabilityService.RoundEventCapabilities(
                        true, false, true, false, false, true, 4
                )
        );

        StrokesPerDayResponse response = service.getStrokesPerDay(11L);

        assertEquals(0, response.getRounds().size());
        verify(roundTeeProvisioningService, never()).ensureRoundTeeOptions(round);
    }

    @Test
    void saveTeePlan_shouldRejectRoundFromAnotherTrip() {
        Trip trip = mock(Trip.class);
        Trip otherTrip = mock(Trip.class);
        Round round = mock(Round.class);
        when(trip.getId()).thenReturn(11L);
        when(otherTrip.getId()).thenReturn(99L);
        when(tripRepository.findById(11L)).thenReturn(Optional.of(trip));
        when(roundRepository.findById(22L)).thenReturn(Optional.of(round));
        when(round.getTrip()).thenReturn(otherTrip);

        StrokesPerDayTeePlanSaveRequest request = request(7L, 22L, 33L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveTeePlan(11L, request)
        );

        assertEquals("Round 22 does not belong to trip 11", ex.getMessage());
    }

    @Test
    void saveTeePlan_shouldRejectFinalizedRound() {
        Trip trip = mock(Trip.class);
        Round round = mock(Round.class);
        when(trip.getId()).thenReturn(11L);
        when(tripRepository.findById(11L)).thenReturn(Optional.of(trip));
        when(roundRepository.findById(22L)).thenReturn(Optional.of(round));
        when(round.getId()).thenReturn(22L);
        when(round.getTrip()).thenReturn(trip);
        when(round.getFinalized()).thenReturn(true);

        StrokesPerDayTeePlanSaveRequest request = request(7L, 22L, 33L);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.saveTeePlan(11L, request)
        );

        assertEquals("Tee planning is locked for round 22 because it is Finalized", ex.getMessage());
        verify(roundTeeRepository, never()).findById(33L);
    }

    @Test
    void saveTeePlan_shouldApplyScorecardTeeAndReturnRefreshedResponse() {
        Trip trip = mock(Trip.class);
        Round round = mock(Round.class);
        RoundTee roundTee = mock(RoundTee.class);
        Scorecard scorecard = mock(Scorecard.class);

        when(trip.getId()).thenReturn(11L);
        when(trip.getName()).thenReturn("Myrtle 2027");
        when(tripRepository.findById(11L)).thenReturn(Optional.of(trip));
        when(roundRepository.findById(22L)).thenReturn(Optional.of(round));
        when(round.getId()).thenReturn(22L);
        when(round.getTrip()).thenReturn(trip);
        when(round.getFinalized()).thenReturn(false);
        when(scorecardRepository.findByRound_Id(22L)).thenReturn(List.of());
        when(roundTeeRepository.findById(33L)).thenReturn(Optional.of(roundTee));
        when(roundTee.getId()).thenReturn(33L);
        when(roundTee.getRound()).thenReturn(round);
        when(scorecard.getId()).thenReturn(44L);
        when(scorecardRepository.findByRound_IdAndPlayer_Id(22L, 7L)).thenReturn(Optional.of(scorecard));
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(11L)).thenReturn(List.of());
        when(tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(11L)).thenReturn(List.of());

        StrokesPerDayResponse response = service.saveTeePlan(11L, request(7L, 22L, 33L));

        verify(scorecardHandicapService).setScorecardTee(44L, 33L);
        assertEquals(11L, response.getTripId());
        assertEquals("Myrtle 2027", response.getTripName());
    }

    private StrokesPerDayTeePlanSaveRequest request(Long playerId, Long roundId, Long roundTeeId) {
        StrokesPerDayTeePlanItemRequest change = new StrokesPerDayTeePlanItemRequest();
        change.setPlayerId(playerId);
        change.setRoundId(roundId);
        change.setRoundTeeId(roundTeeId);

        StrokesPerDayTeePlanSaveRequest request = new StrokesPerDayTeePlanSaveRequest();
        request.setChanges(List.of(change));
        return request;
    }
}
