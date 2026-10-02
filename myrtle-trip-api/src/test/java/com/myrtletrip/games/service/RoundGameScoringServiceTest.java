package com.myrtletrip.games.service;

import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.event.service.RoundEventService;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RoundGameScoringServiceTest {

    @Test
    void recalculateRound_shouldMarkUsedHoleScores_forOneTwoThree() {
        Long roundId = 500L;

        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundScoringDataService roundScoringDataService = mock(RoundScoringDataService.class);
        ScorecardRepository scorecardRepository = mock(ScorecardRepository.class);
        HoleScoreRepository holeScoreRepository = mock(HoleScoreRepository.class);
        RoundEventService roundEventService = mock(RoundEventService.class);
        TripEditingGuardService tripEditingGuardService = mock(TripEditingGuardService.class);

        List<RoundGameScorer> scorers = new ArrayList<RoundGameScorer>();
        scorers.add(new OneTwoThreeScorer());

        RoundGameScoringService service = new RoundGameScoringService(
                roundRepository,
                roundScoringDataService,
                new RoundGameScorerRegistry(scorers),
                roundEventService,
                tripEditingGuardService,
                new RoundIndividualStrokePlayResultService(),
                new RoundTeamGameResultService(),
                new RoundUsedHoleScoreService(scorecardRepository, holeScoreRepository)
        );

        Round round = mock(Round.class);
        when(round.getId()).thenReturn(roundId);
        when(round.getFormat()).thenReturn(RoundFormat.ONE_TWO_THREE);
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));
        when(roundEventService.findActiveEventsForRound(roundId)).thenReturn(new ArrayList<>());

        RoundScoringData data = buildOneTwoThreeRoundData(roundId);
        when(roundScoringDataService.build(round)).thenReturn(data);

        List<Scorecard> scorecards = buildScorecards(roundId, 4);
        when(scorecardRepository.findByRound_Id(roundId)).thenReturn(scorecards);

        List<HoleScore> allHoleScores = buildHoleScores(scorecards);
        when(holeScoreRepository.findByScorecard_Round_Id(roundId)).thenReturn(allHoleScores);

        for (int scorecardIndex = 0; scorecardIndex < scorecards.size(); scorecardIndex++) {
            Scorecard scorecard = scorecards.get(scorecardIndex);
            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                HoleScore holeScore = findHoleScore(allHoleScores, scorecard.getId(), holeNumber);
                when(holeScoreRepository.findByScorecard_IdAndHoleNumber(scorecard.getId(), holeNumber))
                        .thenReturn(Optional.of(holeScore));
            }
        }

        RoundGameResult result = service.recalculateRound(roundId);

        assertEquals(roundId, result.getRoundId());
        assertEquals(RoundFormat.ONE_TWO_THREE, result.getFormat());
        assertEquals(1, result.getTeams().size());

        TeamGameResult team = result.getTeams().get(0);
        assertEquals(1L, team.getTeamId());
        assertEquals(168, team.getTotalNet());

        /*
         * Hole 1: use best 1 -> Player 1 only
         * Hole 2: use best 2 -> Players 1 and 2
         * Hole 3: use best 3 -> Players 1, 2, and 3
         * Hole 4: repeat -> Player 1 only
         */

        assertTrue(findHoleScore(allHoleScores, 1001L, 1).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1002L, 1).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1003L, 1).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1004L, 1).getUsedInTeamGame());

        assertTrue(findHoleScore(allHoleScores, 1001L, 2).getUsedInTeamGame());
        assertTrue(findHoleScore(allHoleScores, 1002L, 2).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1003L, 2).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1004L, 2).getUsedInTeamGame());

        assertTrue(findHoleScore(allHoleScores, 1001L, 3).getUsedInTeamGame());
        assertTrue(findHoleScore(allHoleScores, 1002L, 3).getUsedInTeamGame());
        assertTrue(findHoleScore(allHoleScores, 1003L, 3).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1004L, 3).getUsedInTeamGame());

        assertTrue(findHoleScore(allHoleScores, 1001L, 4).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1002L, 4).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1003L, 4).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1004L, 4).getUsedInTeamGame());

        /*
         * Also confirm reset behavior worked:
         * every hole score started TRUE, so any non-selected row above being FALSE
         * proves the service cleared flags before re-marking the used rows.
         */
    }

    @Test
    void recalculateRound_shouldMarkUsedHoleScores_forThreeLowNet() {
        Long roundId = 600L;

        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundScoringDataService roundScoringDataService = mock(RoundScoringDataService.class);
        ScorecardRepository scorecardRepository = mock(ScorecardRepository.class);
        HoleScoreRepository holeScoreRepository = mock(HoleScoreRepository.class);
        RoundEventService roundEventService = mock(RoundEventService.class);
        TripEditingGuardService tripEditingGuardService = mock(TripEditingGuardService.class);

        List<RoundGameScorer> scorers = new ArrayList<RoundGameScorer>();
        scorers.add(new ThreeLowNetScorer());

        RoundGameScoringService service = new RoundGameScoringService(
                roundRepository,
                roundScoringDataService,
                new RoundGameScorerRegistry(scorers),
                roundEventService,
                tripEditingGuardService,
                new RoundIndividualStrokePlayResultService(),
                new RoundTeamGameResultService(),
                new RoundUsedHoleScoreService(scorecardRepository, holeScoreRepository)
        );

        Round round = mock(Round.class);
        when(round.getId()).thenReturn(roundId);
        when(round.getFormat()).thenReturn(RoundFormat.THREE_LOW_NET);
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));
        when(roundEventService.findActiveEventsForRound(roundId)).thenReturn(new ArrayList<>());

        RoundScoringData data = buildThreeLowNetRoundData(roundId);
        when(roundScoringDataService.build(round)).thenReturn(data);

        List<Scorecard> scorecards = buildScorecards(roundId, 4);
        when(scorecardRepository.findByRound_Id(roundId)).thenReturn(scorecards);

        List<HoleScore> allHoleScores = buildHoleScores(scorecards);
        when(holeScoreRepository.findByScorecard_Round_Id(roundId)).thenReturn(allHoleScores);

        for (int scorecardIndex = 0; scorecardIndex < scorecards.size(); scorecardIndex++) {
            Scorecard scorecard = scorecards.get(scorecardIndex);
            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                HoleScore holeScore = findHoleScore(allHoleScores, scorecard.getId(), holeNumber);
                when(holeScoreRepository.findByScorecard_IdAndHoleNumber(scorecard.getId(), holeNumber))
                        .thenReturn(Optional.of(holeScore));
            }
        }

        RoundGameResult result = service.recalculateRound(roundId);

        assertEquals(roundId, result.getRoundId());
        assertEquals(RoundFormat.THREE_LOW_NET, result.getFormat());
        assertEquals(1, result.getTeams().size());

        TeamGameResult team = result.getTeams().get(0);
        assertEquals(1L, team.getTeamId());
        assertEquals(270, team.getTotalNet());

        /*
         * Every hole should use the lowest 3 of 4 players.
         * In this setup:
         * Player 1 = 4
         * Player 2 = 5
         * Player 3 = 6
         * Player 4 = 9
         * So Player 4 should be excluded every hole.
         */
        assertTrue(findHoleScore(allHoleScores, 1001L, 1).getUsedInTeamGame());
        assertTrue(findHoleScore(allHoleScores, 1002L, 1).getUsedInTeamGame());
        assertTrue(findHoleScore(allHoleScores, 1003L, 1).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1004L, 1).getUsedInTeamGame());

        assertTrue(findHoleScore(allHoleScores, 1001L, 18).getUsedInTeamGame());
        assertTrue(findHoleScore(allHoleScores, 1002L, 18).getUsedInTeamGame());
        assertTrue(findHoleScore(allHoleScores, 1003L, 18).getUsedInTeamGame());
        assertFalse(findHoleScore(allHoleScores, 1004L, 18).getUsedInTeamGame());
    }

    @Test
    void getRoundResult_shouldBuildIndividualStrokePlayAndAssignTiedPlacements() {
        Long roundId = 700L;

        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundScoringDataService roundScoringDataService = mock(RoundScoringDataService.class);
        ScorecardRepository scorecardRepository = mock(ScorecardRepository.class);
        HoleScoreRepository holeScoreRepository = mock(HoleScoreRepository.class);
        RoundEventService roundEventService = mock(RoundEventService.class);
        TripEditingGuardService tripEditingGuardService = mock(TripEditingGuardService.class);

        RoundGameScoringService service = new RoundGameScoringService(
                roundRepository,
                roundScoringDataService,
                new RoundGameScorerRegistry(new ArrayList<RoundGameScorer>()),
                roundEventService,
                tripEditingGuardService,
                new RoundIndividualStrokePlayResultService(),
                new RoundTeamGameResultService(),
                new RoundUsedHoleScoreService(scorecardRepository, holeScoreRepository)
        );

        Round round = mock(Round.class);
        when(round.getId()).thenReturn(roundId);
        when(round.getFormat()).thenReturn(RoundFormat.STROKE_PLAY);
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));
        when(roundEventService.findActiveEventsForRound(roundId)).thenReturn(new ArrayList<RoundEvent>());

        RoundScoringData data = new RoundScoringData();
        data.setRoundId(roundId);
        data.setFormat(RoundFormat.STROKE_PLAY);

        TeamScoringData group = new TeamScoringData();
        group.setTeamId(1L);
        group.setTeamName("Group 1");
        group.setPlayers(List.of(
                buildIndividualPlayer(1L, "Alice", 75, 70),
                buildIndividualPlayer(2L, "Bob", 76, 70),
                buildIndividualPlayer(3L, "Charlie", 80, 72)
        ));
        data.setTeams(List.of(group));
        when(roundScoringDataService.build(round)).thenReturn(data);

        RoundGameResult result = service.getRoundResult(roundId);

        assertEquals(3, result.getTeams().size());
        assertEquals("Alice", result.getTeams().get(0).getTeamName());
        assertEquals(1, result.getTeams().get(0).getPlacement());
        assertEquals("Bob", result.getTeams().get(1).getTeamName());
        assertEquals(1, result.getTeams().get(1).getPlacement());
        assertEquals("Charlie", result.getTeams().get(2).getTeamName());
        assertEquals(3, result.getTeams().get(2).getPlacement());
    }

    @Test
    void getRoundResult_shouldReturnUnscoredTeamRowsWhenRequiredScoresAreIncomplete() {
        Long roundId = 710L;

        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundScoringDataService roundScoringDataService = mock(RoundScoringDataService.class);
        ScorecardRepository scorecardRepository = mock(ScorecardRepository.class);
        HoleScoreRepository holeScoreRepository = mock(HoleScoreRepository.class);
        RoundEventService roundEventService = mock(RoundEventService.class);
        TripEditingGuardService tripEditingGuardService = mock(TripEditingGuardService.class);
        RoundGameScorer scorer = mock(RoundGameScorer.class);
        when(scorer.supports()).thenReturn(RoundEventType.TEAM_THREE_LOW_NET);

        RoundGameScoringService service = new RoundGameScoringService(
                roundRepository,
                roundScoringDataService,
                new RoundGameScorerRegistry(List.of(scorer)),
                roundEventService,
                tripEditingGuardService,
                new RoundIndividualStrokePlayResultService(),
                new RoundTeamGameResultService(),
                new RoundUsedHoleScoreService(scorecardRepository, holeScoreRepository)
        );

        Round round = mock(Round.class);
        when(round.getId()).thenReturn(roundId);
        when(round.getFormat()).thenReturn(RoundFormat.THREE_LOW_NET);
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));
        when(roundEventService.findActiveEventsForRound(roundId)).thenReturn(new ArrayList<RoundEvent>());

        RoundScoringData data = new RoundScoringData();
        data.setRoundId(roundId);
        data.setFormat(RoundFormat.THREE_LOW_NET);
        TeamScoringData team = new TeamScoringData();
        team.setTeamId(9L);
        team.setTeamName("Team 9");
        team.setPlayers(List.of(
                buildPlayer(91L, 1091L, "P1", new int[]{4}),
                buildPlayer(92L, 1092L, "P2", new int[]{5})
        ));
        data.setTeams(List.of(team));
        when(roundScoringDataService.build(round)).thenReturn(data);

        RoundGameResult result = service.getRoundResult(roundId);

        assertEquals(1, result.getTeams().size());
        assertEquals(9L, result.getTeams().get(0).getTeamId());
        assertEquals("Team 9", result.getTeams().get(0).getTeamName());
        assertEquals(0, result.getTeams().get(0).getTotalNet());
        verify(scorer, never()).scoreRound(data);
    }

    @Test
    void getRoundResult_shouldRejectExplicitEventTypeThatIsNotActiveForRound() {
        Long roundId = 720L;

        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundScoringDataService roundScoringDataService = mock(RoundScoringDataService.class);
        ScorecardRepository scorecardRepository = mock(ScorecardRepository.class);
        HoleScoreRepository holeScoreRepository = mock(HoleScoreRepository.class);
        RoundEventService roundEventService = mock(RoundEventService.class);
        TripEditingGuardService tripEditingGuardService = mock(TripEditingGuardService.class);

        RoundGameScoringService service = new RoundGameScoringService(
                roundRepository,
                roundScoringDataService,
                new RoundGameScorerRegistry(new ArrayList<RoundGameScorer>()),
                roundEventService,
                tripEditingGuardService,
                new RoundIndividualStrokePlayResultService(),
                new RoundTeamGameResultService(),
                new RoundUsedHoleScoreService(scorecardRepository, holeScoreRepository)
        );

        Round round = mock(Round.class);
        when(round.getId()).thenReturn(roundId);
        when(round.getFormat()).thenReturn(RoundFormat.STROKE_PLAY);
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));

        RoundEvent individual = new RoundEvent();
        individual.setEventType(RoundEventType.INDIVIDUAL_LOW_NET);
        when(roundEventService.findActiveEventsForRound(roundId)).thenReturn(List.of(individual));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.getRoundResult(roundId, RoundEventType.TEAM_SCRAMBLE)
        );

        assertTrue(error.getMessage().contains("does not have active event type TEAM_SCRAMBLE"));
    }

    @Test
    void recalculateRoundEvents_shouldProcessEveryActiveEventAndApplyCorrectionGuard() {
        Long roundId = 730L;

        RoundRepository roundRepository = mock(RoundRepository.class);
        RoundScoringDataService roundScoringDataService = mock(RoundScoringDataService.class);
        ScorecardRepository scorecardRepository = mock(ScorecardRepository.class);
        HoleScoreRepository holeScoreRepository = mock(HoleScoreRepository.class);
        RoundEventService roundEventService = mock(RoundEventService.class);
        TripEditingGuardService tripEditingGuardService = mock(TripEditingGuardService.class);

        RoundGameScoringService service = new RoundGameScoringService(
                roundRepository,
                roundScoringDataService,
                new RoundGameScorerRegistry(new ArrayList<RoundGameScorer>()),
                roundEventService,
                tripEditingGuardService,
                new RoundIndividualStrokePlayResultService(),
                new RoundTeamGameResultService(),
                new RoundUsedHoleScoreService(scorecardRepository, holeScoreRepository)
        );

        Round round = mock(Round.class);
        when(round.getId()).thenReturn(roundId);
        when(round.getFormat()).thenReturn(RoundFormat.STROKE_PLAY);
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));

        RoundEvent lowNet = new RoundEvent();
        lowNet.setEventType(RoundEventType.INDIVIDUAL_LOW_NET);
        RoundEvent lowGross = new RoundEvent();
        lowGross.setEventType(RoundEventType.INDIVIDUAL_LOW_GROSS);
        when(roundEventService.findActiveEventsForRound(roundId)).thenReturn(List.of(lowNet, lowGross));

        RoundScoringData data = new RoundScoringData();
        data.setRoundId(roundId);
        data.setFormat(RoundFormat.STROKE_PLAY);
        data.setTeams(new ArrayList<TeamScoringData>());
        when(roundScoringDataService.build(round)).thenReturn(data);
        when(holeScoreRepository.findByScorecard_Round_Id(roundId)).thenReturn(new ArrayList<HoleScore>());

        List<RoundGameResult> results = service.recalculateRoundEvents(roundId);

        assertEquals(2, results.size());
        assertEquals(roundId, results.get(0).getRoundId());
        assertEquals(roundId, results.get(1).getRoundId());
        verify(roundScoringDataService, times(2)).build(round);
        verify(tripEditingGuardService, times(3)).assertCorrectionAllowedForRound(round);
        verify(holeScoreRepository, times(2)).saveAll(new ArrayList<HoleScore>());
    }

    private PlayerScoringData buildIndividualPlayer(Long playerId, String name, int gross, int net) {
        PlayerScoringData player = new PlayerScoringData();
        player.setPlayerId(playerId);
        player.setPlayerName(name);

        PlayerHoleScoringData hole = new PlayerHoleScoringData();
        hole.setHoleNumber(1);
        hole.setGross(gross);
        hole.setNet(net);
        player.setHoles(List.of(hole));
        return player;
    }

    private RoundScoringData buildOneTwoThreeRoundData(Long roundId) {
        RoundScoringData data = new RoundScoringData();
        data.setRoundId(roundId);
        data.setFormat(RoundFormat.ONE_TWO_THREE);

        List<TeamScoringData> teams = new ArrayList<TeamScoringData>();
        teams.add(buildFourManTeam(
                1L,
                "Team 1",
                "A1",
                "A2",
                "A3",
                "A4",
                new int[]{4},
                new int[]{5},
                new int[]{6},
                new int[]{7}
        ));
        data.setTeams(teams);

        return data;
    }

    private RoundScoringData buildThreeLowNetRoundData(Long roundId) {
        RoundScoringData data = new RoundScoringData();
        data.setRoundId(roundId);
        data.setFormat(RoundFormat.THREE_LOW_NET);

        List<TeamScoringData> teams = new ArrayList<TeamScoringData>();
        teams.add(buildFourManTeam(
                1L,
                "Team 1",
                "A1",
                "A2",
                "A3",
                "A4",
                new int[]{4},
                new int[]{5},
                new int[]{6},
                new int[]{9}
        ));
        data.setTeams(teams);

        return data;
    }

    private TeamScoringData buildFourManTeam(Long teamId,
                                             String teamName,
                                             String player1Name,
                                             String player2Name,
                                             String player3Name,
                                             String player4Name,
                                             int[] player1Cycle,
                                             int[] player2Cycle,
                                             int[] player3Cycle,
                                             int[] player4Cycle) {
        TeamScoringData team = new TeamScoringData();
        team.setTeamId(teamId);
        team.setTeamName(teamName);

        List<PlayerScoringData> players = new ArrayList<PlayerScoringData>();
        players.add(buildPlayer(1L, 1001L, player1Name, player1Cycle));
        players.add(buildPlayer(2L, 1002L, player2Name, player2Cycle));
        players.add(buildPlayer(3L, 1003L, player3Name, player3Cycle));
        players.add(buildPlayer(4L, 1004L, player4Name, player4Cycle));
        team.setPlayers(players);

        return team;
    }

    private PlayerScoringData buildPlayer(Long playerId,
                                          Long scorecardId,
                                          String playerName,
                                          int[] cycleValues) {
        PlayerScoringData player = new PlayerScoringData();
        player.setPlayerId(playerId);
        player.setScorecardId(scorecardId);
        player.setPlayerName(playerName);

        List<PlayerHoleScoringData> holes = new ArrayList<PlayerHoleScoringData>();
        for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
            int cycleIndex = (holeNumber - 1) % cycleValues.length;
            int net = cycleValues[cycleIndex];

            PlayerHoleScoringData hole = new PlayerHoleScoringData();
            hole.setHoleNumber(holeNumber);
            hole.setGross(net);
            hole.setNet(net);
            holes.add(hole);
        }

        player.setHoles(holes);
        return player;
    }

    private List<Scorecard> buildScorecards(Long roundId, int playerCount) {
        List<Scorecard> scorecards = new ArrayList<Scorecard>();

        for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
            Player player = new Player();
            player.setId((long) playerNumber);
            player.setDisplayName("Player " + playerNumber);

            Scorecard scorecard = new Scorecard();
            scorecard.setId(1000L + playerNumber);
            scorecard.setPlayer(player);

            Round round = mock(Round.class);
            when(round.getId()).thenReturn(roundId);
            scorecard.setRound(round);

            scorecards.add(scorecard);
        }

        return scorecards;
    }

    private List<HoleScore> buildHoleScores(List<Scorecard> scorecards) {
        List<HoleScore> holeScores = new ArrayList<HoleScore>();

        for (int scorecardIndex = 0; scorecardIndex < scorecards.size(); scorecardIndex++) {
            Scorecard scorecard = scorecards.get(scorecardIndex);

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                HoleScore holeScore = new HoleScore();
                holeScore.setScorecard(scorecard);
                holeScore.setHoleNumber(holeNumber);
                holeScore.setUsedInTeamGame(Boolean.TRUE);
                holeScores.add(holeScore);
            }
        }

        return holeScores;
    }

    private HoleScore findHoleScore(List<HoleScore> holeScores, Long scorecardId, int holeNumber) {
        for (int i = 0; i < holeScores.size(); i++) {
            HoleScore holeScore = holeScores.get(i);
            if (holeScore.getScorecard() != null
                    && holeScore.getScorecard().getId() != null
                    && holeScore.getScorecard().getId().equals(scorecardId)
                    && holeScore.getHoleNumber() != null
                    && holeScore.getHoleNumber() == holeNumber) {
                return holeScore;
            }
        }

        throw new IllegalStateException(
                "HoleScore not found for scorecardId=" + scorecardId + ", holeNumber=" + holeNumber
        );
    }
}
