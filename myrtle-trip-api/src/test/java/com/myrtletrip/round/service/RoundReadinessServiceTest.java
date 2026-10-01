package com.myrtletrip.round.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.round.dto.RoundReadinessResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.exceptionmodel.service.RoundTeamExceptionService;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.trip.entity.Trip;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoundReadinessServiceTest {

    @Test
    void getReadiness_shouldReturnNotFoundWhenRoundDoesNotExist() {
        Fixture fixture = new Fixture();
        when(fixture.roundRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.service.getReadiness(404L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Round not found: 404", exception.getReason());
        verifyNoInteractions(
                fixture.scorecardRepository,
                fixture.roundGroupRepository,
                fixture.roundTeamRepository,
                fixture.teamHoleScoreRepository,
                fixture.roundTeeResolver,
                fixture.roundEventCapabilityService,
                fixture.roundTeamExceptionService
        );
    }

    @Test
    void getReadiness_shouldReportConfiguredRoundWithoutScorecardsAsNotReady() {
        Fixture fixture = new Fixture();
        Round round = fixture.configuredStrokePlayRound(17L, 3);
        RoundEventCapabilityService.RoundEventCapabilities capabilities = individualGrossCapabilities();

        when(fixture.roundRepository.findById(17L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.getCapabilities(round)).thenReturn(capabilities);
        when(fixture.roundEventCapabilityService.hasConfiguredEvents(round)).thenReturn(true);
        when(fixture.roundEventCapabilityService.requiresNetScores(round)).thenReturn(false);
        when(fixture.scorecardRepository.findByRound_Id(17L)).thenReturn(List.of());
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(17L)).thenReturn(List.of());
        when(fixture.roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(17L)).thenReturn(List.of());

        RoundReadinessResponse response = fixture.service.getReadiness(17L);

        assertTrue(response.isRoundConfigured());
        assertFalse(response.isScorecardsReady());
        assertFalse(response.isTeesReady());
        assertFalse(response.isHandicapsReady());
        assertFalse(response.isGroupsReady());
        assertTrue(response.isTeamsReady());
        assertFalse(response.isReadyForScoring());
        assertFalse(response.isReadyForFinalization());
        assertFalse(response.isReady());
        assertEquals(0, response.getScorecardCount());
        assertEquals(1, response.getMissingScoreCount());
        assertEquals(List.of("No scorecards exist for this round."), response.getBlockingIssues());
    }

    @Test
    void getReadiness_shouldIgnoreNoShowsWhenCountingActiveScorecards() {
        Fixture fixture = new Fixture();
        Round round = fixture.configuredStrokePlayRound(18L, 4);
        RoundEventCapabilityService.RoundEventCapabilities capabilities = individualGrossCapabilities();

        Scorecard noShow = new Scorecard();
        noShow.setParticipationStatus(ScorecardParticipationStatus.NO_SHOW);

        when(fixture.roundRepository.findById(18L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.getCapabilities(round)).thenReturn(capabilities);
        when(fixture.roundEventCapabilityService.hasConfiguredEvents(round)).thenReturn(true);
        when(fixture.roundEventCapabilityService.requiresNetScores(round)).thenReturn(false);
        when(fixture.scorecardRepository.findByRound_Id(18L)).thenReturn(List.of(noShow));
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(18L)).thenReturn(List.of());
        when(fixture.roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(18L)).thenReturn(List.of());

        RoundReadinessResponse response = fixture.service.getReadiness(18L);

        assertEquals(0, response.getScorecardCount());
        assertFalse(response.isScorecardsReady());
        assertTrue(response.getBlockingIssues().contains("No scorecards exist for this round."));
        assertEquals(1, response.getMissingScoreCount());
    }

    @Test
    void getReadiness_shouldKeepMidRoundWithdrawalInActiveReadinessPopulation() {
        Fixture fixture = new Fixture();
        Round round = fixture.configuredStrokePlayRound(19L, 5);
        RoundEventCapabilityService.RoundEventCapabilities capabilities = individualGrossCapabilities();

        Scorecard withdrawn = new Scorecard();
        withdrawn.setParticipationStatus(ScorecardParticipationStatus.WITHDRAWN);
        withdrawn.setWithdrawalHoleNumber(8);
        RoundTee withdrawnTee = mockRoundTee(301L);
        when(fixture.roundTeeResolver.resolve(withdrawn)).thenReturn(withdrawnTee);

        when(fixture.roundRepository.findById(19L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.getCapabilities(round)).thenReturn(capabilities);
        when(fixture.roundEventCapabilityService.hasConfiguredEvents(round)).thenReturn(true);
        when(fixture.roundEventCapabilityService.requiresNetScores(round)).thenReturn(false);
        when(fixture.scorecardRepository.findByRound_Id(19L)).thenReturn(List.of(withdrawn));
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(19L)).thenReturn(List.of());
        when(fixture.roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(19L)).thenReturn(List.of());

        RoundReadinessResponse response = fixture.service.getReadiness(19L);

        assertEquals(1, response.getScorecardCount());
        assertTrue(response.isScorecardsReady());
        assertTrue(response.isTeesReady());
        assertTrue(response.isHandicapsReady());
        assertFalse(response.isGroupsReady());
        assertEquals(1, response.getMissingScoreCount());
        assertFalse(response.isReadyForScoring());
    }

    private static RoundEventCapabilityService.RoundEventCapabilities individualGrossCapabilities() {
        return new RoundEventCapabilityService.RoundEventCapabilities(
                false,
                true,
                false,
                false,
                false,
                true,
                null
        );
    }

    private static RoundTee mockRoundTee(Long id) {
        RoundTee tee = mock(RoundTee.class);
        when(tee.getId()).thenReturn(id);
        return tee;
    }

    private static final class Fixture {
        private final RoundRepository roundRepository = mock(RoundRepository.class);
        private final ScorecardRepository scorecardRepository = mock(ScorecardRepository.class);
        private final RoundGroupRepository roundGroupRepository = mock(RoundGroupRepository.class);
        private final RoundTeamRepository roundTeamRepository = mock(RoundTeamRepository.class);
        private final TeamHoleScoreRepository teamHoleScoreRepository = mock(TeamHoleScoreRepository.class);
        private final RoundTeeResolver roundTeeResolver = mock(RoundTeeResolver.class);
        private final RoundEventCapabilityService roundEventCapabilityService = mock(RoundEventCapabilityService.class);
        private final RoundTeamExceptionService roundTeamExceptionService = mock(RoundTeamExceptionService.class);
        private final RoundReadinessService service = new RoundReadinessService(
                roundRepository,
                scorecardRepository,
                roundGroupRepository,
                roundTeamRepository,
                teamHoleScoreRepository,
                roundTeeResolver,
                roundEventCapabilityService,
                roundTeamExceptionService
        );

        private Round configuredStrokePlayRound(Long roundId, int roundNumber) {
            Round round = mock(Round.class);
            Trip trip = mock(Trip.class);
            Course course = mock(Course.class);
            RoundTee defaultTee = mockRoundTee(201L + roundNumber);

            when(round.getId()).thenReturn(roundId);
            when(round.getTrip()).thenReturn(trip);
            when(trip.getId()).thenReturn(101L);
            when(round.getRoundNumber()).thenReturn(roundNumber);
            when(round.getRoundDate()).thenReturn(LocalDate.of(2026, 10, roundNumber));
            when(round.getCourse()).thenReturn(course);
            when(course.getId()).thenReturn(102L);
            when(round.getDefaultRoundTee()).thenReturn(defaultTee);
            when(round.getHandicapPercent()).thenReturn(100);
            when(round.getFormat()).thenReturn(RoundFormat.STROKE_PLAY);
            when(round.getFinalized()).thenReturn(false);
            return round;
        }
    }
}
