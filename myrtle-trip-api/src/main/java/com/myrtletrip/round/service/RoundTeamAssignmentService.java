package com.myrtletrip.round.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.RoundTeamPlayerResponse;
import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RoundTeamAssignmentService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final ScorecardRepository scorecardRepository;

    public RoundTeamAssignmentService(RoundRepository roundRepository,
                                      RoundTeamRepository roundTeamRepository,
                                      RoundTeamPlayerRepository roundTeamPlayerRepository,
                                      ScorecardRepository scorecardRepository) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
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

            List<RoundTeamPlayerResponse> players = roundTeamPlayerRepository
                    .findByRoundTeam_IdOrderByPlayerOrderAsc(team.getId())
                    .stream()
                    .map(roundTeamPlayer -> mapAssignedPlayer(roundTeamPlayer, roundId, round))
                    .toList();

            teamResponse.setPlayers(players);
            teamResponses.add(teamResponse);
        }

        scorecards.stream()
                .filter(scorecard -> scorecard.getTeam() == null || scorecard.getTeam().getId() == null)
                .sorted(Comparator.comparing(scorecard -> buildPlayerName(scorecard.getPlayer()), String.CASE_INSENSITIVE_ORDER))
                .map(scorecard -> mapUnassignedPlayer(scorecard, round))
                .forEach(unassignedPlayers::add);

        response.setTeams(teamResponses);
        response.setUnassignedPlayers(unassignedPlayers);

        return response;
    }

    private RoundTeamPlayerResponse mapAssignedPlayer(RoundTeamPlayer roundTeamPlayer, Long roundId, Round round) {
        Player player = roundTeamPlayer.getPlayer();

        RoundTeamPlayerResponse response = new RoundTeamPlayerResponse();
        response.setPlayerId(player.getId());
        response.setPlayerName(buildPlayerName(player));
        response.setPlayerOrder(roundTeamPlayer.getPlayerOrder());

        Optional<Scorecard> scorecardOpt =
                scorecardRepository.findByRound_IdAndPlayer_Id(roundId, player.getId());

        scorecardOpt.ifPresent(scorecard -> {
            response.setScorecardId(scorecard.getId());
            response.setUseAlternateTee(isUsingAlternateTee(scorecard, round));
        });

        return response;
    }

    private RoundTeamPlayerResponse mapUnassignedPlayer(Scorecard scorecard, Round round) {
        Player player = scorecard.getPlayer();

        RoundTeamPlayerResponse response = new RoundTeamPlayerResponse();
        response.setScorecardId(scorecard.getId());
        response.setPlayerId(player.getId());
        response.setPlayerName(buildPlayerName(player));
        response.setPlayerOrder(null);
        response.setUseAlternateTee(isUsingAlternateTee(scorecard, round));

        return response;
    }

    private Boolean isUsingAlternateTee(Scorecard scorecard, Round round) {
        if (scorecard == null || scorecard.getRoundTee() == null) {
            return false;
        }

        RoundTee alternateRoundTee = round.getAlternateRoundTee();
        if (alternateRoundTee == null || alternateRoundTee.getId() == null) {
            return false;
        }

        return alternateRoundTee.getId().equals(scorecard.getRoundTee().getId());
    }

    private String buildPlayerName(Player player) {
        String firstName = player.getFirstName() == null ? "" : player.getFirstName().trim();
        String lastName = player.getLastName() == null ? "" : player.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }
}