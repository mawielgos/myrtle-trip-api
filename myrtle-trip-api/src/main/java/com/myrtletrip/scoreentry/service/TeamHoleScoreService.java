package com.myrtletrip.scoreentry.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.dto.TeamHoleScoreResponse;
import com.myrtletrip.scoreentry.entity.TeamHoleScore;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamHoleScoreService {

    private final RoundTeamRepository roundTeamRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;

    public TeamHoleScoreService(RoundTeamRepository roundTeamRepository,
                                TeamHoleScoreRepository teamHoleScoreRepository) {
        this.roundTeamRepository = roundTeamRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
    }

    @Transactional
    public void updateTeamHoleScore(Long roundTeamId, int holeNumber, int strokes) {

        if (holeNumber < 1 || holeNumber > 18) {
            throw new IllegalArgumentException("Invalid hole number: " + holeNumber);
        }

        if (strokes < 1 || strokes > 15) {
            throw new IllegalArgumentException("Invalid strokes: " + strokes);
        }

        RoundTeam roundTeam = roundTeamRepository.findById(roundTeamId)
                .orElseThrow(() -> new IllegalArgumentException("Round team not found"));

        Round round = roundTeam.getRound();

        if (round.getFormat() != RoundFormat.TEAM_SCRAMBLE) {
            throw new IllegalStateException("Team hole scores are only valid for TEAM_SCRAMBLE rounds");
        }

        if (Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Round is already finalized");
        }

        TeamHoleScore score = teamHoleScoreRepository
                .findByRoundTeam_IdAndHoleNumber(roundTeamId, holeNumber)
                .orElseGet(() -> {
                    TeamHoleScore s = new TeamHoleScore();
                    s.setRoundTeam(roundTeam);
                    s.setHoleNumber(holeNumber);
                    return s;
                });

        score.setStrokes(strokes);
        teamHoleScoreRepository.save(score);
    }

    @Transactional(readOnly = true)
    public List<TeamHoleScoreResponse> getTeamHoleScores(Long roundTeamId) {

        RoundTeam roundTeam = roundTeamRepository.findById(roundTeamId)
                .orElseThrow(() -> new IllegalArgumentException("Round team not found"));

        List<TeamHoleScore> scores = teamHoleScoreRepository.findByRoundTeam_IdOrderByHoleNumberAsc(roundTeamId);

        return scores.stream().map(score -> {
            TeamHoleScoreResponse dto = new TeamHoleScoreResponse();
            dto.setRoundTeamId(roundTeam.getId());
            dto.setTeamNumber(roundTeam.getTeamNumber());
            dto.setTeamName(roundTeam.getTeamName());
            dto.setHoleNumber(score.getHoleNumber());
            dto.setStrokes(score.getStrokes());
            return dto;
        }).toList();
    }
}
