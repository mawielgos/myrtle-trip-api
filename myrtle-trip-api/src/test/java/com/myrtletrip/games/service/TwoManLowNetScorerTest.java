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

public class TwoManLowNetScorerTest {

    @Test
    void scoreRound_shouldUseBothPlayersNetScoresEachHole() {
        TwoManLowNetScorer scorer = new TwoManLowNetScorer();

        RoundScoringData data = new RoundScoringData();
        data.setRoundId(103L);
        data.setFormat(RoundFormat.TWO_MAN_LOW_NET);

        List<TeamScoringData> teams = new ArrayList<TeamScoringData>();
        teams.add(buildTwoManTeam(
                1L,
                "Team 1",
                "A1",
                "A2",
                new int[]{4},
                new int[]{5}
        ));
        teams.add(buildTwoManTeam(
                2L,
                "Team 2",
                "B1",
                "B2",
                new int[]{5},
                new int[]{6}
        ));
        data.setTeams(teams);

        RoundGameResult result = scorer.scoreRound(data);

        TeamGameResult team1 = findTeam(result, 1L);
        TeamGameResult team2 = findTeam(result, 2L);

        assertEquals(162, team1.getTotalNet());
        assertEquals(198, team2.getTotalNet());

        assertEquals(18, team1.getTotalPoints());
        assertEquals(0, team2.getTotalPoints());

        assertEquals(1, team1.getPlacement());
        assertEquals(2, team2.getPlacement());

        HoleGameResult hole1 = findHole(team1, 1);
        assertEquals(9, hole1.getNetScore());
        assertEquals(1, hole1.getPoints());
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

    private TeamScoringData buildTwoManTeam(Long teamId,
                                            String teamName,
                                            String player1Name,
                                            String player2Name,
                                            int[] player1Cycle,
                                            int[] player2Cycle) {
        TeamScoringData team = new TeamScoringData();
        team.setTeamId(teamId);
        team.setTeamName(teamName);

        List<PlayerScoringData> players = new ArrayList<PlayerScoringData>();
        players.add(buildPlayer(teamId * 10 + 1, player1Name, player1Cycle));
        players.add(buildPlayer(teamId * 10 + 2, player2Name, player2Cycle));
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