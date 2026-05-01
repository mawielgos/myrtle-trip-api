package com.myrtletrip.round.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.round.dto.RoundTeamPlayerRequest;
import com.myrtletrip.round.dto.RoundTeamPlayerResponse;
import com.myrtletrip.round.dto.RoundTeamRequest;
import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.dto.SaveRoundTeamsRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoundTeamService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final PlayerRepository playerRepository;
    private final ScorecardRepository scorecardRepository;
    private final ScorecardHandicapService scorecardHandicapService;
    private final RoundGroupAutoAssignmentService roundGroupAutoAssignmentService;
    private final RoundTeeResolver roundTeeResolver;

    public RoundTeamService(
            RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundTeeRepository roundTeeRepository,
            PlayerRepository playerRepository,
            ScorecardRepository scorecardRepository,
            ScorecardHandicapService scorecardHandicapService,
            RoundGroupAutoAssignmentService roundGroupAutoAssignmentService,
            RoundTeeResolver roundTeeResolver
    ) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.playerRepository = playerRepository;
        this.scorecardRepository = scorecardRepository;
        this.scorecardHandicapService = scorecardHandicapService;
        this.roundGroupAutoAssignmentService = roundGroupAutoAssignmentService;
        this.roundTeeResolver = roundTeeResolver;
    }

    @Transactional
    public List<RoundTeamResponse> saveTeams(Long roundId, SaveRoundTeamsRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        if (Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Round is already finalized");
        }

        RoundFormat format = round.getFormat();
        if (format == null) throw new IllegalStateException("Round format is not set");
        if (!format.requiresTeams()) throw new IllegalStateException("This round format does not use teams");
        if (request == null || request.getTeams() == null || request.getTeams().isEmpty()) {
            throw new IllegalArgumentException("At least one team is required");
        }

        validateRequest(request, format, round);

        List<Scorecard> existingScorecards = scorecardRepository.findByRound_Id(roundId);
        for (Scorecard scorecard : existingScorecards) {
            scorecard.setTeam(null);
        }
        scorecardRepository.saveAll(existingScorecards);

        roundTeamPlayerRepository.deleteByRoundTeam_Round_Id(roundId);
        roundTeamRepository.deleteByRound_Id(roundId);

        for (RoundTeamRequest teamRequest : request.getTeams()) {
            RoundTeam roundTeam = new RoundTeam();
            roundTeam.setRound(round);
            roundTeam.setTeamNumber(teamRequest.getTeamNumber());
            roundTeam.setTeamName(teamRequest.getTeamName());
            roundTeam = roundTeamRepository.save(roundTeam);

            for (RoundTeamPlayerRequest playerRequest : teamRequest.getPlayers()) {
                Player player = playerRepository.findById(playerRequest.getPlayerId())
                        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerRequest.getPlayerId()));

                RoundTeamPlayer roundTeamPlayer = new RoundTeamPlayer();
                roundTeamPlayer.setRoundTeam(roundTeam);
                roundTeamPlayer.setPlayer(player);
                roundTeamPlayer.setPlayerOrder(playerRequest.getPlayerOrder());
                roundTeamPlayerRepository.save(roundTeamPlayer);

                Scorecard scorecard = scorecardRepository.findById(playerRequest.getScorecardId())
                        .orElseThrow(() -> new IllegalStateException("Scorecard not found: " + playerRequest.getScorecardId()));

                if (!scorecard.getRound().getId().equals(roundId)) {
                    throw new IllegalStateException("Scorecard " + playerRequest.getScorecardId() + " does not belong to round " + roundId);
                }
                if (!scorecard.getPlayer().getId().equals(player.getId())) {
                    throw new IllegalStateException("Scorecard " + playerRequest.getScorecardId() + " does not belong to player " + player.getId());
                }

                scorecard.setTeam(roundTeam);
                scorecardRepository.save(scorecard);

                Long roundTeeId = playerRequest.getRoundTeeId();
                if (roundTeeId == null) {
                    roundTeeId = round.getDefaultRoundTee() == null ? null : round.getDefaultRoundTee().getId();
                }
                if (roundTeeId != null) {
                    scorecardHandicapService.setScorecardTee(scorecard.getId(), roundTeeId);
                }
            }
        }

        if (format == RoundFormat.TWO_MAN_LOW_NET) {
            roundGroupAutoAssignmentService.syncGroupsFromTeamsIfNeeded(roundId);
        }

        return getTeams(roundId);
    }

    @Transactional(readOnly = true)
    public List<RoundTeamResponse> getTeams(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        if (round.getFormat() == null) throw new IllegalStateException("Round format is not set");
        if (!round.getFormat().requiresTeams()) return List.of();

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);

        return teams.stream().map(team -> {
            RoundTeamResponse teamResponse = new RoundTeamResponse();
            teamResponse.setRoundTeamId(team.getId());
            teamResponse.setTeamNumber(team.getTeamNumber());
            teamResponse.setTeamName(team.getTeamName());

            List<RoundTeamPlayerResponse> players = roundTeamPlayerRepository
                    .findByRoundTeam_IdOrderByPlayerOrderAsc(team.getId())
                    .stream()
                    .map(tp -> {
                        RoundTeamPlayerResponse playerResponse = new RoundTeamPlayerResponse();
                        playerResponse.setPlayerId(tp.getPlayer().getId());
                        playerResponse.setPlayerName(tp.getPlayer().getDisplayName());
                        playerResponse.setPlayerOrder(tp.getPlayerOrder());
                        playerResponse.setGender(normalizeGender(tp.getPlayer().getGender()));

                        scorecardRepository.findByRound_IdAndPlayer_Id(roundId, tp.getPlayer().getId())
                                .ifPresent(scorecard -> applyScorecardTee(playerResponse, scorecard, round));

                        return playerResponse;
                    })
                    .toList();

            teamResponse.setPlayers(players);
            return teamResponse;
        }).toList();
    }

    private void validateRequest(SaveRoundTeamsRequest request, RoundFormat format, Round round) {
        Set<Integer> teamNumbers = new HashSet<>();
        Set<Long> playerIds = new HashSet<>();
        int expectedTeamSize = format.expectedTeamSize();

        for (RoundTeamRequest team : request.getTeams()) {
            if (team.getTeamNumber() == null) throw new IllegalArgumentException("Each team must have a teamNumber");
            if (!teamNumbers.add(team.getTeamNumber())) throw new IllegalArgumentException("Duplicate teamNumber: " + team.getTeamNumber());
            if (team.getPlayers() == null || team.getPlayers().isEmpty()) throw new IllegalArgumentException("Each team must have at least one player");
            if (team.getPlayers().size() != expectedTeamSize) {
                throw new IllegalArgumentException("Format " + format + " requires exactly " + expectedTeamSize
                        + " players per team. Team " + team.getTeamNumber() + " has " + team.getPlayers().size());
            }

            Set<Integer> playerOrders = new HashSet<>();
            for (RoundTeamPlayerRequest player : team.getPlayers()) {
                if (player.getPlayerId() == null) throw new IllegalArgumentException("Each team player must have a playerId");
                if (player.getScorecardId() == null) throw new IllegalArgumentException("Each team player must have a scorecardId");
                if (!playerIds.add(player.getPlayerId())) throw new IllegalArgumentException("Player assigned more than once: " + player.getPlayerId());
                if (player.getPlayerOrder() == null) throw new IllegalArgumentException("Each team player must have a playerOrder");
                if (!playerOrders.add(player.getPlayerOrder())) {
                    throw new IllegalArgumentException("Duplicate playerOrder " + player.getPlayerOrder() + " in team " + team.getTeamNumber());
                }
                if (player.getRoundTeeId() != null) {
                    RoundTee tee = roundTeeRepository.findById(player.getRoundTeeId())
                            .orElseThrow(() -> new IllegalArgumentException("Round tee not found: " + player.getRoundTeeId()));
                    if (tee.getRound() == null || tee.getRound().getId() == null || !tee.getRound().getId().equals(round.getId())) {
                        throw new IllegalArgumentException("Selected tee does not belong to this round");
                    }
                }
            }
        }
    }

    private void applyScorecardTee(RoundTeamPlayerResponse playerResponse, Scorecard scorecard, Round round) {
        playerResponse.setScorecardId(scorecard.getId());
        RoundTee resolved = roundTeeResolver.resolve(scorecard);
        playerResponse.setRoundTeeId(resolved.getId());
        playerResponse.setRoundTeeName(resolved.getTeeName());
        Long defaultId = round.getDefaultRoundTee() == null ? null : round.getDefaultRoundTee().getId();
        playerResponse.setTeeOverride(defaultId != null && resolved.getId() != null && !defaultId.equals(resolved.getId()));
        playerResponse.setUseAlternateTee(false);
        playerResponse.setStandardTeeEligible(true);
        playerResponse.setAlternateTeeEligible(false);
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) return "M";
        return gender.trim().toUpperCase();
    }
}
