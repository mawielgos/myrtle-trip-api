package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.exceptionmodel.service.RoundTeamExceptionService;
import com.myrtletrip.scoreentry.entity.Scorecard;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoundTeamReadinessService {

    private final RoundEventCapabilityService roundEventCapabilityService;
    private final RoundTeamExceptionService roundTeamExceptionService;
    private final RoundScoreReadinessService roundScoreReadinessService;

    public RoundTeamReadinessService(
            RoundEventCapabilityService roundEventCapabilityService,
            RoundTeamExceptionService roundTeamExceptionService,
            RoundScoreReadinessService roundScoreReadinessService
    ) {
        this.roundEventCapabilityService = roundEventCapabilityService;
        this.roundTeamExceptionService = roundTeamExceptionService;
        this.roundScoreReadinessService = roundScoreReadinessService;
    }

    public boolean calculateTeamsReady(
            Round round,
            List<Scorecard> scorecards,
            List<RoundTeam> teams,
            List<String> blockingIssues,
            List<String> warnings
    ) {
        RoundEventCapabilityService.RoundEventCapabilities capabilities = roundEventCapabilityService.getCapabilities(round);

        if (!capabilities.requiresTeams()) {
            return true;
        }

        if (scorecards == null || scorecards.isEmpty()) {
            return false;
        }

        if (capabilities.hasTwoManLowNetEvent()) {
            boolean twoManTeamsReady = !calculateNeedsTeams(round, scorecards, teams);
            if (!twoManTeamsReady) {
                blockingIssues.add("2-Man Low Net teams are incomplete. Every team must have exactly 2 players and every player must be assigned to a team.");
            }
            return twoManTeamsReady;
        }

        int expectedTeamSize = resolveExpectedTeamSize(round);
        if (roundTeamExceptionService.hasDuplicateGhostAssignments(round.getId())) {
            blockingIssues.add("A player can only be used as a ghost player for one team in this round.");
            return false;
        }

        boolean teamsReady = !calculateNeedsTeams(round, scorecards, teams);
        if (!teamsReady) {
            blockingIssues.add("Competition teams are incomplete. For this format, each team must contain " + expectedTeamSize + " players, or a valid short-team exception must be configured.");
        } else {
            addTeamExceptionWarnings(round, teams, expectedTeamSize, warnings);
        }
        return teamsReady;
    }

    private int resolveExpectedTeamSize(Round round) {
        int size = roundEventCapabilityService.expectedTeamSize(round);
        return size < 1 ? 4 : size;
    }

    private boolean isAllowedShortTeamException(Round round, Long teamId, int teamSize, int expectedTeamSize) {
        if (round == null || round.getId() == null || teamId == null) {
            return false;
        }

        RoundEventCapabilityService.RoundEventCapabilities capabilities = roundEventCapabilityService.getCapabilities(round);

        if (expectedTeamSize == 4 && teamSize == 3 && capabilities.hasScrambleEvent()) {
            return roundTeamExceptionService.findActiveExtraShotRotation(round.getId(), teamId).isPresent();
        }

        if (expectedTeamSize == 4 && teamSize == 3 && capabilities.hasTeamEvent() && !capabilities.hasScrambleEvent()) {
            return roundTeamExceptionService.findActiveGhostException(round.getId(), teamId).isPresent();
        }

        return false;
    }

    private void addTeamExceptionWarnings(Round round, List<RoundTeam> teams, int expectedTeamSize, List<String> warnings) {
        if (round == null || round.getId() == null || teams == null || warnings == null) {
            return;
        }

        RoundEventCapabilityService.RoundEventCapabilities capabilities = roundEventCapabilityService.getCapabilities(round);
        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null) {
                continue;
            }
            int teamSize = roundTeamExceptionService.countTeamPlayers(team.getId());
            String teamLabel = team.getTeamName() == null || team.getTeamName().isBlank()
                    ? "Team " + team.getTeamNumber()
                    : team.getTeamName();

            if (expectedTeamSize == 4 && teamSize == 3 && capabilities.hasScrambleEvent()
                    && roundTeamExceptionService.findActiveExtraShotRotation(round.getId(), team.getId()).isPresent()) {
                warnings.add(teamLabel + " has 3 players. Extra-shot rotation is configured for this scramble team.");
            }

            if (expectedTeamSize == 4 && teamSize == 3 && capabilities.hasTeamEvent() && !capabilities.hasScrambleEvent()
                    && roundTeamExceptionService.findActiveGhostException(round.getId(), team.getId()).isPresent()) {
                warnings.add(teamLabel + " has 3 players. A ghost player is configured for team scoring only.");
            }
        }
    }

    private boolean calculateNeedsTeams(Round round, List<Scorecard> scorecards, List<RoundTeam> teams) {
        RoundEventCapabilityService.RoundEventCapabilities capabilities = roundEventCapabilityService.getCapabilities(round);

        if (!capabilities.requiresTeams()) {
            return false;
        }

        if (scorecards == null || scorecards.isEmpty()) {
            return true;
        }

        if (teams == null || teams.isEmpty()) {
            return true;
        }

        int expectedTeamSize = resolveExpectedTeamSize(round);
        Map<Long, Integer> teamCounts = new HashMap<>();
        Set<Long> knownTeamIds = new HashSet<>();
        int activeScorecardCount = 0;

        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null) {
                return true;
            }
            knownTeamIds.add(team.getId());
            teamCounts.put(team.getId(), 0);
        }

        for (Scorecard scorecard : scorecards) {
            if (!roundScoreReadinessService.isActiveScorecard(scorecard)) {
                continue;
            }

            if (scorecard == null || scorecard.getPlayer() == null || scorecard.getPlayer().getId() == null) {
                return true;
            }

            activeScorecardCount++;

            if (scorecard.getTeam() == null || scorecard.getTeam().getId() == null) {
                return true;
            }

            Long teamId = scorecard.getTeam().getId();

            if (!knownTeamIds.contains(teamId)) {
                return true;
            }

            Integer currentCount = teamCounts.get(teamId);
            if (currentCount == null) {
                currentCount = 0;
            }

            teamCounts.put(teamId, currentCount + 1);
        }

        if (activeScorecardCount == 0) {
            return true;
        }

        for (Map.Entry<Long, Integer> entry : teamCounts.entrySet()) {
            Integer teamSize = entry.getValue();
            if (teamSize == null) {
                return true;
            }

            if (teamSize == 0) {
                continue;
            }

            if (teamSize == expectedTeamSize) {
                continue;
            }

            if (!isAllowedShortTeamException(round, entry.getKey(), teamSize, expectedTeamSize)) {
                return true;
            }
        }

        return false;
    }
}
