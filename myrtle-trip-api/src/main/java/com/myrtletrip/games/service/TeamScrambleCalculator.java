package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.GameScoreResponse;
import com.myrtletrip.games.dto.HoleGameResultResponse;
import com.myrtletrip.games.dto.TeamGameResultResponse;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.scoreentry.entity.TeamHoleScore;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TeamScrambleCalculator implements RoundGameCalculator {

    private final RoundGameDataService roundGameDataService;
    private final TeamHoleScoreRepository teamHoleScoreRepository;

    public TeamScrambleCalculator(RoundGameDataService roundGameDataService,
                                  TeamHoleScoreRepository teamHoleScoreRepository) {
        this.roundGameDataService = roundGameDataService;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
    }

    @Override
    public RoundFormat getSupportedFormat() {
        return RoundFormat.TEAM_SCRAMBLE;
    }

    @Override
    public GameScoreResponse calculate(Long roundId) {

        List<RoundTeam> teams = roundGameDataService.getTeams(roundId);
        List<TeamGameResultResponse> results = new ArrayList<>();

        if (teams.isEmpty()) {
            throw new IllegalStateException("No teams have been assigned for this round");
        }
        
        for (RoundTeam team : teams) {
            List<TeamHoleScore> holeScores = teamHoleScoreRepository.findByRoundTeam_IdOrderByHoleNumberAsc(team.getId());

            if (holeScores.size() != 18) {
                throw new IllegalStateException("Scramble round requires 18 team hole scores for team " + team.getId());
            }

            TeamGameResultResponse teamResult = new TeamGameResultResponse();
            teamResult.setTeamId(team.getId());
            teamResult.setTeamNumber(team.getTeamNumber());
            teamResult.setTeamName(team.getTeamName());

            List<String> playerNames = roundGameDataService.getTeamPlayers(team.getId()).stream()
                    .map(tp -> tp.getPlayer().getDisplayName())
                    .toList();
            teamResult.setPlayerNames(playerNames);

            int total = 0;
            List<HoleGameResultResponse> holeResults = new ArrayList<>();

            for (TeamHoleScore holeScore : holeScores) {
                total += holeScore.getStrokes();

                HoleGameResultResponse holeResult = new HoleGameResultResponse();
                holeResult.setHoleNumber(holeScore.getHoleNumber());
                holeResult.setTeamScore(holeScore.getStrokes());
                holeResult.setDetail("Team scramble score");

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
        response.setFormat(RoundFormat.TEAM_SCRAMBLE.name());
        response.setTeams(results);

        return response;
    }
}
