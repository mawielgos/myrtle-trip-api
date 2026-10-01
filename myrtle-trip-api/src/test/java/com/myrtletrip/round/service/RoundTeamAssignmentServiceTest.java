package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.ScorecardParticipationRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundScrambleSeedRoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoundTeamAssignmentServiceTest {

    @Test
    void updateScorecardParticipation_shouldMarkPlayerNoShowAndRemoveTeamAssignment() {
        Fixture fixture = new Fixture();
        Round round = round(21L);
        Player player = player(1001L);
        RoundTeam team = mock(RoundTeam.class);
        Scorecard scorecard = scorecard(501L, round, player, team);
        RoundTeamPlayer assignment = teamPlayer(player);

        when(fixture.roundRepository.findById(21L)).thenReturn(Optional.of(round));
        when(fixture.scorecardRepository.findById(501L)).thenReturn(Optional.of(scorecard));
        when(fixture.roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(21L))
                .thenReturn(List.of(assignment));

        ScorecardParticipationRequest request = request("NO_SHOW", 8);

        RoundTeamAssignmentPageResponse result =
                fixture.service.updateScorecardParticipation(21L, 501L, request);

        assertEquals(ScorecardParticipationStatus.NO_SHOW, scorecard.getParticipationStatus());
        assertNull(scorecard.getWithdrawalHoleNumber());
        assertNull(scorecard.getTeam());
        assertSame(fixture.pageResponse, result);
        verify(fixture.roundTeamPlayerRepository).delete(assignment);
        verify(fixture.scorecardRepository).save(scorecard);
    }

    @Test
    void updateScorecardParticipation_shouldPreserveWithdrawalHoleAndRemoveTeamAssignment() {
        Fixture fixture = new Fixture();
        Round round = round(22L);
        Player player = player(1002L);
        RoundTeam team = mock(RoundTeam.class);
        Scorecard scorecard = scorecard(502L, round, player, team);
        RoundTeamPlayer assignment = teamPlayer(player);

        when(fixture.roundRepository.findById(22L)).thenReturn(Optional.of(round));
        when(fixture.scorecardRepository.findById(502L)).thenReturn(Optional.of(scorecard));
        when(fixture.roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(22L))
                .thenReturn(List.of(assignment));

        fixture.service.updateScorecardParticipation(22L, 502L, request("WITHDRAWN", 9));

        assertEquals(ScorecardParticipationStatus.WITHDRAWN, scorecard.getParticipationStatus());
        assertEquals(9, scorecard.getWithdrawalHoleNumber());
        assertNull(scorecard.getTeam());
        verify(fixture.roundTeamPlayerRepository).delete(assignment);
        verify(fixture.scorecardRepository).save(scorecard);
    }

    @Test
    void updateScorecardParticipation_shouldReactivatePlayerWithoutRemovingTeamAssignment() {
        Fixture fixture = new Fixture();
        Round round = round(23L);
        Player player = player(1003L);
        RoundTeam team = mock(RoundTeam.class);
        Scorecard scorecard = scorecard(503L, round, player, team);
        scorecard.setParticipationStatus(ScorecardParticipationStatus.WITHDRAWN);
        scorecard.setWithdrawalHoleNumber(6);
        scorecard.setTeam(team);

        when(fixture.roundRepository.findById(23L)).thenReturn(Optional.of(round));
        when(fixture.scorecardRepository.findById(503L)).thenReturn(Optional.of(scorecard));

        fixture.service.updateScorecardParticipation(23L, 503L, request("ACTIVE", null));

        assertEquals(ScorecardParticipationStatus.ACTIVE, scorecard.getParticipationStatus());
        assertNull(scorecard.getWithdrawalHoleNumber());
        assertSame(team, scorecard.getTeam());
        verify(fixture.roundTeamPlayerRepository, never())
                .findForRoundOrderedByTeamNumberAndPlayerOrder(23L);
        verify(fixture.scorecardRepository).save(scorecard);
    }

    @Test
    void updateScorecardParticipation_shouldRejectWithdrawalHoleOutsideRoundRange() {
        Fixture fixture = new Fixture();
        Round round = round(24L);
        Player player = player(1004L);
        Scorecard scorecard = scorecard(504L, round, player, null);

        when(fixture.roundRepository.findById(24L)).thenReturn(Optional.of(round));
        when(fixture.scorecardRepository.findById(504L)).thenReturn(Optional.of(scorecard));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.updateScorecardParticipation(
                        24L,
                        504L,
                        request("WITHDRAWN", 19)
                )
        );

        assertEquals("Withdrawal hole must be between 0 and 18.", exception.getMessage());
        verify(fixture.scorecardRepository, never()).save(scorecard);
    }

    @Test
    void updateScorecardParticipation_shouldRejectScorecardFromAnotherRound() {
        Fixture fixture = new Fixture();
        Round requestedRound = round(25L);
        Round otherRound = round(26L);
        Player player = player(1005L);
        Scorecard scorecard = scorecard(505L, otherRound, player, null);

        when(fixture.roundRepository.findById(25L)).thenReturn(Optional.of(requestedRound));
        when(fixture.scorecardRepository.findById(505L)).thenReturn(Optional.of(scorecard));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.updateScorecardParticipation(
                        25L,
                        505L,
                        request("NO_SHOW", null)
                )
        );

        assertEquals("Scorecard does not belong to this round.", exception.getMessage());
        verify(fixture.scorecardRepository, never()).save(scorecard);
    }

    private static ScorecardParticipationRequest request(String status, Integer withdrawalHoleNumber) {
        ScorecardParticipationRequest request = new ScorecardParticipationRequest();
        request.setParticipationStatus(status);
        request.setWithdrawalHoleNumber(withdrawalHoleNumber);
        return request;
    }

    private static Round round(Long id) {
        Round round = mock(Round.class);
        when(round.getId()).thenReturn(id);
        return round;
    }

    private static Player player(Long id) {
        Player player = new Player();
        player.setId(id);
        return player;
    }

    private static Scorecard scorecard(
            Long id,
            Round round,
            Player player,
            RoundTeam team
    ) {
        Scorecard scorecard = new Scorecard();
        scorecard.setId(id);
        scorecard.setRound(round);
        scorecard.setPlayer(player);
        scorecard.setTeam(team);
        return scorecard;
    }

    private static RoundTeamPlayer teamPlayer(Player player) {
        RoundTeamPlayer row = new RoundTeamPlayer();
        row.setPlayer(player);
        return row;
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
            doReturn(pageResponse).when(service).getAssignmentPage(org.mockito.ArgumentMatchers.anyLong());
        }
    }
}
