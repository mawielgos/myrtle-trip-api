package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.GameScoreResponse;
import com.myrtletrip.games.dto.HoleGameResultResponse;
import com.myrtletrip.games.dto.TeamGameResultResponse;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class ThreeLowNetCalculator implements RoundGameCalculator {

    private final RoundGameDataService roundGameDataService;

    public ThreeLowNetCalculator(RoundGameDataService roundGameDataService) {
        this.roundGameDataService = roundGameDataService;
    }

    @Override
    public RoundFormat getSupportedFormat() {
        return RoundFormat.THREE_LOW_NET;
    }

    @Override
    public GameScoreResponse calculate(Long roundId) {

        List<RoundTeam> teams = roundGameDataService.getTeams(roundId);
        Map<Long, Scorecard> scorecardsByPlayerId = roundGameDataService.getScorecardsByPlayerId(roundId);
        Map<Long, List<HoleScore>> holesByScorecardId = roundGameDataService.getHoleScoresByScorecardId(roundId);

        List<TeamGameResultResponse> results = new ArrayList<>();
        
        if (teams.isEmpty()) {
            throw new IllegalStateException("No teams have been assigned for this round");
        }
        
        for (RoundTeam team : teams) {
            List<RoundTeamPlayer> teamPlayers = roundGameDataService.getTeamPlayers(team.getId());

            TeamGameResultResponse teamResult = new TeamGameResultResponse();
            teamResult.setTeamId(team.getId());
            teamResult.setTeamNumber(team.getTeamNumber());
            teamResult.setTeamName(team.getTeamName());

            List<String> playerNames = teamPlayers.stream()
                    .map(tp -> tp.getPlayer().getDisplayName())
                    .toList();
            teamResult.setPlayerNames(playerNames);

            List<HoleGameResultResponse> holeResults = new ArrayList<>();
            int total = 0;

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
            	final int currentHole = holeNumber;
                List<Integer> netScores = new ArrayList<>();

                for (RoundTeamPlayer teamPlayer : teamPlayers) {
                    Scorecard scorecard = scorecardsByPlayerId.get(teamPlayer.getPlayer().getId());
                    if (scorecard == null) {
                        throw new IllegalStateException("Missing scorecard for player " + teamPlayer.getPlayer().getId());
                    }

                    List<HoleScore> holes = holesByScorecardId.get(scorecard.getId());
                    HoleScore hole = holes.stream()
                            .filter(h -> h.getHoleNumber() == currentHole)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Missing hole " + currentHole + " for scorecard " + scorecard.getId()));

                    if (hole.getNetStrokes() == null || hole.getNetStrokes() == 0) {
                        throw new IllegalStateException("Incomplete net score for hole " + currentHole + " player " + teamPlayer.getPlayer().getId());
                    }

                    netScores.add(hole.getNetStrokes());
                }

                netScores.sort(Comparator.naturalOrder());

                int holeScore = netScores.stream().limit(3).mapToInt(Integer::intValue).sum();
                total += holeScore;

                HoleGameResultResponse holeResult = new HoleGameResultResponse();
                holeResult.setHoleNumber(holeNumber);
                holeResult.setTeamScore(holeScore);
                holeResult.setDetail("Best 3 net scores: " + netScores.get(0) + ", " + netScores.get(1) + ", " + netScores.get(2));

                holeResults.add(holeResult);
            }

            teamResult.setTotalScore(total);
            teamResult.setHoles(holeResults);

            results.add(teamResult);
        }

        results.sort(Comparator.comparing(TeamGameResultResponse::getTotalScore));

        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        GameScoreResponse response = new GameScoreResponse();
        response.setRoundId(roundId);
        response.setFormat(RoundFormat.THREE_LOW_NET.name());
        response.setTeams(results);

        return response;
    }
}
