package com.myrtletrip.round.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.round.dto.RoundGroupAssignmentItemRequest;
import com.myrtletrip.round.dto.RoundGroupAssignmentRequest;
import com.myrtletrip.round.dto.RoundGroupPageResponse;
import com.myrtletrip.round.dto.RoundGroupPlayerResponse;
import com.myrtletrip.round.dto.RoundGroupResponse;
import com.myrtletrip.round.dto.RoundGroupTeeTimeRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalTime;

@Service
@Transactional
public class RoundGroupService {

    private final RoundRepository roundRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final PlayerRepository playerRepository;
    private final ScorecardRepository scorecardRepository;
    private final ScorecardHandicapService scorecardHandicapService;
    private final RoundTeeRepository roundTeeRepository;
    private final RoundTeeProvisioningService roundTeeProvisioningService;
    private final RoundTeamAutoAssignmentService roundTeamAutoAssignmentService;
    private final RoundGroupAutoAssignmentService roundGroupAutoAssignmentService;
    private final TripEditingGuardService tripEditingGuardService;

    public RoundGroupService(
            RoundRepository roundRepository,
            RoundGroupRepository roundGroupRepository,
            PlayerRepository playerRepository,
            ScorecardRepository scorecardRepository,
            ScorecardHandicapService scorecardHandicapService,
            RoundTeeRepository roundTeeRepository,
            RoundTeeProvisioningService roundTeeProvisioningService,
            RoundTeamAutoAssignmentService roundTeamAutoAssignmentService,
            RoundGroupAutoAssignmentService roundGroupAutoAssignmentService,
            TripEditingGuardService tripEditingGuardService
    ) {
        this.roundRepository = roundRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.playerRepository = playerRepository;
        this.scorecardRepository = scorecardRepository;
        this.scorecardHandicapService = scorecardHandicapService;
        this.roundTeeRepository = roundTeeRepository;
        this.roundTeeProvisioningService = roundTeeProvisioningService;
        this.roundTeamAutoAssignmentService = roundTeamAutoAssignmentService;
        this.roundGroupAutoAssignmentService = roundGroupAutoAssignmentService;
        this.tripEditingGuardService = tripEditingGuardService;
    }

    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public RoundGroupPageResponse getRoundGroups(Long roundId) {
        Round round = getRoundOrThrow(roundId);

        // This is a read/view endpoint used by scoring, correction mode, and the round progress bar.
        // Completed trips must still be able to load their existing group data. Structural locking
        // belongs in save/update paths, not read paths.
        roundTeeProvisioningService.ensureRoundTeeOptions(round);

        List<RoundGroup> groups;

        if (round.getFormat() == RoundFormat.TWO_MAN_LOW_NET) {
            if (!Boolean.TRUE.equals(round.getFinalized())) {
                roundGroupAutoAssignmentService.syncGroupsFromTeamsIfNeeded(roundId);
            }
            groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);
        } else {
            groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);

            if (groups.isEmpty() && !Boolean.TRUE.equals(round.getFinalized())) {
                groups = initializeGroups(round);
            }
        }
        RoundGroupPageResponse response = new RoundGroupPageResponse();
        response.setRoundId(round.getId());

        List<RoundGroupResponse> groupResponses = new ArrayList<>();
        for (RoundGroup group : groups) {
            groupResponses.add(toResponse(group));
        }
        response.setGroups(groupResponses);

        return response;
    }

    public RoundGroupPageResponse saveRoundGroups(Long roundId, RoundGroupAssignmentRequest request) {
        Round round = getRoundOrThrow(roundId);
        tripEditingGuardService.assertStructureEditable(round.getTrip());
        roundTeeProvisioningService.ensureRoundTeeOptions(round);
        validateRequest(request, round);

        if (Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Cannot update groups after round is finalized.");
        }

        List<RoundGroup> existingGroups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);
        if (!existingGroups.isEmpty()) {
            roundGroupRepository.deleteAll(existingGroups);
            roundGroupRepository.flush();
        }

        Map<Long, Player> playersById = loadPlayers(request);
        Map<Long, Scorecard> scorecardsById = loadScorecardsForRound(roundId);
        Map<Integer, List<RoundGroupAssignmentItemRequest>> groupedAssignments = new HashMap<>();
        Map<Integer, LocalTime> teeTimesByGroupNumber = buildTeeTimesByGroupNumber(request);

        for (RoundGroupAssignmentItemRequest item : request.getAssignments()) {
            groupedAssignments
                    .computeIfAbsent(item.getGroupNumber(), key -> new ArrayList<>())
                    .add(item);
        }

        List<Integer> sortedGroupNumbers = new ArrayList<>(groupedAssignments.keySet());
        sortedGroupNumbers.sort(Integer::compareTo);

        List<RoundGroup> groupsToSave = new ArrayList<>();

        for (Integer groupNumber : sortedGroupNumbers) {
            RoundGroup roundGroup = new RoundGroup();
            roundGroup.setRound(round);
            roundGroup.setGroupNumber(groupNumber);
            roundGroup.setTeeTime(teeTimesByGroupNumber.get(groupNumber));

            List<RoundGroupAssignmentItemRequest> items = groupedAssignments.get(groupNumber);
            items.sort(Comparator.comparing(RoundGroupAssignmentItemRequest::getSeatOrder));

            for (RoundGroupAssignmentItemRequest item : items) {
                RoundGroupPlayer groupPlayer = new RoundGroupPlayer();
                groupPlayer.setPlayer(playersById.get(item.getPlayerId()));
                groupPlayer.setSeatOrder(item.getSeatOrder());
                roundGroup.addPlayer(groupPlayer);
            }

            groupsToSave.add(roundGroup);
        }

        roundGroupRepository.saveAll(groupsToSave);
        roundGroupRepository.flush();

        applyTeeSelections(round, request, scorecardsById);

        if (round.getFormat() != null && round.getFormat() != RoundFormat.TWO_MAN_LOW_NET) {
            roundTeamAutoAssignmentService.rebuildTeamsFromGroups(roundId);
        }

        return getRoundGroups(roundId);
    }

    private Map<Integer, LocalTime> buildTeeTimesByGroupNumber(RoundGroupAssignmentRequest request) {
        Map<Integer, LocalTime> result = new HashMap<>();

        if (request == null || request.getGroupTeeTimes() == null) {
            return result;
        }

        for (RoundGroupTeeTimeRequest teeTimeRequest : request.getGroupTeeTimes()) {
            if (teeTimeRequest == null || teeTimeRequest.getGroupNumber() == null) {
                continue;
            }

            result.put(teeTimeRequest.getGroupNumber(), teeTimeRequest.getTeeTime());
        }

        return result;
    }

    private void applyTeeSelections(
            Round round,
            RoundGroupAssignmentRequest request,
            Map<Long, Scorecard> scorecardsById
    ) {
        if (request == null || request.getAssignments() == null) {
            return;
        }

        for (RoundGroupAssignmentItemRequest item : request.getAssignments()) {
            Scorecard scorecard = resolveScorecard(round.getId(), item, scorecardsById);
            applyTeeSelection(round, scorecard, item);
        }

        scorecardRepository.flush();
    }

    private void applyTeeSelection(
            Round round,
            Scorecard scorecard,
            RoundGroupAssignmentItemRequest item
    ) {
        RoundTee selectedRoundTee;

        if (item.getRoundTeeId() != null) {
            selectedRoundTee = roundTeeRepository.findById(item.getRoundTeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Round tee not found: " + item.getRoundTeeId()));

            if (selectedRoundTee.getRound() == null || !selectedRoundTee.getRound().getId().equals(round.getId())) {
                throw new IllegalArgumentException(
                        "Round tee " + item.getRoundTeeId() + " does not belong to round " + round.getId() + "."
                );
            }
        } else {
            selectedRoundTee = round.getDefaultRoundTee();
            if (selectedRoundTee == null) {
                selectedRoundTee = round.getStandardRoundTee();
            }
            if (selectedRoundTee == null) {
                throw new IllegalArgumentException(
                        "Round " + round.getId() + " does not have a default tee configured."
                );
            }
        }

        scorecard.setRoundTee(selectedRoundTee);
        scorecardRepository.save(scorecard);
        scorecardHandicapService.refreshHandicaps(scorecard.getId());
    }

    private Scorecard resolveScorecard(
            Long roundId,
            RoundGroupAssignmentItemRequest item,
            Map<Long, Scorecard> scorecardsById
    ) {
        if (item.getScorecardId() != null) {
            Scorecard scorecard = scorecardsById.get(item.getScorecardId());
            if (scorecard == null) {
                throw new IllegalArgumentException(
                        "Scorecard " + item.getScorecardId() + " is not valid for round " + roundId + "."
                );
            }

            if (!scorecard.getPlayer().getId().equals(item.getPlayerId())) {
                throw new IllegalArgumentException(
                        "Scorecard " + item.getScorecardId() + " does not belong to player " + item.getPlayerId() + "."
                );
            }

            return scorecard;
        }

        return scorecardRepository.findByRound_IdAndPlayer_Id(roundId, item.getPlayerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Scorecard not found for round " + roundId + " and player " + item.getPlayerId() + "."
                ));
    }

    private Map<Long, Scorecard> loadScorecardsForRound(Long roundId) {
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
        Map<Long, Scorecard> scorecardsById = new HashMap<>();

        for (Scorecard scorecard : scorecards) {
            scorecardsById.put(scorecard.getId(), scorecard);
        }

        return scorecardsById;
    }

    private List<RoundGroup> initializeGroups(Round round) {
        int playerCount = scorecardRepository.findByRound_Id(round.getId()).size();
        int groupCount = (int) Math.ceil(playerCount / 4.0);

        List<RoundGroup> groups = new ArrayList<>();

        for (int i = 1; i <= groupCount; i++) {
            RoundGroup group = new RoundGroup();
            group.setRound(round);
            group.setGroupNumber(i);
            groups.add(group);
        }

        return roundGroupRepository.saveAll(groups);
    }

    private void validateRequest(RoundGroupAssignmentRequest request, Round round) {
        if (request == null || request.getAssignments() == null) {
            throw new IllegalArgumentException("Assignments are required.");
        }

        List<RoundGroupAssignmentItemRequest> assignments = request.getAssignments();

        validateGroupTeeTimes(request);

        Set<Long> playerIds = new HashSet<>();
        Set<String> groupSeatKeys = new HashSet<>();
        Map<Integer, Integer> countsByGroup = new HashMap<>();

        for (RoundGroupAssignmentItemRequest item : assignments) {
            if (item.getPlayerId() == null) {
                throw new IllegalArgumentException("Each assignment must include playerId.");
            }
            if (item.getGroupNumber() == null || item.getGroupNumber() < 1) {
                throw new IllegalArgumentException("Each assignment must include a valid groupNumber.");
            }
            if (item.getSeatOrder() == null || item.getSeatOrder() < 1 || item.getSeatOrder() > 4) {
                throw new IllegalArgumentException("Each assignment must include seatOrder from 1 to 4.");
            }

            if (!playerIds.add(item.getPlayerId())) {
                throw new IllegalArgumentException("Player " + item.getPlayerId() + " is assigned more than once.");
            }

            String seatKey = item.getGroupNumber() + ":" + item.getSeatOrder();
            if (!groupSeatKeys.add(seatKey)) {
                throw new IllegalArgumentException(
                        "Duplicate seatOrder " + item.getSeatOrder() + " in group " + item.getGroupNumber() + "."
                );
            }

            Integer currentCount = countsByGroup.get(item.getGroupNumber());
            if (currentCount == null) {
                currentCount = 0;
            }
            currentCount = currentCount + 1;
            countsByGroup.put(item.getGroupNumber(), currentCount);

            if (currentCount > 4) {
                throw new IllegalArgumentException("Group " + item.getGroupNumber() + " has more than 4 players.");
            }

            if (item.getScorecardId() != null) {
                Scorecard scorecard = scorecardRepository.findById(item.getScorecardId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Scorecard not found: " + item.getScorecardId()
                        ));

                if (scorecard.getRound() == null || !scorecard.getRound().getId().equals(round.getId())) {
                    throw new IllegalArgumentException(
                            "Scorecard " + item.getScorecardId() + " does not belong to round " + round.getId() + "."
                    );
                }

                if (scorecard.getPlayer() == null || !scorecard.getPlayer().getId().equals(item.getPlayerId())) {
                    throw new IllegalArgumentException(
                            "Scorecard " + item.getScorecardId() + " does not belong to player " + item.getPlayerId() + "."
                    );
                }
            }

        }
    }

    private void validateGroupTeeTimes(RoundGroupAssignmentRequest request) {
        if (request.getGroupTeeTimes() == null) {
            return;
        }

        Set<Integer> groupNumbers = new HashSet<>();

        for (RoundGroupTeeTimeRequest teeTimeRequest : request.getGroupTeeTimes()) {
            if (teeTimeRequest == null) {
                continue;
            }

            Integer groupNumber = teeTimeRequest.getGroupNumber();
            if (groupNumber == null || groupNumber < 1) {
                throw new IllegalArgumentException("Each tee time row must include a valid groupNumber.");
            }

            if (!groupNumbers.add(groupNumber)) {
                throw new IllegalArgumentException("Duplicate tee time row for group " + groupNumber + ".");
            }
        }
    }

    private Map<Long, Player> loadPlayers(RoundGroupAssignmentRequest request) {
        List<Long> playerIds = new ArrayList<>();

        for (RoundGroupAssignmentItemRequest item : request.getAssignments()) {
            if (!playerIds.contains(item.getPlayerId())) {
                playerIds.add(item.getPlayerId());
            }
        }

        List<Player> players = playerRepository.findAllById(playerIds);
        Map<Long, Player> playersById = new HashMap<>();

        for (Player player : players) {
            playersById.put(player.getId(), player);
        }

        for (Long playerId : playerIds) {
            if (!playersById.containsKey(playerId)) {
                throw new IllegalArgumentException("Player not found: " + playerId);
            }
        }

        return playersById;
    }

    private Round getRoundOrThrow(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
    }

    private RoundGroupResponse toResponse(RoundGroup group) {
        RoundGroupResponse response = new RoundGroupResponse();
        response.setGroupId(group.getId());
        response.setGroupNumber(group.getGroupNumber());
        response.setTeeTime(group.getTeeTime());

        List<RoundGroupPlayerResponse> players = new ArrayList<>();
        List<RoundGroupPlayer> sortedPlayers = new ArrayList<>(group.getPlayers());
        sortedPlayers.sort(Comparator.comparing(RoundGroupPlayer::getSeatOrder));

        for (RoundGroupPlayer groupPlayer : sortedPlayers) {
            RoundGroupPlayerResponse playerResponse = new RoundGroupPlayerResponse();
            playerResponse.setPlayerId(groupPlayer.getPlayer().getId());
            playerResponse.setPlayerName(groupPlayer.getPlayer().getDisplayName());
            playerResponse.setSeatOrder(groupPlayer.getSeatOrder());
            players.add(playerResponse);
        }

        response.setPlayers(players);
        return response;
    }
}
