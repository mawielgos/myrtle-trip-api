package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundGroupPageResponse;
import com.myrtletrip.round.dto.RoundGroupPlayerResponse;
import com.myrtletrip.round.dto.RoundGroupResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class RoundGroupReadModelService {

    private final RoundRepository roundRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundTeeProvisioningService roundTeeProvisioningService;
    private final RoundGroupAutoAssignmentService roundGroupAutoAssignmentService;
    private final RoundEventCapabilityService roundEventCapabilityService;

    public RoundGroupReadModelService(
            RoundRepository roundRepository,
            RoundGroupRepository roundGroupRepository,
            ScorecardRepository scorecardRepository,
            RoundTeeProvisioningService roundTeeProvisioningService,
            RoundGroupAutoAssignmentService roundGroupAutoAssignmentService,
            RoundEventCapabilityService roundEventCapabilityService
    ) {
        this.roundRepository = roundRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundTeeProvisioningService = roundTeeProvisioningService;
        this.roundGroupAutoAssignmentService = roundGroupAutoAssignmentService;
        this.roundEventCapabilityService = roundEventCapabilityService;
    }

    public RoundGroupPageResponse getRoundGroups(Long roundId) {
        Round round = getRoundOrThrow(roundId);

        roundTeeProvisioningService.ensureRoundTeeOptions(round);

        List<RoundGroup> groups;
        if (roundEventCapabilityService.requiresTeams(round)) {
            if (!Boolean.TRUE.equals(round.getFinalized())) {
                roundGroupAutoAssignmentService.syncGroupsFromTeamsIfNeeded(roundId);
            }
            groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);
        } else {
            groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);
            if (!Boolean.TRUE.equals(round.getFinalized())) {
                groups = ensureEnoughGroups(round, groups);
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

    private List<RoundGroup> ensureEnoughGroups(Round round, List<RoundGroup> existingGroups) {
        int requiredGroupCount = calculateRequiredGroupCount(round);
        if (requiredGroupCount <= 0) {
            return existingGroups;
        }

        Map<Integer, RoundGroup> existingByGroupNumber = new HashMap<>();
        for (RoundGroup group : existingGroups) {
            existingByGroupNumber.put(group.getGroupNumber(), group);
        }

        List<RoundGroup> groupsToCreate = new ArrayList<>();
        for (int i = 1; i <= requiredGroupCount; i++) {
            if (!existingByGroupNumber.containsKey(i)) {
                RoundGroup group = new RoundGroup();
                group.setRound(round);
                group.setGroupNumber(i);
                groupsToCreate.add(group);
            }
        }

        if (!groupsToCreate.isEmpty()) {
            roundGroupRepository.saveAll(groupsToCreate);
            roundGroupRepository.flush();
        }
        return roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(round.getId());
    }

    private int calculateRequiredGroupCount(Round round) {
        int playerCount = 0;
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(round.getId());
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getParticipationStatus() == null
                    || scorecard.getParticipationStatus() == ScorecardParticipationStatus.ACTIVE) {
                playerCount++;
            }
        }
        return playerCount <= 0 ? 0 : (int) Math.ceil(playerCount / 4.0);
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
        response.setStartingHole(group.getStartingHole());

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
