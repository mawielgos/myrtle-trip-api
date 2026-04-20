package com.myrtletrip.round.dto;

public class RoundReadinessResponse {

    private Long roundId;

    private boolean groupsReady;
    private boolean teamsReady;
    private boolean scorecardsReady;
    private boolean readyForScoring;

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
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

    public boolean isScorecardsReady() {
        return scorecardsReady;
    }

    public void setScorecardsReady(boolean scorecardsReady) {
        this.scorecardsReady = scorecardsReady;
    }

    public boolean isReadyForScoring() {
        return readyForScoring;
    }

    public void setReadyForScoring(boolean readyForScoring) {
        this.readyForScoring = readyForScoring;
    }
}
