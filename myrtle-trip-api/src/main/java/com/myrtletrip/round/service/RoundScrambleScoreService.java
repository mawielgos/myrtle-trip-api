package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundScrambleScoreResponse;
import com.myrtletrip.round.dto.RoundScrambleTeamScoreResponse;
import com.myrtletrip.round.dto.SaveRoundScrambleScoresRequest;
import com.myrtletrip.round.dto.SaveRoundScrambleTeamScoreRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.TeamHoleScore;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundScrambleScoreService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final RoundRecalculationOrchestrationService roundRecalculationOrchestrationService;

    public RoundScrambleScoreService(RoundRepository roundRepository,
                                     RoundTeamRepository roundTeamRepository,
                                     TeamHoleScoreRepository teamHoleScoreRepository,
                                     RoundRecalculationOrchestrationService roundRecalculationOrchestrationService) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.roundRecalculationOrchestrationService = roundRecalculationOrchestrationService;
    }

    @Transactional(readOnly = true)
    public RoundScrambleScoreResponse getScrambleScores(Long roundId) {
        Round round = loadScrambleRound(roundId);
        RoundScrambleScoreResponse response = new RoundScrambleScoreResponse();
        response.setRoundId(round.getId());

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
        for (RoundTeam team : teams) {
            RoundScrambleTeamScoreResponse teamResponse = new RoundScrambleTeamScoreResponse();
            teamResponse.setRoundTeamId(team.getId());
            teamResponse.setTeamNumber(team.getTeamNumber());
            teamResponse.setTeamName(resolveTeamName(team));
            teamResponse.setTotalScore(team.getScrambleTotalScore());

            Map<Integer, Integer> scoreByHole = new HashMap<>();
            List<TeamHoleScore> holeScores = teamHoleScoreRepository.findByRoundTeam_IdOrderByHoleNumberAsc(team.getId());
            for (TeamHoleScore holeScore : holeScores) {
                scoreByHole.put(holeScore.getHoleNumber(), holeScore.getStrokes());
            }
            for (int i = 1; i <= 18; i++) {
                teamResponse.getHoles().add(scoreByHole.get(i));
            }

            response.getTeams().add(teamResponse);
        }

        return response;
    }

    @Transactional
    public void saveScrambleScores(Long roundId, SaveRoundScrambleScoresRequest request) {
        Round round = loadScrambleRound(roundId);
        if (request == null || request.getTeams() == null || request.getTeams().isEmpty()) {
            throw new IllegalArgumentException("No scramble team scores supplied");
        }

        Map<Long, RoundTeam> teamById = new HashMap<>();
        List<RoundTeam> roundTeams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(round.getId());
        for (RoundTeam team : roundTeams) {
            teamById.put(team.getId(), team);
        }

        boolean totalMode = "TOTAL".equalsIgnoreCase(request.getEntryMode());

        for (SaveRoundScrambleTeamScoreRequest teamRequest : request.getTeams()) {
            if (teamRequest == null || teamRequest.getRoundTeamId() == null) {
                throw new IllegalArgumentException("roundTeamId is required");
            }

            RoundTeam team = teamById.get(teamRequest.getRoundTeamId());
            if (team == null) {
                throw new IllegalArgumentException("Team does not belong to round " + roundId + ": " + teamRequest.getRoundTeamId());
            }

            if (totalMode) {
                Integer totalScore = teamRequest.getTotalScore();
                validateScore(totalScore, "total score", team.getId());
                team.setScrambleTotalScore(totalScore);
                roundTeamRepository.save(team);
                teamHoleScoreRepository.deleteByRoundTeam_Id(team.getId());
            } else {
                team.setScrambleTotalScore(null);
                roundTeamRepository.save(team);
                if (teamRequest.getHoles() == null || teamRequest.getHoles().size() != 18) {
                    throw new IllegalArgumentException("Exactly 18 scramble hole scores are required for teamId=" + team.getId());
                }
                saveHoleScores(team, teamRequest.getHoles());
            }
        }

        roundRecalculationOrchestrationService.handlePostRoundChange(roundId);
    }

    private void saveHoleScores(RoundTeam team, List<Integer> holes) {
        for (int i = 0; i < 18; i++) {
            int holeNumber = i + 1;
            Integer strokes = holes.get(i);
            validateScore(strokes, "hole " + holeNumber, team.getId());

            TeamHoleScore score = teamHoleScoreRepository
                    .findByRoundTeam_IdAndHoleNumber(team.getId(), holeNumber)
                    .orElseGet(TeamHoleScore::new);
            score.setRoundTeam(team);
            score.setHoleNumber(holeNumber);
            score.setStrokes(strokes);
            teamHoleScoreRepository.save(score);
        }
    }

    private Round loadScrambleRound(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
        if (round.getFormat() != RoundFormat.TEAM_SCRAMBLE) {
            throw new IllegalArgumentException("Round is not a TEAM_SCRAMBLE round: " + roundId);
        }
        return round;
    }

    private void validateScore(Integer score, String label, Long teamId) {
        if (score == null) {
            throw new IllegalArgumentException("Missing scramble " + label + " for teamId=" + teamId);
        }
        if (score < 1 || score > 200) {
            throw new IllegalArgumentException("Invalid scramble " + label + " for teamId=" + teamId + ": " + score);
        }
    }

    private String resolveTeamName(RoundTeam team) {
        if (team.getTeamName() != null && !team.getTeamName().isBlank()) {
            return team.getTeamName();
        }
        return "Team " + team.getTeamNumber();
    }
}
