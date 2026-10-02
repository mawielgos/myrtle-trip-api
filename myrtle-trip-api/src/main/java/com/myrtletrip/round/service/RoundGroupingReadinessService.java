package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.scoreentry.entity.Scorecard;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoundGroupingReadinessService {

    private final RoundEventCapabilityService roundEventCapabilityService;
    private final RoundScoreReadinessService roundScoreReadinessService;

    public RoundGroupingReadinessService(
            RoundEventCapabilityService roundEventCapabilityService,
            RoundScoreReadinessService roundScoreReadinessService
    ) {
        this.roundEventCapabilityService = roundEventCapabilityService;
        this.roundScoreReadinessService = roundScoreReadinessService;
    }

    public boolean calculateGroupsReady(
            Round round,
            List<Scorecard> scorecards,
            List<RoundGroup> groups,
            List<RoundTeam> teams,
            List<String> blockingIssues
    ) {
        if (scorecards == null || scorecards.isEmpty()) {
            return false;
        }

        RoundEventCapabilityService.RoundEventCapabilities capabilities = roundEventCapabilityService.getCapabilities(round);
        if (capabilities.hasTwoManLowNetEvent()) {
            boolean twoManTeamsReady = twoManTeamPairsReady(scorecards, teams);
            boolean twoManGroupsReady = twoManTeamsReady && !calculateNeedsGrouping(scorecards, groups);
            boolean twoManTeeTimesReady = twoManGroupsReady && allGroupsHaveTeeTimeData(groups);

            if (!twoManTeamsReady) {
                blockingIssues.add("2-Man Low Net tee-sheet groups are not ready. Assign all players to complete 2-man teams; teams 1+2, 3+4, etc. form tee-sheet groups.");
            } else if (!twoManGroupsReady) {
                blockingIssues.add("2-Man Low Net derived tee-sheet groups have not been generated yet. Open Set Tee Times / Derived Groups before scoring.");
            } else if (!twoManTeeTimesReady) {
                blockingIssues.add("2-Man Low Net tee times are missing. Open Set Tee Times / Derived Groups and generate tee times before scoring.");
            }

            return twoManTeeTimesReady;
        }

        if (capabilities.hasScrambleEvent()) {
            boolean groupingValid = !calculateNeedsGrouping(scorecards, groups);
            if (!groupingValid) {
                blockingIssues.add("Scramble tee-sheet groups are incomplete. Save Scramble teams to auto-create one tee-sheet group per Scramble team.");
            }
            return groupingValid;
        }

        boolean groupingValid = !calculateNeedsGrouping(scorecards, groups);
        if (!groupingValid) {
            blockingIssues.add("Tee-sheet groups are incomplete. Every player must be assigned to exactly one group, with no more than 4 players per group.");
        }
        return groupingValid;
    }

    private boolean allGroupsHaveTeeTimeData(List<RoundGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return false;
        }

        for (RoundGroup group : groups) {
            if (group == null || group.getTeeTime() == null || group.getStartingHole() == null) {
                return false;
            }
        }

        return true;
    }

    private boolean twoManTeamPairsReady(List<Scorecard> scorecards, List<RoundTeam> teams) {
        if (scorecards == null || scorecards.isEmpty()) {
            return false;
        }

        if (teams == null || teams.isEmpty()) {
            return false;
        }

        Map<Long, Integer> playerCountsByTeamId = new HashMap<>();
        Map<Long, Integer> teamNumbersById = new HashMap<>();
        int activeScorecardCount = 0;

        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null || team.getTeamNumber() == null) {
                return false;
            }
            playerCountsByTeamId.put(team.getId(), 0);
            teamNumbersById.put(team.getId(), team.getTeamNumber());
        }

        for (Scorecard scorecard : scorecards) {
            if (!roundScoreReadinessService.isActiveScorecard(scorecard)) {
                continue;
            }
            activeScorecardCount++;
            if (scorecard == null || scorecard.getTeam() == null || scorecard.getTeam().getId() == null) {
                return false;
            }

            Long teamId = scorecard.getTeam().getId();
            Integer currentCount = playerCountsByTeamId.get(teamId);
            if (currentCount == null) {
                return false;
            }
            playerCountsByTeamId.put(teamId, currentCount + 1);
        }

        if (activeScorecardCount == 0) {
            return false;
        }

        Map<Integer, Integer> groupPlayerCounts = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : playerCountsByTeamId.entrySet()) {
            Integer teamPlayerCount = entry.getValue();
            if (teamPlayerCount == null || teamPlayerCount != 2) {
                return false;
            }

            Integer teamNumber = teamNumbersById.get(entry.getKey());
            if (teamNumber == null || teamNumber < 1) {
                return false;
            }

            int groupNumber = ((teamNumber - 1) / 2) + 1;
            Integer groupCount = groupPlayerCounts.get(groupNumber);
            if (groupCount == null) {
                groupCount = 0;
            }
            groupPlayerCounts.put(groupNumber, groupCount + teamPlayerCount);
        }

        for (Map.Entry<Integer, Integer> entry : groupPlayerCounts.entrySet()) {
            Integer groupCount = entry.getValue();
            if (groupCount == null || groupCount != 4) {
                return false;
            }
        }

        return true;
    }

    private boolean calculateNeedsGrouping(List<Scorecard> scorecards, List<RoundGroup> groups) {
        if (scorecards == null || scorecards.isEmpty()) {
            return true;
        }

        if (groups == null || groups.isEmpty()) {
            return true;
        }

        Set<Long> roundPlayerIds = new HashSet<>();

        for (Scorecard scorecard : scorecards) {
            if (!roundScoreReadinessService.isActiveScorecard(scorecard)) {
                continue;
            }
            if (scorecard == null || scorecard.getPlayer() == null || scorecard.getPlayer().getId() == null) {
                continue;
            }
            roundPlayerIds.add(scorecard.getPlayer().getId());
        }

        if (roundPlayerIds.isEmpty()) {
            return true;
        }

        Set<Long> groupedPlayerIds = new HashSet<>();

        for (RoundGroup group : groups) {
            List<RoundGroupPlayer> players = group.getPlayers();

            if (players == null || players.isEmpty()) {
                return true;
            }

            if (players.size() > 4) {
                return true;
            }

            for (RoundGroupPlayer groupPlayer : players) {
                if (groupPlayer == null || groupPlayer.getPlayer() == null || groupPlayer.getPlayer().getId() == null) {
                    return true;
                }

                Long playerId = groupPlayer.getPlayer().getId();

                if (!roundPlayerIds.contains(playerId)) {
                    return true;
                }

                if (!groupedPlayerIds.add(playerId)) {
                    return true;
                }
            }
        }

        return groupedPlayerIds.size() != roundPlayerIds.size();
    }
}
