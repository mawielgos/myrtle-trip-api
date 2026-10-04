package com.myrtletrip.standings.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.prize.entity.PrizeSchedule;
import com.myrtletrip.prize.entity.PrizeSchedulePayout;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.service.RoundEventCapabilityService;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.standings.dto.TournamentStandingRowResponse;
import com.myrtletrip.standings.dto.TournamentStandingsResponse;
import com.myrtletrip.tournament.model.TournamentCompetitionType;
import com.myrtletrip.tournament.service.TripTournamentService;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentStandingsServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private TripPlayerRepository tripPlayerRepository;
    @Mock private TripTournamentService tripTournamentService;
    @Mock private RoundRepository roundRepository;
    @Mock private ScorecardRepository scorecardRepository;
    @Mock private PrizeScheduleRepository prizeScheduleRepository;
    @Mock private RoundEventCapabilityService roundEventCapabilityService;

    private TournamentStandingsService service;

    @BeforeEach
    void setUp() {
        TournamentStandingsCalculationService calculationService = new TournamentStandingsCalculationService(
                tripRepository,
                tripPlayerRepository,
                tripTournamentService,
                roundRepository,
                scorecardRepository,
                roundEventCapabilityService
        );
        TournamentStandingsPayoutService payoutService = new TournamentStandingsPayoutService(prizeScheduleRepository);
        service = new TournamentStandingsService(calculationService, payoutService);
    }

    @Test
    void getTournamentStandings_shouldRejectMissingTrip() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getTournamentStandings(99L)
        );

        assertEquals("Trip not found: 99", ex.getMessage());
    }

    @Test
    void getTournamentStandings_shouldExcludeUnfinalizedAndScrambleRounds() {
        Trip trip = trip(1L, "Myrtle 2026");
        Player player = player(10L, "Alice");
        TripPlayer tripPlayer = tripPlayer(trip, player, 1);

        TripPlannedRound planned1 = plannedRound(trip, 1);
        TripPlannedRound planned2 = plannedRound(trip, 2);
        TripPlannedRound planned3 = plannedRound(trip, 3);

        Round finalized = round(101L, trip, 1, true, 72);
        Round unfinalized = round(102L, trip, 2, false, 72);
        Round scramble = round(103L, trip, 3, true, 72);

        Scorecard scorecard = scorecard(finalized, player, 80, 72);

        stubBaseTrip(trip, List.of(tripPlayer), List.of(planned1, planned2, planned3));
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(1L))
                .thenReturn(new ArrayList<>(List.of(finalized, unfinalized, scramble)));
        when(roundEventCapabilityService.isScrambleRound(finalized)).thenReturn(false);
        when(roundEventCapabilityService.isScrambleRound(scramble)).thenReturn(true);
        when(scorecardRepository.findByRound_Id(101L)).thenReturn(new ArrayList<>(List.of(scorecard)));

        TournamentStandingsResponse response = service.getTournamentStandings(1L);

        assertEquals(3, response.getRequiredRounds());
        assertEquals(1, response.getCompletedRounds());
        assertFalse(response.getLeaderboardFinal());
        assertEquals(1, response.getRoundLabels().size());
        assertEquals(1, response.getRows().get(0).getCompletedRounds());
    }

    @Test
    void getTournamentStandings_shouldUseNetOrGrossScoreForRequestedCompetition() {
        Trip trip = trip(1L, "Myrtle 2026");
        Player player = player(10L, "Alice");
        TripPlayer tripPlayer = tripPlayer(trip, player, 1);
        TripPlannedRound planned = plannedRound(trip, 1);
        Round round = round(101L, trip, 1, true, 72);
        Scorecard scorecard = scorecard(round, player, 80, 72);

        stubBaseTrip(trip, List.of(tripPlayer), List.of(planned));
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(1L))
                .thenReturn(new ArrayList<>(List.of(round)));
        when(roundEventCapabilityService.isScrambleRound(round)).thenReturn(false);
        when(scorecardRepository.findByRound_Id(101L)).thenReturn(new ArrayList<>(List.of(scorecard)));
        when(prizeScheduleRepository.findByTrip_IdAndGameKey(anyLong(), anyString())).thenReturn(Optional.empty());

        TournamentStandingsResponse net = service.getTournamentStandings(1L, TournamentCompetitionType.LOW_NET.name());
        TournamentStandingsResponse gross = service.getTournamentStandings(1L, TournamentCompetitionType.LOW_GROSS.name());

        assertEquals(72, net.getRows().get(0).getTotalScore());
        assertEquals(0, net.getRows().get(0).getTotalToPar());
        assertEquals(80, gross.getRows().get(0).getTotalScore());
        assertEquals(8, gross.getRows().get(0).getTotalToPar());
    }

    @Test
    void getTournamentStandings_shouldAssignSamePositionToTiedCompletePlayers() {
        Trip trip = trip(1L, "Myrtle 2026");
        Player alice = player(10L, "Alice");
        Player bob = player(20L, "Bob");
        TripPlayer tripAlice = tripPlayer(trip, alice, 1);
        TripPlayer tripBob = tripPlayer(trip, bob, 2);
        TripPlannedRound planned = plannedRound(trip, 1);
        Round round = round(101L, trip, 1, true, 72);
        Scorecard aliceCard = scorecard(round, alice, 76, 70);
        Scorecard bobCard = scorecard(round, bob, 78, 70);

        stubBaseTrip(trip, List.of(tripAlice, tripBob), List.of(planned));
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(1L))
                .thenReturn(new ArrayList<>(List.of(round)));
        when(roundEventCapabilityService.isScrambleRound(round)).thenReturn(false);
        when(scorecardRepository.findByRound_Id(101L))
                .thenReturn(new ArrayList<>(List.of(aliceCard, bobCard)));
        when(prizeScheduleRepository.findByTrip_IdAndGameKey(1L, "TOURNAMENT_LOW_NET"))
                .thenReturn(Optional.empty());
        when(prizeScheduleRepository.findByTrip_IdAndGameKey(1L, "FOUR_DAY_INDIVIDUAL"))
                .thenReturn(Optional.empty());

        TournamentStandingsResponse response = service.getTournamentStandings(1L);

        assertTrue(response.getLeaderboardFinal());
        assertEquals(2, response.getRows().size());
        assertEquals(1, response.getRows().get(0).getPosition());
        assertEquals(1, response.getRows().get(1).getPosition());
    }

    @Test
    void getTournamentStandings_shouldSplitTiedPayoutPoolAcrossPlayers() {
        Trip trip = trip(1L, "Myrtle 2026");
        Player alice = player(10L, "Alice");
        Player bob = player(20L, "Bob");
        TripPlayer tripAlice = tripPlayer(trip, alice, 1);
        TripPlayer tripBob = tripPlayer(trip, bob, 2);
        TripPlannedRound planned = plannedRound(trip, 1);
        Round round = round(101L, trip, 1, true, 72);
        Scorecard aliceCard = scorecard(round, alice, 76, 70);
        Scorecard bobCard = scorecard(round, bob, 78, 70);

        PrizeSchedule schedule = new PrizeSchedule();
        PrizeSchedulePayout first = payout(schedule, 1, "100.00");
        PrizeSchedulePayout second = payout(schedule, 2, "60.00");
        schedule.setPayouts(new ArrayList<>(List.of(first, second)));

        stubBaseTrip(trip, List.of(tripAlice, tripBob), List.of(planned));
        when(roundRepository.findByTrip_IdOrderByRoundNumberAsc(1L))
                .thenReturn(new ArrayList<>(List.of(round)));
        when(roundEventCapabilityService.isScrambleRound(round)).thenReturn(false);
        when(scorecardRepository.findByRound_Id(101L))
                .thenReturn(new ArrayList<>(List.of(aliceCard, bobCard)));
        when(prizeScheduleRepository.findByTrip_IdAndGameKey(1L, "TOURNAMENT_LOW_NET"))
                .thenReturn(Optional.of(schedule));

        TournamentStandingsResponse response = service.getTournamentStandings(1L);

        assertTrue(response.getLeaderboardFinal());
        for (TournamentStandingRowResponse row : response.getRows()) {
            assertEquals(new BigDecimal("80.00"), row.getMoney());
        }
    }

    private void stubBaseTrip(Trip trip, List<TripPlayer> tripPlayers, List<TripPlannedRound> plannedRounds) {
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(1L))
                .thenReturn(new ArrayList<>(tripPlayers));
        when(tripTournamentService.getIncludedTournamentPlannedRounds(1L))
                .thenReturn(new ArrayList<>(plannedRounds));
        when(tripTournamentService.getCompetitionName(eq(1L), any(TournamentCompetitionType.class)))
                .thenAnswer(invocation -> ((TournamentCompetitionType) invocation.getArgument(1)).getDisplayName());
    }

    private Trip trip(Long id, String name) {
        Trip trip = mock(Trip.class);
        lenient().when(trip.getId()).thenReturn(id);
        lenient().when(trip.getName()).thenReturn(name);
        return trip;
    }

    private Player player(Long id, String displayName) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName(displayName);
        return player;
    }

    private TripPlayer tripPlayer(Trip trip, Player player, int order) {
        TripPlayer tripPlayer = new TripPlayer();
        tripPlayer.setTrip(trip);
        tripPlayer.setPlayer(player);
        tripPlayer.setDisplayOrder(order);
        return tripPlayer;
    }

    private TripPlannedRound plannedRound(Trip trip, int roundNumber) {
        TripPlannedRound plannedRound = new TripPlannedRound();
        plannedRound.setTrip(trip);
        plannedRound.setRoundNumber(roundNumber);
        return plannedRound;
    }

    private Round round(Long id, Trip trip, int number, boolean finalized, int par) {
        Round round = mock(Round.class);
        RoundTee tee = new RoundTee();
        tee.setParTotal(par);
        lenient().when(round.getId()).thenReturn(id);
        lenient().when(round.getTrip()).thenReturn(trip);
        lenient().when(round.getRoundNumber()).thenReturn(number);
        lenient().when(round.getFinalized()).thenReturn(finalized);
        lenient().when(round.getDefaultRoundTee()).thenReturn(tee);
        lenient().when(round.getStandardRoundTee()).thenReturn(tee);
        return round;
    }

    private Scorecard scorecard(Round round, Player player, int gross, int net) {
        Scorecard scorecard = new Scorecard();
        scorecard.setRound(round);
        scorecard.setPlayer(player);
        scorecard.setGrossScore(gross);
        scorecard.setNetScore(net);
        return scorecard;
    }

    private PrizeSchedulePayout payout(PrizeSchedule schedule, int place, String amount) {
        PrizeSchedulePayout payout = new PrizeSchedulePayout();
        payout.setPrizeSchedule(schedule);
        payout.setFinishingPlace(place);
        payout.setAmountPerPlayer(new BigDecimal(amount));
        return payout;
    }
}
