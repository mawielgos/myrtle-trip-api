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
    private boolean ready;

    private int scorecardCount;
    private int groupCount;
    private int teamCount;

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

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

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
