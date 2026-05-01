package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundReadinessResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoundReadinessService {

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeeResolver roundTeeResolver;

    public RoundReadinessService(
            RoundRepository roundRepository,
            ScorecardRepository scorecardRepository,
            RoundGroupRepository roundGroupRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeeResolver roundTeeResolver
    ) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeeResolver = roundTeeResolver;
    }

    @Transactional(readOnly = true)
    public RoundReadinessResponse getReadiness(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Round not found: " + roundId
                ));

        List<String> blockingIssues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        RoundFormat format = round.getFormat();

        boolean roundConfigured = calculateRoundConfigured(round, blockingIssues);

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
        boolean scorecardsReady = scorecards != null && !scorecards.isEmpty();
        if (!scorecardsReady) {
            blockingIssues.add("No scorecards exist for this round.");
        }

        boolean teesReady = scorecardsReady && allScorecardsHaveTee(scorecards);
        if (scorecardsReady && !teesReady) {
            blockingIssues.add("One or more players does not have a tee selected.");
        }

        boolean handicapsReady = scorecardsReady && calculateHandicapsReady(format, scorecards);
        if (scorecardsReady && !handicapsReady) {
            blockingIssues.add("One or more scorecards is missing calculated handicap values.");
        }

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
        boolean teamsReady = calculateTeamsReady(round, scorecards, teams, blockingIssues);

        List<RoundGroup> groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);
        boolean groupsReady = calculateGroupsReady(round, scorecards, groups, teams, blockingIssues);

        boolean finalized = Boolean.TRUE.equals(round.getFinalized());
        if (finalized) {
            warnings.add("This round is already finalized. Corrections should go through the correction/recalculation flow.");
        }

        boolean ready = roundConfigured
                && scorecardsReady
                && teesReady
                && handicapsReady
                && groupsReady
                && teamsReady;

        RoundReadinessResponse response = new RoundReadinessResponse();
        response.setRoundId(roundId);
        response.setRoundNumber(round.getRoundNumber());
        response.setRoundFormat(format == null ? null : format.name());
        response.setFinalized(finalized);
        response.setRoundConfigured(roundConfigured);
        response.setScorecardsReady(scorecardsReady);
        response.setTeesReady(teesReady);
        response.setHandicapsReady(handicapsReady);
        response.setGroupsReady(groupsReady);
        response.setTeamsReady(teamsReady);
        response.setReadyForScoring(ready);
        response.setReady(ready);
        response.setScorecardCount(scorecards == null ? 0 : scorecards.size());
        response.setGroupCount(groups == null ? 0 : groups.size());
        response.setTeamCount(teams == null ? 0 : teams.size());
        response.setBlockingIssues(blockingIssues);
        response.setWarnings(warnings);

        return response;
    }

    private boolean calculateRoundConfigured(Round round, List<String> blockingIssues) {
        boolean configured = true;

        if (round.getTrip() == null || round.getTrip().getId() == null) {
            blockingIssues.add("Round is not linked to a trip.");
            configured = false;
        }

        if (round.getRoundNumber() == null) {
            blockingIssues.add("Round number is missing.");
            configured = false;
        }

        if (round.getRoundDate() == null) {
            blockingIssues.add("Round date is missing.");
            configured = false;
        }

        if (round.getFormat() == null) {
            blockingIssues.add("Round format is missing.");
            configured = false;
        }

        if (round.getCourse() == null || round.getCourse().getId() == null) {
            blockingIssues.add("Course is missing.");
            configured = false;
        }

        if (round.getDefaultRoundTee() == null || round.getDefaultRoundTee().getId() == null) {
            blockingIssues.add("Default tee is missing.");
            configured = false;
        }

        if (round.getHandicapPercent() == null) {
            blockingIssues.add("Handicap percent is missing.");
            configured = false;
        }

        return configured;
    }

    private boolean allScorecardsHaveTee(List<Scorecard> scorecards) {
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null) {
                return false;
            }
            try {
                RoundTee effectiveRoundTee = roundTeeResolver.resolve(scorecard);
                if (effectiveRoundTee == null || effectiveRoundTee.getId() == null) {
                    return false;
                }
            } catch (RuntimeException ex) {
                return false;
            }
        }
        return true;
    }

    private boolean calculateHandicapsReady(RoundFormat format, List<Scorecard> scorecards) {
        if (format == RoundFormat.TEAM_SCRAMBLE) {
            return true;
        }

        for (Scorecard scorecard : scorecards) {
            if (scorecard == null) {
                return false;
            }
            if (scorecard.getCourseHandicap() == null || scorecard.getPlayingHandicap() == null) {
                return false;
            }
        }

        return true;
    }

    private boolean calculateGroupsReady(
            Round round,
            List<Scorecard> scorecards,
            List<RoundGroup> groups,
            List<RoundTeam> teams,
            List<String> blockingIssues
    ) {
        if (scorecards == null || scorecards.isEmpty()) {
            return false;
        }

        RoundFormat format = round.getFormat();
        if (format == RoundFormat.TWO_MAN_LOW_NET) {
            boolean twoManGroupsReady = twoManTeamPairsReady(scorecards, teams);
            if (!twoManGroupsReady) {
                blockingIssues.add("2-Man Low Net tee-sheet groups are not ready. Assign all players to complete 2-man teams; teams 1+2, 3+4, etc. form tee-sheet groups.");
            }
            return twoManGroupsReady;
        }

        boolean groupingValid = !calculateNeedsGrouping(scorecards, groups);
        if (!groupingValid) {
            blockingIssues.add("Tee-sheet groups are incomplete. Every player must be assigned to exactly one group, with no more than 4 players per group.");
        }
        return groupingValid;
    }

    private boolean calculateTeamsReady(
            Round round,
            List<Scorecard> scorecards,
            List<RoundTeam> teams,
            List<String> blockingIssues
    ) {
        RoundFormat format = round.getFormat();

        if (format == null || !format.requiresTeams()) {
            return true;
        }

        if (scorecards == null || scorecards.isEmpty()) {
            return false;
        }

        if (format == RoundFormat.TWO_MAN_LOW_NET) {
            boolean twoManTeamsReady = !calculateNeedsTeams(round, scorecards, teams);
            if (!twoManTeamsReady) {
                blockingIssues.add("2-Man Low Net teams are incomplete. Every team must have exactly 2 players and every player must be assigned to a team.");
            }
            return twoManTeamsReady;
        }

        boolean fourManTeamsReady = !calculateNeedsTeams(round, scorecards, teams);
        if (!fourManTeamsReady) {
            blockingIssues.add("Competition teams are incomplete. For this format, each tee-sheet group is also the competition team and must contain 4 players.");
        }
        return fourManTeamsReady;
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

        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null || team.getTeamNumber() == null) {
                return false;
            }
            playerCountsByTeamId.put(team.getId(), 0);
            teamNumbersById.put(team.getId(), team.getTeamNumber());
        }

        for (Scorecard scorecard : scorecards) {
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

    private boolean calculateNeedsTeams(Round round, List<Scorecard> scorecards, List<RoundTeam> teams) {
        RoundFormat format = round.getFormat();

        if (format == null || !format.requiresTeams()) {
            return false;
        }

        if (scorecards == null || scorecards.isEmpty()) {
            return true;
        }

        if (teams == null || teams.isEmpty()) {
            return true;
        }

        int expectedTeamSize = format.expectedTeamSize();
        Map<Long, Integer> teamCounts = new HashMap<>();
        Set<Long> knownTeamIds = new HashSet<>();

        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null) {
                return true;
            }
            knownTeamIds.add(team.getId());
            teamCounts.put(team.getId(), 0);
        }

        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getPlayer() == null || scorecard.getPlayer().getId() == null) {
                return true;
            }

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

        for (Map.Entry<Long, Integer> entry : teamCounts.entrySet()) {
            Integer teamSize = entry.getValue();
            if (teamSize == null || teamSize != expectedTeamSize) {
                return true;
            }
        }

        return false;
    }
}
