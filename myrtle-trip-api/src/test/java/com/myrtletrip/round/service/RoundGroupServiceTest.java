package com.myrtletrip.round.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.round.dto.RoundGroupAssignmentItemRequest;
import com.myrtletrip.round.dto.RoundGroupAssignmentRequest;
import com.myrtletrip.round.dto.RoundGroupPageResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundGroupServiceTest {

    @Mock private RoundRepository roundRepository;
    @Mock private RoundGroupRepository roundGroupRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private ScorecardRepository scorecardRepository;
    @Mock private ScorecardHandicapService scorecardHandicapService;
    @Mock private RoundTeeRepository roundTeeRepository;
    @Mock private RoundTeeProvisioningService roundTeeProvisioningService;
    @Mock private RoundTeamAutoAssignmentService roundTeamAutoAssignmentService;
    @Mock private RoundGroupAutoAssignmentService roundGroupAutoAssignmentService;
    @Mock private TripEditingGuardService tripEditingGuardService;
    @Mock private RoundEventCapabilityService roundEventCapabilityService;

    private RoundGroupService service;
    private RoundGroupReadModelService readModelService;
    private RoundGroupCommandService commandService;

    @BeforeEach
    void setUp() {
        readModelService = new RoundGroupReadModelService(
                roundRepository,
                roundGroupRepository,
                scorecardRepository,
                roundTeeProvisioningService,
                roundGroupAutoAssignmentService,
                roundEventCapabilityService
        );
        commandService = new RoundGroupCommandService(
                roundRepository,
                roundGroupRepository,
                playerRepository,
                scorecardRepository,
                scorecardHandicapService,
                roundTeeRepository,
                roundTeeProvisioningService,
                roundTeamAutoAssignmentService,
                tripEditingGuardService,
                roundEventCapabilityService
        );
        service = new RoundGroupService(readModelService, commandService);
    }

    @Test
    void getRoundGroups_shouldMapExistingFinalizedGroupsAndPlayers() {
        Round round = mockRound(10L, true);
        when(roundRepository.findById(10L)).thenReturn(Optional.of(round));
        when(roundEventCapabilityService.requiresTeams(round)).thenReturn(false);

        Player player = new Player();
        player.setId(7L);
        player.setDisplayName("Player Seven");
        RoundGroup group = mock(RoundGroup.class);
        when(group.getId()).thenReturn(101L);
        when(group.getGroupNumber()).thenReturn(1);
        when(group.getPlayers()).thenReturn(List.of(groupPlayer(player, 2)));
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(10L)).thenReturn(List.of(group));

        RoundGroupPageResponse result = service.getRoundGroups(10L);

        assertEquals(10L, result.getRoundId());
        assertEquals(1, result.getGroups().size());
        assertEquals(101L, result.getGroups().get(0).getGroupId());
        assertEquals("Player Seven", result.getGroups().get(0).getPlayers().get(0).getPlayerName());
        assertEquals(2, result.getGroups().get(0).getPlayers().get(0).getSeatOrder());
        verify(roundTeeProvisioningService).ensureRoundTeeOptions(round);
        verify(roundGroupAutoAssignmentService, never()).syncGroupsFromTeamsIfNeeded(anyLong());
    }

    @Test
    void getRoundGroups_shouldProvisionEnoughEmptyGroupsForActivePlayers() {
        Round round = mockRound(20L, false);
        when(roundRepository.findById(20L)).thenReturn(Optional.of(round));
        when(roundEventCapabilityService.requiresTeams(round)).thenReturn(false);
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(20L))
                .thenReturn(List.of())
                .thenReturn(List.of(group(1), group(2)));
        when(scorecardRepository.findByRound_Id(20L)).thenReturn(List.of(
                activeScorecard(), activeScorecard(), activeScorecard(), activeScorecard(), activeScorecard()
        ));

        RoundGroupPageResponse result = service.getRoundGroups(20L);

        assertEquals(2, result.getGroups().size());
        verify(roundGroupRepository).saveAll(argThat(groups ->
                StreamSupport.stream(groups.spliterator(), false).count() == 2));
        verify(roundGroupRepository).flush();
    }

    @Test
    void getRoundGroups_shouldNotAutoAssignOrProvisionWhenFinalized() {
        Round round = mockRound(30L, true);
        when(roundRepository.findById(30L)).thenReturn(Optional.of(round));
        when(roundEventCapabilityService.requiresTeams(round)).thenReturn(true);
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(30L)).thenReturn(List.of());

        RoundGroupPageResponse result = service.getRoundGroups(30L);

        assertTrue(result.getGroups().isEmpty());
        verify(roundGroupAutoAssignmentService, never()).syncGroupsFromTeamsIfNeeded(anyLong());
        verify(roundGroupRepository, never()).saveAll(anyList());
    }

    @Test
    void saveRoundGroups_shouldHonorTripEditingGuardBeforeChangingAnything() {
        Round round = mockRound(40L, false);
        Trip trip = round.getTrip();
        when(roundRepository.findById(40L)).thenReturn(Optional.of(round));
        doThrow(new IllegalStateException("locked")).when(tripEditingGuardService).assertStructureEditable(trip);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.saveRoundGroups(40L, new RoundGroupAssignmentRequest())
        );

        assertEquals("locked", error.getMessage());
        verify(roundTeeProvisioningService, never()).ensureRoundTeeOptions(any());
        verify(roundGroupRepository, never()).deleteAll(anyList());
    }

    @Test
    void saveRoundGroups_shouldPersistAssignmentsRefreshHandicapAndRebuildTeams() {
        Round round = mockRound(50L, false);
        RoundTee defaultTee = mock(RoundTee.class);
        when(round.getDefaultRoundTee()).thenReturn(defaultTee);
        when(roundRepository.findById(50L)).thenReturn(Optional.of(round));
        when(roundEventCapabilityService.isTwoManLowNetRound(round)).thenReturn(false);
        when(roundEventCapabilityService.requiresTeams(round)).thenReturn(false);
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(50L))
                .thenReturn(List.of())
                .thenReturn(List.of(group(1)));

        Player player = new Player();
        player.setId(9L);
        player.setDisplayName("Player Nine");
        when(playerRepository.findAllById(List.of(9L))).thenReturn(List.of(player));

        Scorecard scorecard = new Scorecard();
        scorecard.setId(900L);
        scorecard.setRound(round);
        scorecard.setPlayer(player);
        scorecard.setParticipationStatus(ScorecardParticipationStatus.ACTIVE);
        when(scorecardRepository.findByRound_Id(50L)).thenReturn(List.of(scorecard));
        when(scorecardRepository.findByRound_IdAndPlayer_Id(50L, 9L)).thenReturn(Optional.of(scorecard));

        RoundGroupAssignmentItemRequest item = new RoundGroupAssignmentItemRequest();
        item.setPlayerId(9L);
        item.setGroupNumber(1);
        item.setSeatOrder(1);
        RoundGroupAssignmentRequest request = new RoundGroupAssignmentRequest();
        request.setAssignments(List.of(item));

        RoundGroupPageResponse result = service.saveRoundGroups(50L, request);

        assertEquals(50L, result.getRoundId());
        verify(roundGroupRepository).saveAll(argThat(groups -> {
            List<RoundGroup> savedGroups = StreamSupport.stream(groups.spliterator(), false).toList();
            return savedGroups.size() == 1
                    && savedGroups.get(0).getPlayers().size() == 1
                    && savedGroups.get(0).getPlayers().get(0).getPlayer().getId().equals(9L);
        }));
        verify(scorecardRepository).save(scorecard);
        verify(scorecardHandicapService).refreshHandicaps(900L);
        verify(roundTeamAutoAssignmentService).rebuildTeamsFromGroups(50L);
        assertSame(defaultTee, scorecard.getRoundTee());
    }

    private Round mockRound(Long id, boolean finalized) {
        Round round = mock(Round.class);
        Trip trip = mock(Trip.class);
        lenient().when(round.getId()).thenReturn(id);
        lenient().when(round.getFinalized()).thenReturn(finalized);
        lenient().when(round.getTrip()).thenReturn(trip);
        return round;
    }

    private RoundGroupPlayer groupPlayer(Player player, int seatOrder) {
        RoundGroupPlayer result = new RoundGroupPlayer();
        result.setPlayer(player);
        result.setSeatOrder(seatOrder);
        return result;
    }

    private RoundGroup group(int number) {
        RoundGroup group = new RoundGroup();
        group.setGroupNumber(number);
        group.setPlayers(List.of());
        return group;
    }

    private Scorecard activeScorecard() {
        Scorecard scorecard = new Scorecard();
        scorecard.setParticipationStatus(ScorecardParticipationStatus.ACTIVE);
        return scorecard;
    }
}
