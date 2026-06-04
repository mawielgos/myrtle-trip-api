package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.repository.RoundGroupPlayerRepository;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundGroupAutoAssignmentService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundGroupPlayerRepository roundGroupPlayerRepository;
    private final RoundEventCapabilityService roundEventCapabilityService;

    public RoundGroupAutoAssignmentService(
            RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundGroupRepository roundGroupRepository,
            RoundGroupPlayerRepository roundGroupPlayerRepository,
            RoundEventCapabilityService roundEventCapabilityService
    ) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundGroupPlayerRepository = roundGroupPlayerRepository;
        this.roundEventCapabilityService = roundEventCapabilityService;
    }

    @Transactional
    public void syncGroupsFromTeamsIfNeeded(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        RoundEventCapabilityService.RoundEventCapabilities capabilities = roundEventCapabilityService.getCapabilities(round);
        if (!capabilities.requiresTeams()) {
            return;
        }

        if (Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Cannot update groups after round is finalized.");
        }

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
        if (teams == null || teams.isEmpty()) {
            clearExistingGroups(roundId);
            return;
        }

        if (capabilities.hasTwoManLowNetEvent()) {
            syncTwoManGroups(round, roundId, teams);
            return;
        }

        if (capabilities.expectedTeamSize() == 4) {
            syncOneGroupPerTeam(round, roundId, teams);
        }
    }

    private void syncTwoManGroups(Round round, Long roundId, List<RoundTeam> teams) {
        Map<Integer, GroupLogistics> existingLogisticsByGroupNumber = loadExistingLogisticsByGroupNumber(roundId);
        List<RoundTeam> completeTeams = new ArrayList<>();

        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null) {
                continue;
            }

            List<RoundTeamPlayer> teamPlayers =
                    roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(team.getId());

            if (teamPlayers != null && teamPlayers.size() == 2) {
                completeTeams.add(team);
            }
        }

        clearExistingGroups(roundId);

        int groupNumber = 1;

        for (int i = 0; i + 1 < completeTeams.size(); i += 2) {
            RoundTeam firstTeam = completeTeams.get(i);
            RoundTeam secondTeam = completeTeams.get(i + 1);

            List<RoundTeamPlayer> firstTeamPlayers =
                    roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(firstTeam.getId());
            List<RoundTeamPlayer> secondTeamPlayers =
                    roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(secondTeam.getId());

            if (firstTeamPlayers.size() != 2 || secondTeamPlayers.size() != 2) {
                continue;
            }

            RoundGroup group = new RoundGroup();
            group.setRound(round);
            group.setGroupNumber(groupNumber);
            applyExistingLogistics(group, existingLogisticsByGroupNumber.get(groupNumber));

            addGroupPlayer(group, firstTeamPlayers.get(0), 1);
            addGroupPlayer(group, firstTeamPlayers.get(1), 2);
            addGroupPlayer(group, secondTeamPlayers.get(0), 3);
            addGroupPlayer(group, secondTeamPlayers.get(1), 4);

            roundGroupRepository.save(group);
            groupNumber++;
        }
    }

    private void syncOneGroupPerTeam(Round round, Long roundId, List<RoundTeam> teams) {
        Map<Integer, GroupLogistics> existingLogisticsByGroupNumber = loadExistingLogisticsByGroupNumber(roundId);
        clearExistingGroups(roundId);

        int expectedTeamSize = resolveExpectedTeamSize(round);
        int groupNumber = 1;

        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null) {
                continue;
            }

            List<RoundTeamPlayer> teamPlayers =
                    roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(team.getId());

            if (teamPlayers == null || teamPlayers.isEmpty()) {
                continue;
            }

            // Four-player team games use the team as the physical tee-sheet group.
            // A three-player short team is valid for tee-sheet grouping; readiness/scoring
            // separately decide whether a ghost-player or extra-shot exception is required.
            if (teamPlayers.size() > expectedTeamSize) {
                continue;
            }
            if (expectedTeamSize == 4 && teamPlayers.size() < 3) {
                continue;
            }
            if (expectedTeamSize != 4 && teamPlayers.size() != expectedTeamSize) {
                continue;
            }

            RoundGroup group = new RoundGroup();
            group.setRound(round);
            group.setGroupNumber(groupNumber);
            applyExistingLogistics(group, existingLogisticsByGroupNumber.get(groupNumber));

            int seatOrder = 1;
            for (RoundTeamPlayer teamPlayer : teamPlayers) {
                addGroupPlayer(group, teamPlayer, seatOrder);
                seatOrder++;
            }

            roundGroupRepository.save(group);
            groupNumber++;
        }
    }

    private int resolveExpectedTeamSize(Round round) {
        int size = roundEventCapabilityService.expectedTeamSize(round);
        return size < 1 ? 4 : size;
    }

    private Map<Integer, GroupLogistics> loadExistingLogisticsByGroupNumber(Long roundId) {
        Map<Integer, GroupLogistics> result = new HashMap<>();

        List<RoundGroup> existingGroups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);
        if (existingGroups == null || existingGroups.isEmpty()) {
            return result;
        }

        for (RoundGroup existingGroup : existingGroups) {
            if (existingGroup == null || existingGroup.getGroupNumber() == null) {
                continue;
            }

            result.put(
                    existingGroup.getGroupNumber(),
                    new GroupLogistics(existingGroup.getTeeTime(), existingGroup.getStartingHole())
            );
        }

        return result;
    }

    private void applyExistingLogistics(RoundGroup group, GroupLogistics logistics) {
        if (group == null || logistics == null) {
            return;
        }

        group.setTeeTime(logistics.teeTime);
        group.setStartingHole(logistics.startingHole);
    }

    private void addGroupPlayer(RoundGroup group, RoundTeamPlayer teamPlayer, int seatOrder) {
        if (teamPlayer == null || teamPlayer.getPlayer() == null) {
            return;
        }

        RoundGroupPlayer groupPlayer = new RoundGroupPlayer();
        groupPlayer.setPlayer(teamPlayer.getPlayer());
        groupPlayer.setSeatOrder(seatOrder);
        group.addPlayer(groupPlayer);
    }

    private void clearExistingGroups(Long roundId) {
        roundGroupPlayerRepository.deleteByRoundGroup_Round_Id(roundId);
        roundGroupPlayerRepository.flush();

        roundGroupRepository.deleteByRound_Id(roundId);
        roundGroupRepository.flush();
    }

    private static class GroupLogistics {
        private final LocalTime teeTime;
        private final Integer startingHole;

        private GroupLogistics(LocalTime teeTime, Integer startingHole) {
            this.teeTime = teeTime;
            this.startingHole = startingHole;
        }
    }
 }
