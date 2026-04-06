package com.myrtletrip.round.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.RoundTeamPlayerResponse;
import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoundTeamAssignmentService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final ScorecardRepository scorecardRepository;

    public RoundTeamAssignmentService(RoundRepository roundRepository,
                                      RoundTeamRepository roundTeamRepository,
                                      ScorecardRepository scorecardRepository) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.scorecardRepository = scorecardRepository;
    }

    @Transactional(readOnly = true)
    public RoundTeamAssignmentPageResponse getAssignmentPage(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);

        RoundTeamAssignmentPageResponse response = new RoundTeamAssignmentPageResponse();
        response.setRoundId(round.getId());

        List<RoundTeamResponse> teamResponses = new ArrayList<>();
        List<RoundTeamPlayerResponse> unassignedPlayers = new ArrayList<>();

        for (RoundTeam team : teams) {
            RoundTeamResponse teamResponse = new RoundTeamResponse();
            teamResponse.setRoundTeamId(team.getId());
            teamResponse.setTeamNumber(team.getTeamNumber());
            teamResponse.setTeamName(team.getTeamName());

            List<RoundTeamPlayerResponse> players = new ArrayList<>();

            for (Scorecard scorecard : scorecards) {
                if (scorecard.getTeam() != null
                        && scorecard.getTeam().getId() != null
                        && scorecard.getTeam().getId().equals(team.getId())) {
                    players.add(mapPlayer(scorecard));
                }
            }

            teamResponse.setPlayers(players);
            teamResponses.add(teamResponse);
        }

        for (Scorecard scorecard : scorecards) {
            if (scorecard.getTeam() == null || scorecard.getTeam().getId() == null) {
                unassignedPlayers.add(mapPlayer(scorecard));
            }
        }

        response.setTeams(teamResponses);
        response.setUnassignedPlayers(unassignedPlayers);

        return response;
    }

    private RoundTeamPlayerResponse mapPlayer(Scorecard scorecard) {
        Player player = scorecard.getPlayer();

        RoundTeamPlayerResponse response = new RoundTeamPlayerResponse();
        response.setScorecardId(scorecard.getId());
        response.setPlayerId(player.getId());
        response.setPlayerName(buildPlayerName(player));

        return response;
    }

    private String buildPlayerName(Player player) {
        String firstName = player.getFirstName() == null ? "" : player.getFirstName().trim();
        String lastName = player.getLastName() == null ? "" : player.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }
}
