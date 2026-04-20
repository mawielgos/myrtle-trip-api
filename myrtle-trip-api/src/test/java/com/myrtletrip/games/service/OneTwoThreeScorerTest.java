package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.HoleGameResult;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.round.model.RoundFormat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OneTwoThreeScorerTest {

    @Test
    void scoreRound_shouldUseOneTwoThreePatternAndAssignPoints() {
        OneTwoThreeScorer scorer = new OneTwoThreeScorer();

        RoundScoringData data = new RoundScoringData();
        data.setRoundId(101L);
        data.setFormat(RoundFormat.ONE_TWO_THREE);

        List<TeamScoringData> teams = new ArrayList<TeamScoringData>();
        teams.add(buildFourManTeam(
        	    1L,
        	    "Team 1",
        	    "A1", "A2", "A3", "A4",
        	    new int[]{4, 4, 4, 4},
        	    new int[]{5, 5, 5, 5},
        	    new int[]{6, 6, 6, 6},
        	    new int[]{7, 7, 7, 7}   // <-- ADD THIS
        	));
        teams.add(buildFourManTeam(
                2L,
                "Team 2",
                "B1", "B2", "B3", "B4",
                new int[]{5, 5, 5, 5},
                new int[]{6, 6, 6, 6},
                new int[]{7, 7, 7, 7},
                new int[]{8, 8, 8, 8}   // <-- ADD THIS
        ));
        data.setTeams(teams);

        RoundGameResult result = scorer.scoreRound(data);

        assertEquals(2, result.getTeams().size());

        TeamGameResult team1 = findTeam(result, 1L);
        TeamGameResult team2 = findTeam(result, 2L);

        assertEquals(168, team1.getTotalNet());
        assertEquals(204, team2.getTotalNet());

        assertEquals(18, team1.getTotalPoints());
        assertEquals(0, team2.getTotalPoints());

        assertEquals(1, team1.getPlacement());
        assertEquals(2, team2.getPlacement());

        HoleGameResult team1Hole1 = findHole(team1, 1);
        HoleGameResult team1Hole2 = findHole(team1, 2);
        HoleGameResult team1Hole3 = findHole(team1, 3);
        HoleGameResult team1Hole4 = findHole(team1, 4);

        assertEquals(4, team1Hole1.getNetScore());
        assertEquals(9, team1Hole2.getNetScore());
        assertEquals(15, team1Hole3.getNetScore());
        assertEquals(4, team1Hole4.getNetScore());

        assertEquals(1, team1Hole1.getPoints());
        assertEquals(1, team1Hole2.getPoints());
        assertEquals(1, team1Hole3.getPoints());
        assertEquals(1, team1Hole4.getPoints());
   }

    private TeamGameResult findTeam(RoundGameResult result, Long teamId) {
        for (TeamGameResult team : result.getTeams()) {
            if (team.getTeamId().equals(teamId)) {
                return team;
            }
        }
        throw new IllegalStateException("Team not found: " + teamId);
    }

    private HoleGameResult findHole(TeamGameResult team, int holeNumber) {
        for (HoleGameResult hole : team.getHoleResults()) {
            if (hole.getHoleNumber() == holeNumber) {
                return hole;
            }
        }
        throw new IllegalStateException("Hole not found: " + holeNumber);
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
        players.add(buildPlayer(teamId * 10 + 1, player1Name, player1Cycle));
        players.add(buildPlayer(teamId * 10 + 2, player2Name, player2Cycle));
        players.add(buildPlayer(teamId * 10 + 3, player3Name, player3Cycle));
        players.add(buildPlayer(teamId * 10 + 4, player4Name, player4Cycle));
        team.setPlayers(players);

        return team;
    }

    private PlayerScoringData buildPlayer(long playerId, String playerName, int[] cycleValues) {
        PlayerScoringData player = new PlayerScoringData();
        player.setPlayerId(playerId);
        player.setPlayerName(playerName);
        player.setScorecardId(playerId);

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
}
