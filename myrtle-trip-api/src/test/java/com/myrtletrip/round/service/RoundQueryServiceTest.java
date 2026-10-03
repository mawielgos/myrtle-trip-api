package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.permissions.dto.RoundCapabilityResponse;
import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundScorecardSummaryResponse;
import com.myrtletrip.round.dto.RoundStatusResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoundQueryServiceTest {

    @Mock private RoundRepository roundRepository;
    @Mock private ScorecardRepository scorecardRepository;
    @Mock private RoundTeamPlayerRepository roundTeamPlayerRepository;
    @Mock private RoundGroupRepository roundGroupRepository;
    @Mock private RoundTeeResolver roundTeeResolver;
    @Mock private RoundHandicapService roundHandicapService;
    @Mock private RoundCapabilityService roundCapabilityService;
    @Mock private RoundEventCapabilityService roundEventCapabilityService;

    private RoundQueryService service;

    @BeforeEach
    void setUp() {
        RoundStatusReadModelService roundStatusReadModelService = new RoundStatusReadModelService(
                roundRepository,
                scorecardRepository,
                roundTeamPlayerRepository,
                roundGroupRepository,
                roundTeeResolver,
                roundHandicapService,
                roundCapabilityService,
                roundEventCapabilityService
        );

        RoundScorecardSummaryReadModelService roundScorecardSummaryReadModelService =
                new RoundScorecardSummaryReadModelService(
                        roundRepository,
                        scorecardRepository,
                        roundTeamPlayerRepository,
                        roundGroupRepository,
                        roundTeeResolver,
                        roundHandicapService
                );

        service = new RoundQueryService(
                roundStatusReadModelService,
                roundScorecardSummaryReadModelService
        );
    }

    @Test
    void getRoundStatus_shouldRejectMissingRound() {
        when(roundRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.getRoundStatus(10L)
        );

        assertEquals("Round not found", error.getMessage());
        verifyNoInteractions(scorecardRepository, roundTeamPlayerRepository, roundGroupRepository);
    }

    @Test
    void getRoundStatus_shouldMapTripLockMetadataAndCapabilities() {
        Trip trip = mockTrip(5L, "T26", TripStatus.COMPLETE, false);
        RoundTee defaultTee = mock(RoundTee.class);
        when(defaultTee.getCourseName()).thenReturn("Pine Hills");
        when(defaultTee.getTeeName()).thenReturn("Blue");

        Round round = mockRound(10L, trip, LocalDate.of(2026, 10, 2), RoundFormat.STROKE_PLAY, false);
        when(round.getDefaultRoundTee()).thenReturn(defaultTee);
        when(roundRepository.findById(10L)).thenReturn(Optional.of(round));
        when(scorecardRepository.findByRound_Id(10L)).thenReturn(new ArrayList<>());
        when(roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(10L)).thenReturn(new ArrayList<>());
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(10L)).thenReturn(new ArrayList<>());
        when(roundEventCapabilityService.isScrambleRound(round)).thenReturn(false);

        RoundCapabilityResponse capabilities = new RoundCapabilityResponse();
        capabilities.setRoundId(10L);
        when(roundCapabilityService.build(round)).thenReturn(capabilities);

        RoundStatusResponse result = service.getRoundStatus(10L);

        assertEquals(10L, result.getRoundId());
        assertEquals(5L, result.getTripId());
        assertEquals("Pine Hills", result.getCourseName());
        assertEquals("Blue", result.getTeeName());
        assertEquals("STROKE_PLAY", result.getFormat());
        assertEquals(4, result.getScrambleTeamSize());
        assertNull(result.getScrambleScoreEntryMode());
        assertEquals("COMPLETE", result.getTripStatus());
        assertFalse(result.getTripCorrectionMode());
        assertTrue(result.getTripLocked());
        assertFalse(result.getEditable());
        assertSame(capabilities, result.getCapabilities());
    }

    @Test
    void getRoundStatus_shouldOrderPlayersByGroupAndMapHandicapSnapshot() {
        Trip trip = mockTrip(6L, "T26", TripStatus.IN_PROGRESS, false);
        Round round = mockRound(20L, trip, LocalDate.of(2026, 10, 3), RoundFormat.STROKE_PLAY, false);
        when(roundRepository.findById(20L)).thenReturn(Optional.of(round));
        when(roundEventCapabilityService.isScrambleRound(round)).thenReturn(false);
        when(roundCapabilityService.build(round)).thenReturn(new RoundCapabilityResponse());
        when(roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(20L)).thenReturn(new ArrayList<>());

        Player laterPlayer = player(1L, "Later Player", "M", "GHIN");
        Player firstPlayer = player(2L, "First Player", "female", "GHIN");
        Scorecard laterScorecard = scorecard(101L, round, laterPlayer, 8, 7);
        Scorecard firstScorecard = scorecard(102L, round, firstPlayer, 12, 10);
        when(scorecardRepository.findByRound_Id(20L)).thenReturn(new java.util.ArrayList<>(List.of(laterScorecard, firstScorecard)));

        RoundGroup group1 = group(201L, 1, firstPlayer, 1);
        RoundGroup group2 = group(202L, 2, laterPlayer, 1);
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(20L)).thenReturn(new ArrayList<>(List.of(group1, group2)));

        RoundTee silver = mock(RoundTee.class);
        when(silver.getId()).thenReturn(301L);
        when(silver.getTeeName()).thenReturn("Silver");
        when(roundTeeResolver.resolve(firstScorecard)).thenReturn(silver);
        when(roundTeeResolver.resolve(laterScorecard)).thenReturn(silver);
        when(roundHandicapService.calculateTripIndex(firstScorecard, "T26")).thenReturn(new BigDecimal("9.4"));
        when(roundHandicapService.calculateTripIndex(laterScorecard, "T26")).thenReturn(new BigDecimal("7.2"));

        RoundStatusResponse result = service.getRoundStatus(20L);

        assertEquals(2, result.getPlayers().size());
        assertEquals(2L, result.getPlayers().get(0).getPlayerId());
        assertEquals("Group 1", result.getPlayers().get(0).getTeamName());
        assertEquals(1, result.getPlayers().get(0).getTeamNumber());
        assertEquals(1, result.getPlayers().get(0).getPlayerOrder());
        assertEquals("F", result.getPlayers().get(0).getGender());
        assertEquals("Silver", result.getPlayers().get(0).getRoundTeeName());
        assertEquals(new BigDecimal("9.4"), result.getPlayers().get(0).getTripIndex());
        assertEquals("GHIN index as of 2026-10-03 (same-day scores excluded)", result.getPlayers().get(0).getHandicapLabel());
        assertEquals(1L, result.getPlayers().get(1).getPlayerId());
        assertEquals(2, result.getPlayers().get(1).getTeamNumber());
    }

    @Test
    void getRoundScorecards_shouldPreferTeamAssignmentOrderAndMapScores() {
        Trip trip = mockTrip(7L, "T26", TripStatus.IN_PROGRESS, false);
        Round round = mockRound(30L, trip, LocalDate.of(2026, 10, 4), RoundFormat.TWO_MAN_LOW_NET, false);
        when(roundRepository.findById(30L)).thenReturn(Optional.of(round));

        Player player = player(3L, "Team Player", "M", "DB_SCORE_HISTORY");
        Scorecard scorecard = scorecard(103L, round, player, 9, 8);
        scorecard.setGrossScore(82);
        scorecard.setAdjustedGrossScore(81);
        scorecard.setNetScore(73);
        when(scorecardRepository.findByRound_Id(30L)).thenReturn(new java.util.ArrayList<>(List.of(scorecard)));

        RoundTeam team = mock(RoundTeam.class);
        when(team.getId()).thenReturn(401L);
        when(team.getTeamName()).thenReturn("Team Two");
        when(team.getTeamNumber()).thenReturn(2);
        scorecard.setTeam(team);

        RoundTeamPlayer assignment = new RoundTeamPlayer();
        assignment.setPlayer(player);
        assignment.setRoundTeam(team);
        assignment.setPlayerOrder(2);
        when(roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(30L)).thenReturn(new ArrayList<>(List.of(assignment)));
        RoundGroup assignedGroup = group(402L, 1, player, 1);
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(30L)).thenReturn(new ArrayList<>(List.of(assignedGroup)));

        RoundTee black = mock(RoundTee.class);
        when(black.getId()).thenReturn(302L);
        when(black.getTeeName()).thenReturn("Black");
        when(roundTeeResolver.resolve(scorecard)).thenReturn(black);
        when(roundHandicapService.calculateTripIndex(scorecard, "T26")).thenReturn(new BigDecimal("8.1"));

        List<RoundScorecardSummaryResponse> results = service.getRoundScorecards(30L);

        assertEquals(1, results.size());
        RoundScorecardSummaryResponse result = results.get(0);
        assertEquals(401L, result.getTeamId());
        assertEquals("Team Two", result.getTeamName());
        assertEquals(2, result.getTeamNumber());
        assertEquals(2, result.getPlayerOrder());
        assertEquals(82, result.getGrossScore());
        assertEquals(81, result.getAdjustedGrossScore());
        assertEquals(73, result.getNetScore());
        assertEquals("Black", result.getTeeName());
        assertEquals("DB Score History index as of 2026-10-04 (same-day scores excluded)", result.getHandicapLabel());
    }

    @Test
    void getRoundScorecards_shouldSuppressTripIndexCalculationFailure() {
        Trip trip = mockTrip(8L, "T26", TripStatus.IN_PROGRESS, false);
        Round round = mockRound(40L, trip, LocalDate.of(2026, 10, 5), RoundFormat.STROKE_PLAY, false);
        when(roundRepository.findById(40L)).thenReturn(Optional.of(round));

        Player player = player(4L, "Index Failure", "M", "GHIN");
        Scorecard scorecard = scorecard(104L, round, player, 10, 9);
        when(scorecardRepository.findByRound_Id(40L)).thenReturn(new java.util.ArrayList<>(List.of(scorecard)));
        when(roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(40L)).thenReturn(new ArrayList<>());
        when(roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(40L)).thenReturn(new ArrayList<>());
        when(roundHandicapService.calculateTripIndex(scorecard, "T26")).thenThrow(new IllegalStateException("bad history"));

        List<RoundScorecardSummaryResponse> results = service.getRoundScorecards(40L);

        assertEquals(1, results.size());
        assertNull(results.get(0).getTripIndex());
        assertEquals("GHIN index as of 2026-10-05 (same-day scores excluded)", results.get(0).getHandicapLabel());
    }

    private Trip mockTrip(Long id, String tripCode, TripStatus status, boolean correctionMode) {
        Trip trip = mock(Trip.class);
        lenient().when(trip.getId()).thenReturn(id);
        lenient().when(trip.getTripCode()).thenReturn(tripCode);
        lenient().when(trip.getStatus()).thenReturn(status);
        lenient().when(trip.getCorrectionMode()).thenReturn(correctionMode);
        return trip;
    }

    private Round mockRound(Long id, Trip trip, LocalDate roundDate, RoundFormat format, boolean finalized) {
        Round round = mock(Round.class);
        lenient().when(round.getId()).thenReturn(id);
        lenient().when(round.getTrip()).thenReturn(trip);
        lenient().when(round.getRoundDate()).thenReturn(roundDate);
        lenient().when(round.getFormat()).thenReturn(format);
        lenient().when(round.getFinalized()).thenReturn(finalized);
        return round;
    }

    private Player player(Long id, String name, String gender, String handicapMethod) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName(name);
        player.setGender(gender);
        player.setHandicapMethod(handicapMethod);
        return player;
    }

    private Scorecard scorecard(Long id, Round round, Player player, int courseHandicap, int playingHandicap) {
        Scorecard scorecard = new Scorecard();
        scorecard.setId(id);
        scorecard.setRound(round);
        scorecard.setPlayer(player);
        scorecard.setCourseHandicap(courseHandicap);
        scorecard.setPlayingHandicap(playingHandicap);
        return scorecard;
    }

    private RoundGroup group(Long id, int groupNumber, Player player, int seatOrder) {
        RoundGroup group = mock(RoundGroup.class);
        lenient().when(group.getId()).thenReturn(id);
        lenient().when(group.getGroupNumber()).thenReturn(groupNumber);

        RoundGroupPlayer groupPlayer = new RoundGroupPlayer();
        groupPlayer.setPlayer(player);
        groupPlayer.setSeatOrder(seatOrder);
        lenient().when(group.getPlayers()).thenReturn(new ArrayList<>(List.of(groupPlayer)));
        return group;
    }
}
