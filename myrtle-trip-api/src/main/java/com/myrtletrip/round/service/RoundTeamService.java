package com.myrtletrip.round.service;

import com.myrtletrip.permissions.service.RoundCapabilityService;
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
import com.myrtletrip.round.exceptionmodel.entity.RoundTeamException;
import com.myrtletrip.round.exceptionmodel.repository.RoundTeamExceptionRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final TripEditingGuardService tripEditingGuardService;
    private final RoundCapabilityService roundCapabilityService;
    private final RoundEventCapabilityService roundEventCapabilityService;
    private final RoundTeamExceptionRepository roundTeamExceptionRepository;

    public RoundTeamService(
            RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundTeeRepository roundTeeRepository,
            PlayerRepository playerRepository,
            ScorecardRepository scorecardRepository,
            ScorecardHandicapService scorecardHandicapService,
            RoundGroupAutoAssignmentService roundGroupAutoAssignmentService,
            RoundTeeResolver roundTeeResolver,
            TripEditingGuardService tripEditingGuardService,
            RoundCapabilityService roundCapabilityService,
            RoundEventCapabilityService roundEventCapabilityService,
            RoundTeamExceptionRepository roundTeamExceptionRepository
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
        this.tripEditingGuardService = tripEditingGuardService;
        this.roundCapabilityService = roundCapabilityService;
        this.roundEventCapabilityService = roundEventCapabilityService;
        this.roundTeamExceptionRepository = roundTeamExceptionRepository;
    }

    @Transactional
    public List<RoundTeamResponse> saveTeams(Long roundId, SaveRoundTeamsRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        roundCapabilityService.assertCanAssignTeams(round);

        if (!roundEventCapabilityService.requiresTeams(round)) throw new IllegalStateException("This round does not use teams");
        if (request == null || request.getTeams() == null || request.getTeams().isEmpty()) {
            throw new IllegalArgumentException("At least one team is required");
        }

        validateRequest(request, round);

        List<PreservedTeamException> preservedExceptions = capturePreservableTeamExceptions(roundId);

        List<Scorecard> existingScorecards = scorecardRepository.findByRound_Id(roundId);
        for (Scorecard scorecard : existingScorecards) {
            scorecard.setTeam(null);
        }
        scorecardRepository.saveAll(existingScorecards);

        // Team assignments are rebuilt on every save. Short-team exceptions point at
        // round_team rows, so remove them before deleting/recreating teams to avoid
        // foreign-key violations. Valid exceptions are recreated below when the same
        // team number still contains the same players after the save.
        roundTeamExceptionRepository.deleteByRoundIdHard(roundId);
        roundTeamPlayerRepository.deleteByRoundTeam_Round_Id(roundId);
        roundTeamRepository.deleteByRound_Id(roundId);

        Map<Integer, RoundTeam> savedTeamsByNumber = new HashMap<>();
        Map<Integer, List<Long>> savedPlayerIdsByTeamNumber = new HashMap<>();

        for (RoundTeamRequest teamRequest : request.getTeams()) {
            RoundTeam roundTeam = new RoundTeam();
            roundTeam.setRound(round);
            roundTeam.setTeamNumber(teamRequest.getTeamNumber());
            roundTeam.setTeamName(teamRequest.getTeamName());
            roundTeam = roundTeamRepository.save(roundTeam);
            savedTeamsByNumber.put(roundTeam.getTeamNumber(), roundTeam);
            List<Long> savedPlayerIds = new ArrayList<>();
            savedPlayerIdsByTeamNumber.put(roundTeam.getTeamNumber(), savedPlayerIds);

            for (RoundTeamPlayerRequest playerRequest : teamRequest.getPlayers()) {
                Player player = playerRepository.findById(playerRequest.getPlayerId())
                        .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerRequest.getPlayerId()));

                RoundTeamPlayer roundTeamPlayer = new RoundTeamPlayer();
                roundTeamPlayer.setRoundTeam(roundTeam);
                roundTeamPlayer.setPlayer(player);
                roundTeamPlayer.setPlayerOrder(playerRequest.getPlayerOrder());
                roundTeamPlayerRepository.save(roundTeamPlayer);
                savedPlayerIds.add(player.getId());

                Scorecard scorecard = scorecardRepository.findById(playerRequest.getScorecardId())
                        .orElseThrow(() -> new IllegalStateException("Scorecard not found: " + playerRequest.getScorecardId()));

                if (!scorecard.getRound().getId().equals(roundId)) {
                    throw new IllegalStateException("Scorecard " + playerRequest.getScorecardId() + " does not belong to round " + roundId);
                }
                if (!scorecard.getPlayer().getId().equals(player.getId())) {
                    throw new IllegalStateException("Scorecard " + playerRequest.getScorecardId() + " does not belong to player " + player.getId());
                }
                if (scorecard.getParticipationStatus() != null && scorecard.getParticipationStatus() != ScorecardParticipationStatus.ACTIVE) {
                    throw new IllegalStateException("Player " + player.getDisplayName() + " is marked " + scorecard.getParticipationStatus() + " for this round and cannot be assigned to a team.");
                }

                scorecard.setTeam(roundTeam);
                scorecardRepository.save(scorecard);

                Long roundTeeId = playerRequest.getRoundTeeId();
                if (roundTeeId == null && scorecard.getRoundTee() != null) {
                    // Preserve the initialized player-specific default tee. This matters for
                    // female players because their default tee may intentionally differ from
                    // the round-level men's default tee.
                    roundTeeId = scorecard.getRoundTee().getId();
                }
                if (roundTeeId == null) {
                    roundTeeId = round.getDefaultRoundTee() == null ? null : round.getDefaultRoundTee().getId();
                }
                if (roundTeeId != null) {
                    scorecardHandicapService.setScorecardTee(scorecard.getId(), roundTeeId);
                }
            }
        }

        restorePreservedTeamExceptions(round, preservedExceptions, savedTeamsByNumber, savedPlayerIdsByTeamNumber);

        if (roundEventCapabilityService.requiresTeams(round)) {
            roundGroupAutoAssignmentService.syncGroupsFromTeamsIfNeeded(roundId);
        }

        return getTeams(roundId);
    }


    private List<PreservedTeamException> capturePreservableTeamExceptions(Long roundId) {
        List<RoundTeamException> activeExceptions = roundTeamExceptionRepository.findByRound_IdAndActiveTrueOrderByRoundTeam_TeamNumberAscIdAsc(roundId);
        List<PreservedTeamException> snapshots = new ArrayList<>();

        for (RoundTeamException exception : activeExceptions) {
            if (exception.getRoundTeam() == null || exception.getRoundTeam().getTeamNumber() == null) continue;

            List<Long> playerIds = roundTeamPlayerRepository
                    .findByRoundTeam_IdOrderByPlayerOrderAsc(exception.getRoundTeam().getId())
                    .stream()
                    .map(tp -> tp.getPlayer() == null ? null : tp.getPlayer().getId())
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();

            Integer ghostSourceTeamNumber = null;
            if (exception.getGhostSourceTeam() != null) {
                ghostSourceTeamNumber = exception.getGhostSourceTeam().getTeamNumber();
            }

            snapshots.add(new PreservedTeamException(
                    exception.getRoundTeam().getTeamNumber(),
                    playerIds,
                    exception.getExceptionType(),
                    exception.getGhostPlayer() == null ? null : exception.getGhostPlayer().getId(),
                    ghostSourceTeamNumber,
                    exception.getIndexMin(),
                    exception.getIndexMax(),
                    exception.getSelectionMethod(),
                    exception.getRotationPattern(),
                    exception.getNotes()
            ));
        }

        return snapshots;
    }

    private void restorePreservedTeamExceptions(
            Round round,
            List<PreservedTeamException> preservedExceptions,
            Map<Integer, RoundTeam> savedTeamsByNumber,
            Map<Integer, List<Long>> savedPlayerIdsByTeamNumber
    ) {
        if (preservedExceptions == null || preservedExceptions.isEmpty()) return;

        List<RoundTeamException> toSave = new ArrayList<>();
        for (PreservedTeamException snapshot : preservedExceptions) {
            RoundTeam savedTeam = savedTeamsByNumber.get(snapshot.teamNumber());
            if (savedTeam == null) continue;

            List<Long> savedPlayerIds = new ArrayList<>(savedPlayerIdsByTeamNumber.getOrDefault(snapshot.teamNumber(), List.of()));
            savedPlayerIds.sort(Long::compareTo);
            if (!savedPlayerIds.equals(snapshot.sortedPlayerIds())) continue;

            RoundTeamException restored = new RoundTeamException();
            restored.setRound(round);
            restored.setRoundTeam(savedTeam);
            restored.setExceptionType(snapshot.exceptionType());
            restored.setActive(true);
            restored.setIndexMin(snapshot.indexMin());
            restored.setIndexMax(snapshot.indexMax());
            restored.setSelectionMethod(snapshot.selectionMethod());
            restored.setRotationPattern(snapshot.rotationPattern());
            restored.setNotes(snapshot.notes());

            if (snapshot.ghostPlayerId() != null) {
                playerRepository.findById(snapshot.ghostPlayerId()).ifPresent(restored::setGhostPlayer);
            }
            if (snapshot.ghostSourceTeamNumber() != null) {
                restored.setGhostSourceTeam(savedTeamsByNumber.get(snapshot.ghostSourceTeamNumber()));
            }

            toSave.add(restored);
        }

        if (!toSave.isEmpty()) {
            roundTeamExceptionRepository.saveAll(toSave);
        }
    }

    @Transactional(readOnly = true)
    public List<RoundTeamResponse> getTeams(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        if (!roundEventCapabilityService.requiresTeams(round)) return List.of();

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

    private record PreservedTeamException(
            Integer teamNumber,
            List<Long> sortedPlayerIds,
            com.myrtletrip.round.exceptionmodel.entity.RoundTeamExceptionType exceptionType,
            Long ghostPlayerId,
            Integer ghostSourceTeamNumber,
            java.math.BigDecimal indexMin,
            java.math.BigDecimal indexMax,
            com.myrtletrip.round.exceptionmodel.entity.RoundTeamExceptionSelectionMethod selectionMethod,
            String rotationPattern,
            String notes
    ) {}

    private void validateRequest(SaveRoundTeamsRequest request, Round round) {
        Set<Integer> teamNumbers = new HashSet<>();
        Set<Long> playerIds = new HashSet<>();
        int expectedTeamSize = resolveExpectedTeamSize(round);

        for (RoundTeamRequest team : request.getTeams()) {
            if (team.getTeamNumber() == null) throw new IllegalArgumentException("Each team must have a teamNumber");
            if (!teamNumbers.add(team.getTeamNumber())) throw new IllegalArgumentException("Duplicate teamNumber: " + team.getTeamNumber());
            if (team.getPlayers() == null || team.getPlayers().isEmpty()) throw new IllegalArgumentException("Each team must have at least one player");
            if (team.getPlayers().size() > expectedTeamSize) {
                throw new IllegalArgumentException("This round's event setup allows at most " + expectedTeamSize
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

    private int resolveExpectedTeamSize(Round round) {
        int size = roundEventCapabilityService.expectedTeamSize(round);
        return size < 1 ? 4 : size;
    }

    private void applyScorecardTee(RoundTeamPlayerResponse playerResponse, Scorecard scorecard, Round round) {
        playerResponse.setScorecardId(scorecard.getId());
        RoundTee resolved = roundTeeResolver.resolve(scorecard);
        playerResponse.setRoundTeeId(resolved.getId());
        playerResponse.setRoundTeeName(resolved.getTeeName());
        Long defaultId = round.getDefaultRoundTee() == null ? null : round.getDefaultRoundTee().getId();
        playerResponse.setTeeOverride(defaultId != null && resolved.getId() != null && !defaultId.equals(resolved.getId()));
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) return "M";
        return gender.trim().toUpperCase();
    }
}
