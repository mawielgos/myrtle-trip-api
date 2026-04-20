package com.myrtletrip.round.dto;

public class RoundSetupStatusResponse {

    private Long roundId;
    private RoundStatusResponse round;
    private RoundReadinessResponse readiness;
    private RoundGroupPageResponse groups;
    private RoundTeamAssignmentPageResponse teamAssignment;

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public RoundStatusResponse getRound() {
        return round;
    }

    public void setRound(RoundStatusResponse round) {
        this.round = round;
    }

    public RoundReadinessResponse getReadiness() {
        return readiness;
    }

    public void setReadiness(RoundReadinessResponse readiness) {
        this.readiness = readiness;
    }

    public RoundGroupPageResponse getGroups() {
        return groups;
    }

    public void setGroups(RoundGroupPageResponse groups) {
        this.groups = groups;
    }

    public RoundTeamAssignmentPageResponse getTeamAssignment() {
        return teamAssignment;
    }

    public void setTeamAssignment(RoundTeamAssignmentPageResponse teamAssignment) {
        this.teamAssignment = teamAssignment;
    }
}
