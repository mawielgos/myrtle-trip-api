package com.myrtletrip.contest.service;

import com.myrtletrip.contest.dto.TwoManBestBallHoleResponse;
import com.myrtletrip.contest.dto.TwoManBestBallTeamResponse;
import com.myrtletrip.contest.entity.Team;
import com.myrtletrip.contest.entity.TeamPlayer;
import com.myrtletrip.contest.repository.TeamPlayerRepository;
import com.myrtletrip.contest.repository.TeamRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ContestService {

    private final TeamRepository teamRepository;
    private final TeamPlayerRepository teamPlayerRepository;
    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;

    public ContestService(TeamRepository teamRepository,
                          TeamPlayerRepository teamPlayerRepository,
                          ScorecardRepository scorecardRepository,
                          HoleScoreRepository holeScoreRepository) {
        this.teamRepository = teamRepository;
        this.teamPlayerRepository = teamPlayerRepository;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
    }

    public List<TwoManBestBallTeamResponse> getTwoManBestBallResults(Long roundId) {
        List<Team> teams = teamRepository.findByRound_IdAndTeamTypeOrderByTeamNumber(
                roundId, "TWO_MAN_BEST_BALL");

        List<TwoManBestBallTeamResponse> results = new ArrayList<>();

        for (Team team : teams) {
            List<TeamPlayer> teamPlayers = teamPlayerRepository.findByTeam_Id(team.getId());
            List<TwoManBestBallHoleResponse> holeResults = new ArrayList<>();
            int total = 0;

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                final int currentHole = holeNumber;

                List<PlayerHoleCandidate> candidates = teamPlayers.stream()
                        .map(tp -> getCandidate(roundId, tp, currentHole))
                        .filter(c -> c.netStrokes() != null)
                        .sorted(Comparator.comparing(PlayerHoleCandidate::netStrokes))
                        .toList();

                if (candidates.isEmpty()) {
                    continue;
                }

                PlayerHoleCandidate best = candidates.get(0);
                boolean tie = candidates.size() > 1 &&
                        best.netStrokes().equals(candidates.get(1).netStrokes());

                TwoManBestBallHoleResponse holeResponse = new TwoManBestBallHoleResponse();
                holeResponse.setHoleNumber(currentHole);
                holeResponse.setBestNetStrokes(best.netStrokes());
                holeResponse.setTie(tie);

                if (!tie) {
                    holeResponse.setWinningPlayerId(best.playerId());
                    holeResponse.setWinningPlayerName(best.playerName());
                }

                total += best.netStrokes();
                holeResults.add(holeResponse);
            }

            TwoManBestBallTeamResponse response = new TwoManBestBallTeamResponse();
            response.setTeamId(team.getId());
            response.setTeamNumber(team.getTeamNumber());
            response.setTeamName(team.getTeamName());
            response.setPlayers(teamPlayers.stream()
                    .map(tp -> tp.getPlayer().getDisplayName())
                    .toList());
            response.setTotalBestBall(total);
            response.setHoles(holeResults);

            results.add(response);
        }

        List<TwoManBestBallTeamResponse> sorted = results.stream()
                .sorted(Comparator.comparing(
                        r -> r.getTotalBestBall() == null ? 9999 : r.getTotalBestBall()
                ))
                .toList();

        int rank = 1;

        for (int i = 0; i < sorted.size(); i++) {
            TwoManBestBallTeamResponse current = sorted.get(i);

            if (i > 0) {
                TwoManBestBallTeamResponse prev = sorted.get(i - 1);

                if (!current.getTotalBestBall().equals(prev.getTotalBestBall())) {
                    rank = i + 1;
                }
            }

            current.setRank(rank);

            boolean tied =
                    (i > 0 && current.getTotalBestBall().equals(sorted.get(i - 1).getTotalBestBall())) ||
                    (i < sorted.size() - 1 && current.getTotalBestBall().equals(sorted.get(i + 1).getTotalBestBall()));

            current.setTied(tied);
        }

        return sorted;
    }
    private PlayerHoleCandidate getCandidate(Long roundId, TeamPlayer teamPlayer, int holeNumber) {
        Scorecard scorecard = scorecardRepository
                .findByRound_IdAndPlayer_Id(roundId, teamPlayer.getPlayer().getId())
                .orElse(null);

        if (scorecard == null) {
            return new PlayerHoleCandidate(null, null, null);
        }

        HoleScore holeScore = holeScoreRepository
                .findByScorecard_IdAndHoleNumber(scorecard.getId(), holeNumber)
                .orElse(null);

        if (holeScore == null || holeScore.getNetStrokes() == null) {
            return new PlayerHoleCandidate(null, null, null);
        }

        return new PlayerHoleCandidate(
                teamPlayer.getPlayer().getId(),
                teamPlayer.getPlayer().getDisplayName(),
                holeScore.getNetStrokes()
        );
    }

    private record PlayerHoleCandidate(Long playerId, String playerName, Integer netStrokes) {
    }
}