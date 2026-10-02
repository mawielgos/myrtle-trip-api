package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundReadinessResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundTeam;
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
import java.util.List;

@Service
public class RoundReadinessService {

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundEventCapabilityService roundEventCapabilityService;
    private final RoundScoreReadinessService roundScoreReadinessService;
    private final RoundGroupingReadinessService roundGroupingReadinessService;
    private final RoundTeamReadinessService roundTeamReadinessService;

    public RoundReadinessService(
            RoundRepository roundRepository,
            ScorecardRepository scorecardRepository,
            RoundGroupRepository roundGroupRepository,
            RoundTeamRepository roundTeamRepository,
            RoundEventCapabilityService roundEventCapabilityService,
            RoundScoreReadinessService roundScoreReadinessService,
            RoundGroupingReadinessService roundGroupingReadinessService,
            RoundTeamReadinessService roundTeamReadinessService
    ) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundEventCapabilityService = roundEventCapabilityService;
        this.roundScoreReadinessService = roundScoreReadinessService;
        this.roundGroupingReadinessService = roundGroupingReadinessService;
        this.roundTeamReadinessService = roundTeamReadinessService;
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
                .filter(roundScoreReadinessService::isActiveScorecard)
                .toList();
        boolean scorecardsReady = scorecards != null && !scorecards.isEmpty();
        if (!scorecardsReady) {
            blockingIssues.add("No scorecards exist for this round.");
        }

        boolean teesReady = scorecardsReady && roundScoreReadinessService.allScorecardsHaveTee(scorecards);
        if (scorecardsReady && !teesReady) {
            blockingIssues.add("One or more players does not have a tee selected.");
        }

        boolean handicapsReady = scorecardsReady && roundScoreReadinessService.areHandicapsReady(round, scorecards);
        if (scorecardsReady && !handicapsReady) {
            blockingIssues.add("One or more scorecards is missing calculated handicap values.");
        }

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
        boolean teamsReady = roundTeamReadinessService.calculateTeamsReady(round, scorecards, teams, blockingIssues, warnings);

        List<RoundGroup> groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);
        boolean groupsReady = roundGroupingReadinessService.calculateGroupsReady(round, scorecards, groups, teams, blockingIssues);

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

        int missingScoreCount = roundScoreReadinessService.calculateMissingScoreCount(round, scorecards, teams);
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
}
