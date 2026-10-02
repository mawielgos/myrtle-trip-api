package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.trip.dto.SaveTripPlannedRoundsRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripPlannedRoundServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlannedRoundRepository tripPlannedRoundRepository;
    @Mock private TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseTeeRepository courseTeeRepository;
    @Mock private CourseHoleRepository courseHoleRepository;
    @Mock private CourseTeeComboHoleRepository courseTeeComboHoleRepository;

    private TripPlannedRoundService service;

    @BeforeEach
    void setUp() {
        TripPlannedRoundLifecycleService lifecycleService =
                new TripPlannedRoundLifecycleService(tripPlannedRoundRepository);
        TripPlannedRoundCommandService commandService =
                new TripPlannedRoundCommandService(
                        tripRepository,
                        tripPlannedRoundRepository,
                        tripPlannedRoundEventRepository,
                        courseTeeRepository,
                        courseHoleRepository,
                        courseTeeComboHoleRepository);
        service = new TripPlannedRoundService(
                tripRepository,
                tripPlannedRoundRepository,
                tripPlannedRoundEventRepository,
                courseRepository,
                courseTeeRepository,
                lifecycleService,
                commandService);
    }

    @Test
    void resolvePlannedRoundCount_defaultsToFive() {
        assertEquals(5, service.resolvePlannedRoundCount(null));
        assertEquals(7, service.resolvePlannedRoundCount(7));
    }

    @Test
    void loadActivePlannedRounds_filtersRoundsOutsideConfiguredCount() {
        Trip trip = new Trip();
        trip.setPlannedRoundCount(2);

        TripPlannedRound round1 = round(trip, 1);
        TripPlannedRound round2 = round(trip, 2);
        TripPlannedRound round3 = round(trip, 3);

        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip))
                .thenReturn(List.of(round1, round2, round3));

        List<TripPlannedRound> result = service.loadActivePlannedRounds(trip);

        assertEquals(List.of(round1, round2), result);
    }

    @Test
    void savePlannedRounds_rejectsStartedTripBeforeChangingConfiguration() {
        Trip trip = new Trip();
        trip.setInitialized(true);
        trip.setStatus(TripStatus.IN_PROGRESS);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.savePlannedRounds(10L, new SaveTripPlannedRoundsRequest()));

        assertEquals("Trip setup cannot be changed after the trip has started.", error.getMessage());
    }

    @Test
    void createDefaultPlannedRounds_doesNothingWhenRoundsAlreadyExist() {
        Trip trip = new Trip();
        trip.setPlannedRoundCount(5);
        when(tripPlannedRoundRepository.countByTrip(trip)).thenReturn(1L);

        service.createDefaultPlannedRounds(trip);

        verify(tripPlannedRoundRepository).countByTrip(trip);
    }

    @Test
    void getPlannedRounds_rejectsUnknownTrip() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.getPlannedRounds(99L));

        assertEquals("Trip not found: 99", error.getMessage());
    }


    @Test
    void validateExistingPlannedRoundsWithinTripDates_rejectsRoundBeforeTripStart() {
        Trip trip = new Trip();
        trip.setTripStartDate(LocalDate.of(2026, 4, 10));
        trip.setTripEndDate(LocalDate.of(2026, 4, 15));

        TripPlannedRound plannedRound = round(trip, 2);
        plannedRound.setRoundDate(LocalDate.of(2026, 4, 9));
        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip))
                .thenReturn(List.of(plannedRound));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateExistingPlannedRoundsWithinTripDates(trip));

        assertEquals("Round 2 date must be on or after the trip start date.", error.getMessage());
    }

    @Test
    void syncPlannedRoundsToTripRoundCount_addsMissingRounds() {
        Trip trip = new Trip();
        trip.setPlannedRoundCount(3);
        TripPlannedRound round1 = round(trip, 1);
        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip))
                .thenReturn(List.of(round1));

        service.syncPlannedRoundsToTripRoundCount(trip);

        verify(tripPlannedRoundRepository, times(2)).save(any(TripPlannedRound.class));
        verify(tripPlannedRoundRepository).flush();
    }

    @Test
    void syncPlannedRoundsToTripRoundCount_rejectsReductionWhenRemovedRoundHasSetupDetails() {
        Trip trip = new Trip();
        trip.setPlannedRoundCount(2);
        TripPlannedRound round1 = round(trip, 1);
        TripPlannedRound round2 = round(trip, 2);
        TripPlannedRound round3 = round(trip, 3);
        round3.setCourseId(77L);
        when(tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip))
                .thenReturn(List.of(round1, round2, round3));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.syncPlannedRoundsToTripRoundCount(trip));

        assertEquals(
                "Cannot reduce planned round count because Round 3 already has setup details. Clear that round first.",
                error.getMessage());
    }

    @Test
    void createDefaultPlannedRounds_createsConfiguredNumberOfBlankRounds() {
        Trip trip = new Trip();
        trip.setPlannedRoundCount(3);
        when(tripPlannedRoundRepository.countByTrip(trip)).thenReturn(0L);

        service.createDefaultPlannedRounds(trip);

        verify(tripPlannedRoundRepository, times(3)).save(any(TripPlannedRound.class));
    }

    @Test
    void hasPlannedRoundEventConfiguration_usesLegacyFormatWhenNoEventRowsExist() {
        TripPlannedRound unconfigured = new TripPlannedRound();
        TripPlannedRound configured = new TripPlannedRound();
        configured.setFormat(RoundFormat.STROKE_PLAY);

        assertFalse(service.hasPlannedRoundEventConfiguration(null));
        assertFalse(service.hasPlannedRoundEventConfiguration(unconfigured));
        assertTrue(service.hasPlannedRoundEventConfiguration(configured));
    }

    private TripPlannedRound round(Trip trip, int number) {
        TripPlannedRound round = new TripPlannedRound();
        round.setTrip(trip);
        round.setRoundNumber(number);
        return round;
    }
}
