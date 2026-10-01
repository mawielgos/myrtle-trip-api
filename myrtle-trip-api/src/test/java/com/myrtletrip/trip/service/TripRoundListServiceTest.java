package com.myrtletrip.trip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.repository.RoundEventRepository;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.service.RoundEventCapabilityService;
import com.myrtletrip.round.service.RoundTeamAutoAssignmentService;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.dto.TripRoundListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class TripRoundListServiceTest {

    @Mock private RoundRepository roundRepository;
    @Mock private ScorecardRepository scorecardRepository;
    @Mock private RoundGroupRepository roundGroupRepository;
    @Mock private RoundTeamRepository roundTeamRepository;
    @Mock private RoundTeamAutoAssignmentService roundTeamAutoAssignmentService;
    @Mock private RoundEventRepository roundEventRepository;
    @Mock private RoundEventCapabilityService roundEventCapabilityService;

    @InjectMocks
    private TripRoundListService service;

    @Test
    void getTripRounds_returnsEmptyWhenTripHasNoRounds() {
        when(roundRepository.findByTrip_IdOrderByRoundDateAsc(10L)).thenReturn(Collections.emptyList());

        List<TripRoundListResponse> result = service.getTripRounds(10L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTripRounds_marksUnfinalizedRoundAsNeedingGroupingWhenNoScorecardsExist() {
        Round round = round(101L, false);
        when(roundRepository.findByTrip_IdOrderByRoundDateAsc(10L)).thenReturn(List.of(round));
        when(scorecardRepository.findByRound_Id(101L)).thenReturn(Collections.emptyList());
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(101L)).thenReturn(Collections.emptyList());
        when(roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(101L)).thenReturn(Collections.emptyList());
        when(roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(101L)).thenReturn(Collections.emptyList());

        TripRoundListResponse response = service.getTripRounds(10L).get(0);

        assertTrue(response.isNeedsGrouping());
        assertFalse(response.isNeedsTeams());
        assertFalse(response.isReadyForScoring());
        verify(roundTeamAutoAssignmentService).syncTeamsFromGroupsIfNeeded(101L);
    }

    @Test
    void getTripRounds_finalizedRoundDoesNotReportSetupWork() {
        Round round = round(102L, true);
        when(roundRepository.findByTrip_IdOrderByRoundDateAsc(10L)).thenReturn(List.of(round));
        when(scorecardRepository.findByRound_Id(102L)).thenReturn(Collections.emptyList());
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(102L)).thenReturn(Collections.emptyList());
        when(roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(102L)).thenReturn(Collections.emptyList());
        when(roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(102L)).thenReturn(Collections.emptyList());

        TripRoundListResponse response = service.getTripRounds(10L).get(0);

        assertTrue(response.getFinalized());
        assertFalse(response.isNeedsGrouping());
        assertFalse(response.isNeedsTeams());
        assertFalse(response.isReadyForScoring());
    }

    @Test
    void getTripRounds_groupedIndividualRoundIsReadyForScoring() {
        Round round = round(103L, false);
        Player player = mock(Player.class);
        when(player.getId()).thenReturn(501L);

        Scorecard scorecard = mock(Scorecard.class);
        when(scorecard.getPlayer()).thenReturn(player);

        RoundGroupPlayer groupPlayer = new RoundGroupPlayer();
        groupPlayer.setPlayer(player);
        RoundGroup group = new RoundGroup();
        group.setPlayers(List.of(groupPlayer));

        when(roundRepository.findByTrip_IdOrderByRoundDateAsc(10L)).thenReturn(List.of(round));
        when(scorecardRepository.findByRound_Id(103L)).thenReturn(List.of(scorecard));
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(103L)).thenReturn(List.of(group));
        when(roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(103L)).thenReturn(Collections.emptyList());
        when(roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(103L)).thenReturn(Collections.emptyList());
        when(roundEventCapabilityService.getCapabilities(round)).thenReturn(
                new RoundEventCapabilityService.RoundEventCapabilities(false, true, false, false, false, true, null));

        TripRoundListResponse response = service.getTripRounds(10L).get(0);

        assertFalse(response.isNeedsGrouping());
        assertFalse(response.isNeedsTeams());
        assertTrue(response.isReadyForScoring());
    }

    private Round round(Long id, boolean finalized) {
        Round round = mock(Round.class);
        when(round.getId()).thenReturn(id);
        when(round.getFinalized()).thenReturn(finalized);
        return round;
    }
}
