package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundReadinessResponse {

    private Long roundId;
    private Integer roundNumber;
    private String roundFormat;
    private boolean finalized;

    private boolean roundConfigured;
    private boolean groupsReady;
    private boolean teamsReady;
    private boolean teesReady;
    private boolean scorecardsReady;
    private boolean handicapsReady;
    private boolean readyForScoring;
    private boolean readyForFinalization;
    private boolean scoreEntryComplete;
    private boolean ready;

    // V1.2 event-capability flags. These are derived from round_event rows
    // with legacy Round.format as a fallback, so UI/navigation no longer has
    // to infer behavior from the compatibility format field.
    private boolean hasTeamEvents;
    private boolean hasIndividualEvents;
    private boolean hasScrambleEvent;
    private boolean hasTwoManLowNetEvent;
    private boolean requiresTeams;
    private boolean requiresNetScores;
    private boolean requiresPlayerScorecards;
    private Integer expectedTeamSize;

    private int scorecardCount;
    private int groupCount;
    private int teamCount;
    private int missingScoreCount;

    private List<String> blockingIssues = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getRoundFormat() {
        return roundFormat;
    }

    public void setRoundFormat(String roundFormat) {
        this.roundFormat = roundFormat;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    public boolean isRoundConfigured() {
        return roundConfigured;
    }

    public void setRoundConfigured(boolean roundConfigured) {
        this.roundConfigured = roundConfigured;
    }

    public boolean isGroupsReady() {
        return groupsReady;
    }

    public void setGroupsReady(boolean groupsReady) {
        this.groupsReady = groupsReady;
    }

    public boolean isTeamsReady() {
        return teamsReady;
    }

    public void setTeamsReady(boolean teamsReady) {
        this.teamsReady = teamsReady;
    }

    public boolean isTeesReady() {
        return teesReady;
    }

    public void setTeesReady(boolean teesReady) {
        this.teesReady = teesReady;
    }

    public boolean isScorecardsReady() {
        return scorecardsReady;
    }

    public void setScorecardsReady(boolean scorecardsReady) {
        this.scorecardsReady = scorecardsReady;
    }

    public boolean isHandicapsReady() {
        return handicapsReady;
    }

    public void setHandicapsReady(boolean handicapsReady) {
        this.handicapsReady = handicapsReady;
    }

    public boolean isReadyForScoring() {
        return readyForScoring;
    }

    public void setReadyForScoring(boolean readyForScoring) {
        this.readyForScoring = readyForScoring;
    }

    public boolean isReadyForFinalization() {
        return readyForFinalization;
    }

    public void setReadyForFinalization(boolean readyForFinalization) {
        this.readyForFinalization = readyForFinalization;
    }

    public boolean isScoreEntryComplete() {
        return scoreEntryComplete;
    }

    public void setScoreEntryComplete(boolean scoreEntryComplete) {
        this.scoreEntryComplete = scoreEntryComplete;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isHasTeamEvents() { return hasTeamEvents; }

    public void setHasTeamEvents(boolean hasTeamEvents) { this.hasTeamEvents = hasTeamEvents; }

    public boolean isHasIndividualEvents() { return hasIndividualEvents; }

    public void setHasIndividualEvents(boolean hasIndividualEvents) { this.hasIndividualEvents = hasIndividualEvents; }

    public boolean isHasScrambleEvent() { return hasScrambleEvent; }

    public void setHasScrambleEvent(boolean hasScrambleEvent) { this.hasScrambleEvent = hasScrambleEvent; }

    public boolean isHasTwoManLowNetEvent() { return hasTwoManLowNetEvent; }

    public void setHasTwoManLowNetEvent(boolean hasTwoManLowNetEvent) { this.hasTwoManLowNetEvent = hasTwoManLowNetEvent; }

    public boolean isRequiresTeams() { return requiresTeams; }

    public void setRequiresTeams(boolean requiresTeams) { this.requiresTeams = requiresTeams; }

    public boolean isRequiresNetScores() { return requiresNetScores; }

    public void setRequiresNetScores(boolean requiresNetScores) { this.requiresNetScores = requiresNetScores; }

    public boolean isRequiresPlayerScorecards() { return requiresPlayerScorecards; }

    public void setRequiresPlayerScorecards(boolean requiresPlayerScorecards) { this.requiresPlayerScorecards = requiresPlayerScorecards; }

    public Integer getExpectedTeamSize() { return expectedTeamSize; }

    public void setExpectedTeamSize(Integer expectedTeamSize) { this.expectedTeamSize = expectedTeamSize; }

    public int getScorecardCount() {
        return scorecardCount;
    }

    public void setScorecardCount(int scorecardCount) {
        this.scorecardCount = scorecardCount;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }

    public int getTeamCount() {
        return teamCount;
    }

    public void setTeamCount(int teamCount) {
        this.teamCount = teamCount;
    }

    public int getMissingScoreCount() {
        return missingScoreCount;
    }

    public void setMissingScoreCount(int missingScoreCount) {
        this.missingScoreCount = missingScoreCount;
    }

    public List<String> getBlockingIssues() {
        return blockingIssues;
    }

    public void setBlockingIssues(List<String> blockingIssues) {
        this.blockingIssues = blockingIssues;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
