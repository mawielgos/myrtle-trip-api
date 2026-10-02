package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.SaveRoundScrambleSeedingRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundScrambleSeedRound;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundScrambleSeedRoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.round.model.RoundFormat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoundTeamAssignmentScrambleSeedingTest {

    @Test
    void saveScrambleSeedingRounds_rejectsNonScrambleRound() {
        Fixture fixture = new Fixture();
        Round round = round(40L, 4, LocalDate.of(2026, 3, 4), trip(1L));

        when(fixture.roundRepository.findById(40L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.isScrambleRound(round)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> fixture.service.saveScrambleSeedingRounds(40L, new SaveRoundScrambleSeedingRequest())
        );

        assertEquals(
                "Scramble seeding rounds can only be changed for a round with a Scramble event.",
                exception.getMessage()
        );
        verify(fixture.roundRepository, never()).save(round);
    }

    @Test
    void saveScrambleSeedingRounds_rejectsTeamSizeOutsideTwoThroughFour() {
        Fixture fixture = new Fixture();
        Round round = round(41L, 4, LocalDate.of(2026, 3, 4), trip(1L));
        SaveRoundScrambleSeedingRequest request = new SaveRoundScrambleSeedingRequest();
        request.setScrambleTeamSize(5);

        when(fixture.roundRepository.findById(41L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.isScrambleRound(round)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.saveScrambleSeedingRounds(41L, request)
        );

        assertEquals("Scramble team size must be 2, 3, or 4.", exception.getMessage());
        verify(fixture.roundRepository, never()).save(round);
    }

    @Test
    void saveScrambleSeedingRounds_rejectsTeamSizeChangeAfterScoringStarts() {
        Fixture fixture = new Fixture();
        Round round = round(42L, 4, LocalDate.of(2026, 3, 4), trip(1L));
        RoundTeam scoredTeam = mock(RoundTeam.class);
        SaveRoundScrambleSeedingRequest request = new SaveRoundScrambleSeedingRequest();
        request.setScrambleTeamSize(3);

        when(fixture.roundRepository.findById(42L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.isScrambleRound(round)).thenReturn(true);
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(42L)).thenReturn(List.of(scoredTeam));
        when(scoredTeam.getScrambleTotalScore()).thenReturn(70);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> fixture.service.saveScrambleSeedingRounds(42L, request)
        );

        assertEquals(
                "Cannot change Scramble team size after Scramble scoring has started.",
                exception.getMessage()
        );
        verify(fixture.roundRepository, never()).save(round);
    }

    @Test
    void saveScrambleSeedingRounds_normalizesUnknownSeedingMethod() {
        Fixture fixture = new Fixture();
        Trip trip = trip(2L);
        Round round = round(43L, 5, LocalDate.of(2026, 3, 5), trip);
        SaveRoundScrambleSeedingRequest request = new SaveRoundScrambleSeedingRequest();
        request.setSeedingMethod(" something-unsupported ");

        when(fixture.roundRepository.findById(43L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.isScrambleRound(round)).thenReturn(true);
        when(fixture.tripPlannedRoundRepository.findByTrip_IdOrderByRoundNumberAsc(2L)).thenReturn(List.of());

        RoundTeamAssignmentPageResponse result =
                fixture.service.saveScrambleSeedingRounds(43L, request);

        assertSame(fixture.pageResponse, result);
        verify(round).setScrambleSeedingMethod("CURRENT_HANDICAP_INDEX");
        verify(fixture.roundRepository).save(round);
        verify(fixture.roundScrambleSeedRoundRepository).deleteByScrambleRound_Id(43L);
        verify(fixture.roundScrambleSeedRoundRepository).flush();
    }

    @Test
    void saveScrambleSeedingRounds_savesOnlySelectedEligiblePriorRounds() {
        Fixture fixture = new Fixture();
        Trip trip = trip(3L);
        Round scrambleRound = round(44L, 5, LocalDate.of(2026, 3, 5), trip);

        TripPlannedRound priorSelected = plannedRound(
                101L, 4, LocalDate.of(2026, 3, 4), RoundFormat.STROKE_PLAY);
        TripPlannedRound futureSelected = plannedRound(
                102L, 6, LocalDate.of(2026, 3, 6), RoundFormat.STROKE_PLAY);
        TripPlannedRound priorNotSelected = plannedRound(
                103L, 3, LocalDate.of(2026, 3, 3), RoundFormat.STROKE_PLAY);

        SaveRoundScrambleSeedingRequest request = new SaveRoundScrambleSeedingRequest();
        request.setIncludedPlannedRoundIds(List.of(101L, 102L));

        when(fixture.roundRepository.findById(44L)).thenReturn(Optional.of(scrambleRound));
        when(fixture.roundEventCapabilityService.isScrambleRound(scrambleRound)).thenReturn(true);
        when(fixture.tripPlannedRoundRepository.findByTrip_IdOrderByRoundNumberAsc(3L))
                .thenReturn(List.of(priorSelected, futureSelected, priorNotSelected));
        when(fixture.tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(101L))
                .thenReturn(List.of());
        when(fixture.tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(102L))
                .thenReturn(List.of());
        when(fixture.tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(103L))
                .thenReturn(List.of());

        fixture.service.saveScrambleSeedingRounds(44L, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RoundScrambleSeedRound>> captor =
                ArgumentCaptor.forClass((Class) List.class);
        verify(fixture.roundScrambleSeedRoundRepository).saveAll(captor.capture());

        List<RoundScrambleSeedRound> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertSame(scrambleRound, saved.get(0).getScrambleRound());
        assertSame(priorSelected, saved.get(0).getPlannedRound());
    }

    private static Trip trip(Long id) {
        Trip trip = mock(Trip.class);
        when(trip.getId()).thenReturn(id);
        return trip;
    }

    private static Round round(Long id, int roundNumber, LocalDate roundDate, Trip trip) {
        Round round = mock(Round.class);
        when(round.getId()).thenReturn(id);
        when(round.getRoundNumber()).thenReturn(roundNumber);
        when(round.getRoundDate()).thenReturn(roundDate);
        when(round.getTrip()).thenReturn(trip);
        return round;
    }

    private static TripPlannedRound plannedRound(
            Long id,
            int roundNumber,
            LocalDate roundDate,
            RoundFormat format
    ) {
        TripPlannedRound plannedRound = mock(TripPlannedRound.class);
        when(plannedRound.getId()).thenReturn(id);
        when(plannedRound.getRoundNumber()).thenReturn(roundNumber);
        when(plannedRound.getRoundDate()).thenReturn(roundDate);
        when(plannedRound.getFormat()).thenReturn(format);
        return plannedRound;
    }

    private static final class Fixture {
        private final RoundRepository roundRepository = mock(RoundRepository.class);
        private final RoundTeamRepository roundTeamRepository = mock(RoundTeamRepository.class);
        private final RoundTeamPlayerRepository roundTeamPlayerRepository = mock(RoundTeamPlayerRepository.class);
        private final RoundTeeRepository roundTeeRepository = mock(RoundTeeRepository.class);
        private final ScorecardRepository scorecardRepository = mock(ScorecardRepository.class);
        private final TeamHoleScoreRepository teamHoleScoreRepository = mock(TeamHoleScoreRepository.class);
        private final RoundTeeResolver roundTeeResolver = mock(RoundTeeResolver.class);
        private final RoundTeeProvisioningService roundTeeProvisioningService = mock(RoundTeeProvisioningService.class);
        private final TripHandicapService tripHandicapService = mock(TripHandicapService.class);
        private final TripPlayerRepository tripPlayerRepository = mock(TripPlayerRepository.class);
        private final TripPlannedRoundRepository tripPlannedRoundRepository = mock(TripPlannedRoundRepository.class);
        private final RoundScrambleSeedRoundRepository roundScrambleSeedRoundRepository =
                mock(RoundScrambleSeedRoundRepository.class);
        private final TripPlannedRoundEventRepository tripPlannedRoundEventRepository =
                mock(TripPlannedRoundEventRepository.class);
        private final RoundCapabilityService roundCapabilityService = mock(RoundCapabilityService.class);
        private final RoundEventCapabilityService roundEventCapabilityService = mock(RoundEventCapabilityService.class);

        private final RoundTeamAssignmentPageResponse pageResponse = new RoundTeamAssignmentPageResponse();

        private final RoundTeamAssignmentService service = spy(new RoundTeamAssignmentService(
                roundRepository,
                roundTeamRepository,
                roundTeamPlayerRepository,
                roundTeeRepository,
                scorecardRepository,
                teamHoleScoreRepository,
                roundTeeResolver,
                roundTeeProvisioningService,
                tripHandicapService,
                tripPlayerRepository,
                tripPlannedRoundRepository,
                roundScrambleSeedRoundRepository,
                tripPlannedRoundEventRepository,
                roundCapabilityService,
                roundEventCapabilityService
        ));

        private Fixture() {
            doReturn(pageResponse).when(service).getAssignmentPage(anyLong());
        }
    }
}
