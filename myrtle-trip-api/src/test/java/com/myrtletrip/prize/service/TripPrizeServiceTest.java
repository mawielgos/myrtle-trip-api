package com.myrtletrip.prize.service;

import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.event.repository.RoundEventRepository;
import com.myrtletrip.prize.dto.PrizeScheduleResponse;
import com.myrtletrip.prize.dto.SavePrizeSchedulePayoutRequest;
import com.myrtletrip.prize.dto.SavePrizeScheduleRequest;
import com.myrtletrip.prize.dto.SaveTripPrizeSchedulesRequest;
import com.myrtletrip.prize.entity.PrizeSchedule;
import com.myrtletrip.prize.entity.PrizeSchedulePayout;
import com.myrtletrip.prize.model.PrizePayoutUnit;
import com.myrtletrip.prize.model.PrizeResultScope;
import com.myrtletrip.prize.repository.PrizeSchedulePayoutRepository;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlannedRoundEvent;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripPrizeServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private TripPlannedRoundRepository tripPlannedRoundRepository;
    @Mock private TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    @Mock private RoundEventRepository roundEventRepository;
    @Mock private PrizeScheduleRepository prizeScheduleRepository;
    @Mock private PrizeSchedulePayoutRepository prizeSchedulePayoutRepository;
    @Mock private TripTournamentRepository tripTournamentRepository;
    @Mock private TripEditingGuardService tripEditingGuardService;

    private TripPrizeService service;

    @BeforeEach
    void setUp() {
        TripPrizeReadModelService readModelService = new TripPrizeReadModelService(
                tripRepository,
                roundRepository,
                tripPlannedRoundRepository,
                tripPlannedRoundEventRepository,
                roundEventRepository,
                prizeScheduleRepository,
                prizeSchedulePayoutRepository,
                tripTournamentRepository);
        TripPrizeCommandService commandService = new TripPrizeCommandService(
                tripRepository,
                prizeScheduleRepository,
                prizeSchedulePayoutRepository,
                tripEditingGuardService,
                readModelService);
        service = new TripPrizeService(readModelService, commandService);
    }

    @Test
    void roundEventGameKey_shouldRoundTripEventTypeAndRejectInvalidKeys() {
        String key = TripPrizeService.buildRoundEventGameKey(3, RoundEventType.TEAM_TWO_MAN_LOW_NET);

        assertEquals("ROUND_NUMBER_3_EVENT_TEAM_TWO_MAN_LOW_NET", key);
        assertEquals(RoundEventType.TEAM_TWO_MAN_LOW_NET, TripPrizeService.parseRoundEventTypeFromGameKey(key));
        assertNull(TripPrizeService.parseRoundEventTypeFromGameKey(null));
        assertNull(TripPrizeService.parseRoundEventTypeFromGameKey("ROUND_NUMBER_3"));
        assertNull(TripPrizeService.parseRoundEventTypeFromGameKey("ROUND_NUMBER_3_EVENT_NOT_A_REAL_EVENT"));
    }

    @Test
    void getActivePrizeScheduleKeys_shouldIncludePlannedRoundEventWhenRoundDoesNotExistYet() {
        Long tripId = 11L;
        Trip trip = trip(11L);
        TripPlannedRound plannedRound = plannedRound(101L, trip, 2);
        TripPlannedRoundEvent event = plannedEvent(RoundEventType.TEAM_SCRAMBLE, "Scramble", 1);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTournamentRepository.findByTrip_Id(tripId)).thenReturn(Optional.empty());
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId)).thenReturn(List.of());
        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip)).thenReturn(List.of(plannedRound));
        when(tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(101L)).thenReturn(List.of(event));

        Set<String> keys = service.getActivePrizeScheduleKeys(tripId);

        assertEquals(Set.of("ROUND_NUMBER_2_EVENT_TEAM_SCRAMBLE"), keys);
    }

    @Test
    void getPrizeSchedules_shouldCreateDefaultScheduleForPlannedRoundEvent() {
        Long tripId = 12L;
        Trip trip = trip(12L);
        TripPlannedRound plannedRound = plannedRound(102L, trip, 1);
        TripPlannedRoundEvent event = plannedEvent(RoundEventType.INDIVIDUAL_LOW_NET, "Daily Low Net", 1);
        List<PrizeSchedule> schedules = new ArrayList<>();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTournamentRepository.findByTrip_Id(tripId)).thenReturn(Optional.empty());
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId)).thenReturn(List.of());
        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip)).thenReturn(List.of(plannedRound));
        when(tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(102L)).thenReturn(List.of(event));
        when(prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId)).thenReturn(schedules);
        when(prizeScheduleRepository.save(any(PrizeSchedule.class))).thenAnswer(invocation -> {
            PrizeSchedule schedule = invocation.getArgument(0);
            if (!schedules.contains(schedule)) {
                schedules.add(schedule);
            }
            return schedule;
        });
        when(prizeSchedulePayoutRepository.findByPrizeSchedule_IdOrderByFinishingPlaceAsc(any())).thenReturn(List.of());

        List<PrizeScheduleResponse> responses = service.getPrizeSchedules(tripId);

        assertEquals(1, responses.size());
        PrizeScheduleResponse response = responses.get(0);
        assertEquals("ROUND_NUMBER_1_EVENT_INDIVIDUAL_LOW_NET", response.getGameKey());
        assertEquals("Round 1 - Daily Low Net", response.getGameName());
        assertEquals("PLAYER", response.getResultScope());
        assertEquals("PLAYER", response.getPayoutUnit());
        assertEquals(tripId, response.getTripId());
        verify(prizeScheduleRepository, atLeastOnce()).save(any(PrizeSchedule.class));
    }

    @Test
    void savePrizeSchedules_shouldHonorTripEditingGuardBeforeChangingSchedules() {
        Long tripId = 13L;
        Trip trip = trip(13L);
        SaveTripPrizeSchedulesRequest request = new SaveTripPrizeSchedulesRequest();
        RuntimeException blocked = new IllegalStateException("locked");

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        doThrow(blocked).when(tripEditingGuardService).assertStructureEditable(trip);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> service.savePrizeSchedules(tripId, request));

        assertSame(blocked, thrown);
        verifyNoInteractions(prizeScheduleRepository, prizeSchedulePayoutRepository);
    }

    @Test
    void savePrizeSchedules_shouldReplacePayoutsAndIgnoreInvalidPlaces() {
        Long tripId = 14L;
        Trip trip = trip(14L);
        TripPlannedRound plannedRound = plannedRound(104L, trip, 4);
        TripPlannedRoundEvent event = plannedEvent(RoundEventType.INDIVIDUAL_LOW_GROSS, "Low Gross", 1);
        PrizeSchedule schedule = new PrizeSchedule();
        schedule.setTrip(trip);
        schedule.setGameKey("ROUND_NUMBER_4_EVENT_INDIVIDUAL_LOW_GROSS");
        schedule.setGameName("Round 4 - Low Gross");
        schedule.setResultScope(PrizeResultScope.PLAYER);
        schedule.setPayoutUnit(PrizePayoutUnit.PLAYER);
        setId(schedule, 77L);
        List<PrizeSchedule> schedules = new ArrayList<>(List.of(schedule));

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripTournamentRepository.findByTrip_Id(tripId)).thenReturn(Optional.empty());
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId)).thenReturn(List.of());
        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip)).thenReturn(List.of(plannedRound));
        when(tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(104L)).thenReturn(List.of(event));
        when(prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId)).thenReturn(schedules);
        when(prizeScheduleRepository.save(any(PrizeSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(prizeSchedulePayoutRepository.findByPrizeSchedule_IdOrderByFinishingPlaceAsc(77L)).thenReturn(List.of());

        SavePrizeScheduleRequest scheduleRequest = new SavePrizeScheduleRequest();
        scheduleRequest.setGameKey(schedule.getGameKey());
        scheduleRequest.setGameName("  Gross Prize  ");
        scheduleRequest.setResultScope("player");
        scheduleRequest.setPayoutUnit("player");
        scheduleRequest.setPayouts(List.of(
                payout(2, new BigDecimal("25.00")),
                payout(0, new BigDecimal("99.00")),
                payout(1, null)));
        SaveTripPrizeSchedulesRequest request = new SaveTripPrizeSchedulesRequest();
        request.setSchedules(List.of(scheduleRequest));

        List<PrizeScheduleResponse> responses = service.savePrizeSchedules(tripId, request);

        assertEquals(1, responses.size());
        assertEquals("Gross Prize", schedule.getGameName());
        verify(prizeSchedulePayoutRepository).deleteByPrizeSchedule_Id(77L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PrizeSchedulePayout>> captor = ArgumentCaptor.forClass(List.class);
        verify(prizeSchedulePayoutRepository).saveAll(captor.capture());
        List<PrizeSchedulePayout> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals(1, saved.get(0).getFinishingPlace());
        assertEquals(BigDecimal.ZERO, saved.get(0).getAmountPerPlayer());
        assertEquals(2, saved.get(1).getFinishingPlace());
        assertEquals(new BigDecimal("25.00"), saved.get(1).getAmountPerPlayer());
        assertTrue(saved.stream().allMatch(p -> p.getPrizeSchedule() == schedule));
    }

    private Trip trip(Long id) {
        Trip trip = mock(Trip.class);
        lenient().when(trip.getId()).thenReturn(id);
        return trip;
    }

    private TripPlannedRound plannedRound(Long id, Trip trip, Integer roundNumber) {
        TripPlannedRound round = mock(TripPlannedRound.class);
        lenient().when(round.getId()).thenReturn(id);
        lenient().when(round.getTrip()).thenReturn(trip);
        lenient().when(round.getRoundNumber()).thenReturn(roundNumber);
        lenient().when(round.getFormat()).thenReturn(null);
        lenient().when(round.getScrambleTeamSize()).thenReturn(4);
        return round;
    }

    private TripPlannedRoundEvent plannedEvent(RoundEventType type, String name, Integer order) {
        TripPlannedRoundEvent event = mock(TripPlannedRoundEvent.class);
        lenient().when(event.getEventType()).thenReturn(type);
        lenient().when(event.getEventName()).thenReturn(name);
        lenient().when(event.getEventOrder()).thenReturn(order);
        return event;
    }

    private SavePrizeSchedulePayoutRequest payout(Integer place, BigDecimal amount) {
        SavePrizeSchedulePayoutRequest request = new SavePrizeSchedulePayoutRequest();
        request.setFinishingPlace(place);
        request.setAmountPerPlayer(amount);
        return request;
    }

    private void setId(Object target, Long id) {
        try {
            var field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
