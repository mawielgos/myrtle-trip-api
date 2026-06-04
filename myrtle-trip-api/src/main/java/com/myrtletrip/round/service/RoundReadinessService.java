package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundReadinessResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.exceptionmodel.service.RoundTeamExceptionService;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.entity.TeamHoleScore;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
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
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final RoundTeeResolver roundTeeResolver;
    private final RoundEventCapabilityService roundEventCapabilityService;
    private final RoundTeamExceptionService roundTeamExceptionService;

    public RoundReadinessService(
            RoundRepository roundRepository,
            ScorecardRepository scorecardRepository,
            RoundGroupRepository roundGroupRepository,
            RoundTeamRepository roundTeamRepository,
            TeamHoleScoreRepository teamHoleScoreRepository,
            RoundTeeResolver roundTeeResolver,
            RoundEventCapabilityService roundEventCapabilityService,
            RoundTeamExceptionService roundTeamExceptionService
    ) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.roundTeeResolver = roundTeeResolver;
        this.roundEventCapabilityService = roundEventCapabilityService;
        this.roundTeamExceptionService = roundTeamExceptionService;
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

        RoundEventCapabilityService.RoundEventCapabilities eventCapabilities =
                roundEventCapabilityService.getCapabilities(round);

        boolean roundConfigured = calculateRoundConfigured(round, blockingIssues);

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId).stream()
                .filter(this::isActiveScorecard)
                .toList();
        boolean scorecardsReady = scorecards != null && !scorecards.isEmpty();
        if (!scorecardsReady) {
            blockingIssues.add("No scorecards exist for this round.");
        }

        boolean teesReady = scorecardsReady && allScorecardsHaveTee(scorecards);
        if (scorecardsReady && !teesReady) {
            blockingIssues.add("One or more players does not have a tee selected.");
        }

        boolean handicapsReady = scorecardsReady && calculateHandicapsReady(round, scorecards);
        if (scorecardsReady && !handicapsReady) {
            blockingIssues.add("One or more scorecards is missing calculated handicap values.");
        }

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
        boolean teamsReady = calculateTeamsReady(round, scorecards, teams, blockingIssues, warnings);

        List<RoundGroup> groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);
        boolean groupsReady = calculateGroupsReady(round, scorecards, groups, teams, blockingIssues, warnings);

        boolean finalized = Boolean.TRUE.equals(round.getFinalized());
        if (finalized) {
            warnings.add("This round is already finalized. Corrections should go through the correction/recalculation flow.");
        }

        boolean readyForScoring = roundConfigured
                && scorecardsReady
                && teesReady
                && handicapsReady
                && groupsReady
                && teamsReady;

        int missingScoreCount = calculateMissingScoreCount(round, scorecards, teams);
        boolean scoreEntryComplete = missingScoreCount == 0;
        if (readyForScoring && !scoreEntryComplete && !finalized) {
            warnings.add("Round has " + missingScoreCount + " missing score entr" + (missingScoreCount == 1 ? "y" : "ies") + ". Enter scores before finalizing the round.");
        }

        boolean readyForFinalization = readyForScoring && scoreEntryComplete && !finalized;
        boolean ready = readyForScoring;

        RoundReadinessResponse response = new RoundReadinessResponse();
        response.setRoundId(roundId);
        response.setRoundNumber(round.getRoundNumber());
        response.setRoundFormat(round.getFormat() == null ? null : round.getFormat().name());
        response.setFinalized(finalized);
        response.setRoundConfigured(roundConfigured);
        response.setScorecardsReady(scorecardsReady);
        response.setTeesReady(teesReady);
        response.setHandicapsReady(handicapsReady);
        response.setGroupsReady(groupsReady);
        response.setTeamsReady(teamsReady);
        response.setReadyForScoring(readyForScoring);
        response.setScoreEntryComplete(scoreEntryComplete);
        response.setReadyForFinalization(readyForFinalization);
        response.setReady(ready);
        response.setHasTeamEvents(eventCapabilities.hasTeamEvent());
        response.setHasIndividualEvents(eventCapabilities.hasIndividualEvent());
        response.setHasScrambleEvent(eventCapabilities.hasScrambleEvent());
        response.setHasTwoManLowNetEvent(eventCapabilities.hasTwoManLowNetEvent());
        response.setRequiresTeams(eventCapabilities.requiresTeams());
        response.setRequiresNetScores(eventCapabilities.requiresNetScores());
        response.setRequiresPlayerScorecards(eventCapabilities.requiresPlayerScorecards());
        response.setExpectedTeamSize(eventCapabilities.expectedTeamSize());
        response.setScorecardCount(scorecards == null ? 0 : scorecards.size());
        response.setGroupCount(groups == null ? 0 : groups.size());
        response.setTeamCount(teams == null ? 0 : teams.size());
        response.setMissingScoreCount(missingScoreCount);
        response.setBlockingIssues(blockingIssues);
        response.setWarnings(warnings);

        return response;
    }


    private boolean isActiveScorecard(Scorecard scorecard) {
        if (scorecard == null || scorecard.getParticipationStatus() == null) {
            return true;
        }
        if (scorecard.getParticipationStatus() == ScorecardParticipationStatus.ACTIVE) {
            return true;
        }
        // A mid-round WD is still a valid member of the original team/group setup.
        // Only no-shows and pre-round withdrawals are ignored for setup/readiness.
        return scorecard.getParticipationStatus() == ScorecardParticipationStatus.WITHDRAWN
                && scorecard.getWithdrawalHoleNumber() != null
                && scorecard.getWithdrawalHoleNumber() > 0;
    }

    private int calculateMissingScoreCount(Round round, List<Scorecard> scorecards, List<RoundTeam> teams) {
        if (round == null) {
            return 1;
        }

        RoundEventCapabilityService.RoundEventCapabilities capabilities = roundEventCapabilityService.getCapabilities(round);
        int missing = 0;

        if (capabilities.hasScrambleEvent()) {
            missing += calculateMissingScrambleTeamScoreCount(teams);
        }

        if (capabilities.requiresPlayerScorecards()) {
            missing += calculateMissingPlayerScoreCount(scorecards);
        }

        return missing;
    }

    private int calculateMissingScrambleTeamScoreCount(List<RoundTeam> teams) {
        int missing = 0;
        if (teams == null || teams.isEmpty()) {
            return 1;
        }
        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null) {
                missing++;
                continue;
            }
            if (team.getScrambleTotalScore() != null) {
                continue;
            }
            List<TeamHoleScore> holeScores = teamHoleScoreRepository.findByRoundTeam_IdOrderByHoleNumberAsc(team.getId());
            if (holeScores == null || holeScores.size() != 18) {
                missing++;
                continue;
            }
            Set<Integer> holes = new HashSet<>();
            for (TeamHoleScore holeScore : holeScores) {
                if (holeScore != null && holeScore.getHoleNumber() != null && holeScore.getStrokes() != null) {
                    holes.add(holeScore.getHoleNumber());
                }
            }
            if (holes.size() != 18) {
                missing++;
            }
        }
        return missing;
    }

    private int calculateMissingPlayerScoreCount(List<Scorecard> scorecards) {
        if (scorecards == null || scorecards.isEmpty()) {
            return 1;
        }

        int missing = 0;
        for (Scorecard scorecard : scorecards) {
            if (!isActiveScorecard(scorecard)) {
                continue;
            }
            if (scorecard == null
                    || scorecard.getGrossScore() == null
                    || scorecard.getAdjustedGrossScore() == null
                    || scorecard.getNetScore() == null) {
                missing++;
            }
        }
        return missing;
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

        if (!roundEventCapabilityService.hasConfiguredEvents(round)) {
            blockingIssues.add("No scoring events are configured for this round.");
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

    private boolean calculateHandicapsReady(Round round, List<Scorecard> scorecards) {
        if (!roundEventCapabilityService.requiresNetScores(round)) {
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
            List<String> blockingIssues,
            List<String> warnings
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

    private boolean calculateTeamsReady(
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
            if (!isActiveScorecard(scorecard)) {
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
            if (!isActiveScorecard(scorecard)) {
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
            if (!isActiveScorecard(scorecard)) {
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
