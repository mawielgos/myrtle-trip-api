package com.myrtletrip.round.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.round.dto.RoundReadinessResponse;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
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

    @Test
    void getReadiness_shouldBeReadyForFinalizationWhenIndividualGrossRoundIsComplete() {
        Fixture fixture = new Fixture();
        Round round = fixture.configuredStrokePlayRound(20L, 6);
        RoundEventCapabilityService.RoundEventCapabilities capabilities = individualGrossCapabilities();
        Scorecard scorecard = activeCompleteScorecard(501L, 82, 80, 80);
        RoundGroup group = groupWithPlayers(501L);

        when(fixture.roundRepository.findById(20L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.getCapabilities(round)).thenReturn(capabilities);
        when(fixture.roundEventCapabilityService.hasConfiguredEvents(round)).thenReturn(true);
        when(fixture.roundEventCapabilityService.requiresNetScores(round)).thenReturn(false);
        when(fixture.scorecardRepository.findByRound_Id(20L)).thenReturn(List.of(scorecard));
        RoundTee resolvedTee = mockRoundTee(401L);
        when(fixture.roundTeeResolver.resolve(scorecard)).thenReturn(resolvedTee);
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(20L)).thenReturn(List.of());
        when(fixture.roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(20L)).thenReturn(List.of(group));

        RoundReadinessResponse response = fixture.service.getReadiness(20L);

        assertTrue(response.isRoundConfigured());
        assertTrue(response.isScorecardsReady());
        assertTrue(response.isTeesReady());
        assertTrue(response.isHandicapsReady());
        assertTrue(response.isGroupsReady());
        assertTrue(response.isTeamsReady());
        assertTrue(response.isReadyForScoring());
        assertTrue(response.isScoreEntryComplete());
        assertTrue(response.isReadyForFinalization());
        assertEquals(0, response.getMissingScoreCount());
        assertTrue(response.getBlockingIssues().isEmpty());
        assertTrue(response.getWarnings().isEmpty());
    }

    @Test
    void getReadiness_shouldBlockScoringWhenActiveScorecardHasNoEffectiveTee() {
        Fixture fixture = new Fixture();
        Round round = fixture.configuredStrokePlayRound(21L, 7);
        RoundEventCapabilityService.RoundEventCapabilities capabilities = individualGrossCapabilities();
        Scorecard scorecard = activeCompleteScorecard(502L, 85, 83, 83);
        RoundGroup group = groupWithPlayers(502L);

        when(fixture.roundRepository.findById(21L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.getCapabilities(round)).thenReturn(capabilities);
        when(fixture.roundEventCapabilityService.hasConfiguredEvents(round)).thenReturn(true);
        when(fixture.roundEventCapabilityService.requiresNetScores(round)).thenReturn(false);
        when(fixture.scorecardRepository.findByRound_Id(21L)).thenReturn(List.of(scorecard));
        when(fixture.roundTeeResolver.resolve(scorecard)).thenReturn(null);
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(21L)).thenReturn(List.of());
        when(fixture.roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(21L)).thenReturn(List.of(group));

        RoundReadinessResponse response = fixture.service.getReadiness(21L);

        assertFalse(response.isTeesReady());
        assertFalse(response.isReadyForScoring());
        assertFalse(response.isReadyForFinalization());
        assertTrue(response.getBlockingIssues().contains("One or more players does not have a tee selected."));
    }

    @Test
    void getReadiness_shouldBlockNetRoundWhenCalculatedHandicapValuesAreMissing() {
        Fixture fixture = new Fixture();
        Round round = fixture.configuredStrokePlayRound(22L, 8);
        RoundEventCapabilityService.RoundEventCapabilities capabilities = new RoundEventCapabilityService.RoundEventCapabilities(
                false, true, false, false, true, false, null
        );
        Scorecard scorecard = activeCompleteScorecard(503L, 84, 82, 75);
        when(scorecard.getCourseHandicap()).thenReturn(null);
        when(scorecard.getPlayingHandicap()).thenReturn(7);
        RoundGroup group = groupWithPlayers(503L);

        when(fixture.roundRepository.findById(22L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.getCapabilities(round)).thenReturn(capabilities);
        when(fixture.roundEventCapabilityService.hasConfiguredEvents(round)).thenReturn(true);
        when(fixture.roundEventCapabilityService.requiresNetScores(round)).thenReturn(true);
        when(fixture.scorecardRepository.findByRound_Id(22L)).thenReturn(List.of(scorecard));
        RoundTee resolvedTee = mockRoundTee(402L);
        when(fixture.roundTeeResolver.resolve(scorecard)).thenReturn(resolvedTee);
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(22L)).thenReturn(List.of());
        when(fixture.roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(22L)).thenReturn(List.of(group));

        RoundReadinessResponse response = fixture.service.getReadiness(22L);

        assertFalse(response.isHandicapsReady());
        assertFalse(response.isReadyForScoring());
        assertTrue(response.getBlockingIssues().contains("One or more scorecards is missing calculated handicap values."));
    }

    @Test
    void getReadiness_shouldCountMissingScrambleTeamScoreWithoutRequiringPlayerTotals() {
        Fixture fixture = new Fixture();
        Round round = fixture.configuredStrokePlayRound(23L, 9);
        RoundEventCapabilityService.RoundEventCapabilities capabilities = new RoundEventCapabilityService.RoundEventCapabilities(
                true, false, true, false, false, true, 4
        );
        RoundTeam team = mock(RoundTeam.class);
        when(team.getId()).thenReturn(601L);
        when(team.getTeamNumber()).thenReturn(1);
        when(team.getTeamName()).thenReturn("Team 1");
        when(team.getScrambleTotalScore()).thenReturn(null);

        List<Scorecard> scorecards = List.of(
                activeTeamScorecard(511L, team),
                activeTeamScorecard(512L, team),
                activeTeamScorecard(513L, team),
                activeTeamScorecard(514L, team)
        );
        RoundGroup group = groupWithPlayers(511L, 512L, 513L, 514L);

        when(fixture.roundRepository.findById(23L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.getCapabilities(round)).thenReturn(capabilities);
        when(fixture.roundEventCapabilityService.hasConfiguredEvents(round)).thenReturn(true);
        when(fixture.roundEventCapabilityService.requiresNetScores(round)).thenReturn(false);
        when(fixture.roundEventCapabilityService.expectedTeamSize(round)).thenReturn(4);
        when(fixture.scorecardRepository.findByRound_Id(23L)).thenReturn(scorecards);
        for (int i = 0; i < scorecards.size(); i++) {
            Scorecard scorecard = scorecards.get(i);
            RoundTee resolvedTee = mockRoundTee(410L + i);
            when(fixture.roundTeeResolver.resolve(scorecard)).thenReturn(resolvedTee);
        }
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(23L)).thenReturn(List.of(team));
        when(fixture.roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(23L)).thenReturn(List.of(group));
        when(fixture.roundTeamExceptionService.hasDuplicateGhostAssignments(23L)).thenReturn(false);
        when(fixture.roundTeamExceptionService.countTeamPlayers(601L)).thenReturn(4);
        when(fixture.teamHoleScoreRepository.findByRoundTeam_IdOrderByHoleNumberAsc(601L)).thenReturn(List.of());

        RoundReadinessResponse response = fixture.service.getReadiness(23L);

        assertTrue(response.isReadyForScoring());
        assertFalse(response.isScoreEntryComplete());
        assertFalse(response.isReadyForFinalization());
        assertEquals(1, response.getMissingScoreCount());
        assertTrue(response.getWarnings().contains("Round has 1 missing score entry. Enter scores before finalizing the round."));
    }

    @Test
    void getReadiness_shouldWarnAndPreventRefinalizationWhenRoundIsAlreadyFinalized() {
        Fixture fixture = new Fixture();
        Round round = fixture.configuredStrokePlayRound(24L, 10);
        when(round.getFinalized()).thenReturn(true);
        RoundEventCapabilityService.RoundEventCapabilities capabilities = individualGrossCapabilities();
        Scorecard scorecard = activeCompleteScorecard(504L, 79, 78, 78);
        RoundGroup group = groupWithPlayers(504L);

        when(fixture.roundRepository.findById(24L)).thenReturn(Optional.of(round));
        when(fixture.roundEventCapabilityService.getCapabilities(round)).thenReturn(capabilities);
        when(fixture.roundEventCapabilityService.hasConfiguredEvents(round)).thenReturn(true);
        when(fixture.roundEventCapabilityService.requiresNetScores(round)).thenReturn(false);
        when(fixture.scorecardRepository.findByRound_Id(24L)).thenReturn(List.of(scorecard));
        RoundTee resolvedTee = mockRoundTee(403L);
        when(fixture.roundTeeResolver.resolve(scorecard)).thenReturn(resolvedTee);
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(24L)).thenReturn(List.of());
        when(fixture.roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(24L)).thenReturn(List.of(group));

        RoundReadinessResponse response = fixture.service.getReadiness(24L);

        assertTrue(response.isReadyForScoring());
        assertTrue(response.isScoreEntryComplete());
        assertFalse(response.isReadyForFinalization());
        assertTrue(response.isFinalized());
        assertTrue(response.getWarnings().contains(
                "This round is already finalized. Corrections should go through the correction/recalculation flow."
        ));
    }

    private static Scorecard activeCompleteScorecard(Long playerId, int gross, int adjustedGross, int net) {
        Scorecard scorecard = mock(Scorecard.class);
        Player player = mock(Player.class);
        when(player.getId()).thenReturn(playerId);
        when(scorecard.getParticipationStatus()).thenReturn(ScorecardParticipationStatus.ACTIVE);
        when(scorecard.getPlayer()).thenReturn(player);
        when(scorecard.getGrossScore()).thenReturn(gross);
        when(scorecard.getAdjustedGrossScore()).thenReturn(adjustedGross);
        when(scorecard.getNetScore()).thenReturn(net);
        return scorecard;
    }

    private static Scorecard activeTeamScorecard(Long playerId, RoundTeam team) {
        Scorecard scorecard = activeCompleteScorecard(playerId, 80, 80, 80);
        when(scorecard.getTeam()).thenReturn(team);
        return scorecard;
    }

    private static RoundGroup groupWithPlayers(Long... playerIds) {
        RoundGroup group = new RoundGroup();
        int seat = 1;
        for (Long playerId : playerIds) {
            Player player = mock(Player.class);
            when(player.getId()).thenReturn(playerId);
            RoundGroupPlayer groupPlayer = new RoundGroupPlayer();
            groupPlayer.setPlayer(player);
            groupPlayer.setSeatOrder(seat++);
            group.addPlayer(groupPlayer);
        }
        return group;
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
        private final RoundScoreReadinessService roundScoreReadinessService = new RoundScoreReadinessService(
                teamHoleScoreRepository,
                roundTeeResolver,
                roundEventCapabilityService
        );
        private final RoundGroupingReadinessService roundGroupingReadinessService = new RoundGroupingReadinessService(
                roundEventCapabilityService,
                roundScoreReadinessService
        );
        private final RoundTeamReadinessService roundTeamReadinessService = new RoundTeamReadinessService(
                roundEventCapabilityService,
                roundTeamExceptionService,
                roundScoreReadinessService
        );
        private final RoundReadinessService service = new RoundReadinessService(
                roundRepository,
                scorecardRepository,
                roundGroupRepository,
                roundTeamRepository,
                roundEventCapabilityService,
                roundScoreReadinessService,
                roundGroupingReadinessService,
                roundTeamReadinessService
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
