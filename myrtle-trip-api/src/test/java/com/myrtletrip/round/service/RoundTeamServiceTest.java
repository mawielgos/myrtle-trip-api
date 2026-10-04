package com.myrtletrip.round.service;

import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.round.dto.RoundTeamPlayerRequest;
import com.myrtletrip.round.dto.RoundTeamPlayerResponse;
import com.myrtletrip.round.dto.RoundTeamRequest;
import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.dto.SaveRoundTeamsRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.exceptionmodel.entity.RoundTeamException;
import com.myrtletrip.round.exceptionmodel.entity.RoundTeamExceptionType;
import com.myrtletrip.round.exceptionmodel.repository.RoundTeamExceptionRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RoundTeamServiceTest {

    private RoundRepository roundRepository;
    private RoundTeamRepository roundTeamRepository;
    private RoundTeamPlayerRepository roundTeamPlayerRepository;
    private RoundTeeRepository roundTeeRepository;
    private PlayerRepository playerRepository;
    private ScorecardRepository scorecardRepository;
    private ScorecardHandicapService scorecardHandicapService;
    private RoundGroupAutoAssignmentService roundGroupAutoAssignmentService;
    private RoundTeeResolver roundTeeResolver;
    private RoundTeamReadModelService roundTeamReadModelService;
    private RoundTeamCommandService roundTeamCommandService;
    private RoundCapabilityService roundCapabilityService;
    private RoundEventCapabilityService roundEventCapabilityService;
    private RoundTeamExceptionRepository roundTeamExceptionRepository;
    private RoundTeamService service;

    @BeforeEach
    void setUp() {
        roundRepository = mock(RoundRepository.class);
        roundTeamRepository = mock(RoundTeamRepository.class);
        roundTeamPlayerRepository = mock(RoundTeamPlayerRepository.class);
        roundTeeRepository = mock(RoundTeeRepository.class);
        playerRepository = mock(PlayerRepository.class);
        scorecardRepository = mock(ScorecardRepository.class);
        scorecardHandicapService = mock(ScorecardHandicapService.class);
        roundGroupAutoAssignmentService = mock(RoundGroupAutoAssignmentService.class);
        roundTeeResolver = mock(RoundTeeResolver.class);
        roundCapabilityService = mock(RoundCapabilityService.class);
        roundEventCapabilityService = mock(RoundEventCapabilityService.class);
        roundTeamExceptionRepository = mock(RoundTeamExceptionRepository.class);
        roundTeamReadModelService = new RoundTeamReadModelService(
                roundRepository,
                roundTeamRepository,
                roundTeamPlayerRepository,
                scorecardRepository,
                roundTeeResolver,
                roundEventCapabilityService
        );

        roundTeamCommandService = new RoundTeamCommandService(
                roundRepository,
                roundTeamRepository,
                roundTeamPlayerRepository,
                roundTeeRepository,
                playerRepository,
                scorecardRepository,
                scorecardHandicapService,
                roundGroupAutoAssignmentService,
                roundCapabilityService,
                roundEventCapabilityService,
                roundTeamExceptionRepository
        );

        service = new RoundTeamService(
                roundTeamCommandService,
                roundTeamReadModelService
        );
    }

    @Test
    void getTeamsReturnsEmptyWhenRoundDoesNotUseTeams() {
        Round round = mock(Round.class);
        when(roundRepository.findById(1L)).thenReturn(Optional.of(round));
        when(roundEventCapabilityService.requiresTeams(round)).thenReturn(false);

        assertEquals(List.of(), service.getTeams(1L));
        verify(roundTeamRepository, never()).findByRound_IdOrderByTeamNumberAsc(any());
    }

    @Test
    void getTeamsMapsPlayersAndResolvedTee() {
        Round round = mock(Round.class);
        RoundTeam team = mock(RoundTeam.class);
        RoundTeamPlayer teamPlayer = mock(RoundTeamPlayer.class);
        RoundTee defaultTee = mock(RoundTee.class);
        RoundTee resolvedTee = mock(RoundTee.class);
        Player player = player(7L, "Jane Golfer", "f");
        Scorecard scorecard = scorecard(22L, round, player, resolvedTee);

        when(roundRepository.findById(1L)).thenReturn(Optional.of(round));
        when(roundEventCapabilityService.requiresTeams(round)).thenReturn(true);
        when(roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(1L)).thenReturn(new ArrayList<>(List.of(team)));
        when(team.getId()).thenReturn(11L);
        when(team.getTeamNumber()).thenReturn(1);
        when(team.getTeamName()).thenReturn("Alpha");
        when(roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(11L)).thenReturn(new ArrayList<>(List.of(teamPlayer)));
        when(teamPlayer.getPlayer()).thenReturn(player);
        when(teamPlayer.getPlayerOrder()).thenReturn(1);
        when(scorecardRepository.findByRound_IdAndPlayer_Id(1L, 7L)).thenReturn(Optional.of(scorecard));
        when(roundTeeResolver.resolve(scorecard)).thenReturn(resolvedTee);
        when(round.getDefaultRoundTee()).thenReturn(defaultTee);
        when(defaultTee.getId()).thenReturn(100L);
        when(resolvedTee.getId()).thenReturn(101L);
        when(resolvedTee.getTeeName()).thenReturn("Forward");

        List<RoundTeamResponse> result = service.getTeams(1L);

        assertEquals(1, result.size());
        assertEquals("Alpha", result.get(0).getTeamName());
        RoundTeamPlayerResponse mapped = result.get(0).getPlayers().get(0);
        assertEquals(7L, mapped.getPlayerId());
        assertEquals("Jane Golfer", mapped.getPlayerName());
        assertEquals("F", mapped.getGender());
        assertEquals(22L, mapped.getScorecardId());
        assertEquals(101L, mapped.getRoundTeeId());
        assertEquals("Forward", mapped.getRoundTeeName());
        assertTrue(mapped.getTeeOverride());
    }

    @Test
    void saveTeamsRejectsEmptyRequest() {
        Round round = mock(Round.class);
        when(roundRepository.findById(1L)).thenReturn(Optional.of(round));
        when(roundEventCapabilityService.requiresTeams(round)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.saveTeams(1L, new SaveRoundTeamsRequest()));

        assertEquals("At least one team is required", ex.getMessage());
        verify(roundCapabilityService).assertCanAssignTeams(round);
        verify(roundTeamRepository, never()).deleteByRound_Id(any());
    }

    @Test
    void saveTeamsRebuildsAssignmentsPreservesExistingScorecardTeeAndSyncsGroups() {
        Round round = mock(Round.class);
        when(round.getId()).thenReturn(1L);
        Player player = player(7L, "Jane Golfer", "F");
        RoundTee tee = mock(RoundTee.class);
        when(tee.getId()).thenReturn(55L);
        when(tee.getRound()).thenReturn(round);
        Scorecard scorecard = scorecard(22L, round, player, tee);

        when(roundRepository.findById(1L)).thenReturn(Optional.of(round));
        when(roundEventCapabilityService.requiresTeams(round)).thenReturn(true);
        when(roundEventCapabilityService.expectedTeamSize(round)).thenReturn(2);
        when(roundTeamExceptionRepository.findByRound_IdAndActiveTrueOrderByRoundTeam_TeamNumberAscIdAsc(1L)).thenReturn(List.of());
        when(scorecardRepository.findByRound_Id(1L)).thenReturn(new ArrayList<>(List.of(scorecard)));
        when(roundTeamRepository.save(any(RoundTeam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playerRepository.findById(7L)).thenReturn(Optional.of(player));
        when(scorecardRepository.findById(22L)).thenReturn(Optional.of(scorecard));
        when(roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(1L)).thenReturn(List.of());

        List<RoundTeamResponse> result = service.saveTeams(1L, request(1, "Alpha", 7L, 22L, 1, null));

        assertTrue(result.isEmpty());
        assertNotNull(scorecard.getTeam());
        assertEquals(1, scorecard.getTeam().getTeamNumber());
        verify(roundTeamExceptionRepository).deleteByRoundIdHard(1L);
        verify(roundTeamPlayerRepository).deleteByRoundTeam_Round_Id(1L);
        verify(roundTeamRepository).deleteByRound_Id(1L);
        verify(scorecardHandicapService).setScorecardTee(22L, 55L);
        verify(roundGroupAutoAssignmentService).syncGroupsFromTeamsIfNeeded(1L);
    }

    @Test
    void saveTeamsRestoresMatchingActiveTeamExceptionAfterRebuild() {
        Round round = mock(Round.class);
        when(round.getId()).thenReturn(1L);
        Player player = player(7L, "Jane Golfer", "F");
        Scorecard scorecard = scorecard(22L, round, player, null);
        RoundTeam oldTeam = mock(RoundTeam.class);
        when(oldTeam.getId()).thenReturn(90L);
        when(oldTeam.getTeamNumber()).thenReturn(1);
        RoundTeamPlayer oldTeamPlayer = mock(RoundTeamPlayer.class);
        when(oldTeamPlayer.getPlayer()).thenReturn(player);
        RoundTeamException existing = new RoundTeamException();
        existing.setRound(round);
        existing.setRoundTeam(oldTeam);
        existing.setExceptionType(RoundTeamExceptionType.EXTRA_SHOT_ROTATION);
        existing.setNotes("keep me");

        when(roundRepository.findById(1L)).thenReturn(Optional.of(round));
        when(roundEventCapabilityService.requiresTeams(round)).thenReturn(true);
        when(roundEventCapabilityService.expectedTeamSize(round)).thenReturn(2);
        when(roundTeamExceptionRepository.findByRound_IdAndActiveTrueOrderByRoundTeam_TeamNumberAscIdAsc(1L)).thenReturn(List.of(existing));
        when(roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(90L)).thenReturn(List.of(oldTeamPlayer));
        when(scorecardRepository.findByRound_Id(1L)).thenReturn(new ArrayList<>(List.of(scorecard)));
        when(roundTeamRepository.save(any(RoundTeam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(playerRepository.findById(7L)).thenReturn(Optional.of(player));
        when(scorecardRepository.findById(22L)).thenReturn(Optional.of(scorecard));
        when(roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(1L)).thenReturn(List.of());

        service.saveTeams(1L, request(1, "Alpha", 7L, 22L, 1, null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<RoundTeamException>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(roundTeamExceptionRepository).saveAll(captor.capture());
        List<RoundTeamException> restored = new ArrayList<>();
        captor.getValue().forEach(restored::add);
        assertEquals(1, restored.size());
        assertEquals(RoundTeamExceptionType.EXTRA_SHOT_ROTATION, restored.get(0).getExceptionType());
        assertEquals("keep me", restored.get(0).getNotes());
        assertEquals(1, restored.get(0).getRoundTeam().getTeamNumber());
        assertTrue(restored.get(0).getActive());
    }

    private Player player(Long id, String displayName, String gender) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName(displayName);
        player.setGender(gender);
        return player;
    }

    private Scorecard scorecard(Long id, Round round, Player player, RoundTee tee) {
        Scorecard scorecard = new Scorecard();
        scorecard.setId(id);
        scorecard.setRound(round);
        scorecard.setPlayer(player);
        scorecard.setRoundTee(tee);
        scorecard.setParticipationStatus(ScorecardParticipationStatus.ACTIVE);
        return scorecard;
    }

    private SaveRoundTeamsRequest request(
            Integer teamNumber,
            String teamName,
            Long playerId,
            Long scorecardId,
            Integer playerOrder,
            Long roundTeeId
    ) {
        RoundTeamPlayerRequest player = new RoundTeamPlayerRequest();
        player.setPlayerId(playerId);
        player.setScorecardId(scorecardId);
        player.setPlayerOrder(playerOrder);
        player.setRoundTeeId(roundTeeId);

        RoundTeamRequest team = new RoundTeamRequest();
        team.setTeamNumber(teamNumber);
        team.setTeamName(teamName);
        team.setPlayers(new ArrayList<>(List.of(player)));

        SaveRoundTeamsRequest request = new SaveRoundTeamsRequest();
        request.setTeams(new ArrayList<>(List.of(team)));
        return request;
    }
}
