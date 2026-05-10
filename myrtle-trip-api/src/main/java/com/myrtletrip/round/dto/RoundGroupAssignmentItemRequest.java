package com.myrtletrip.round.dto;

public class RoundGroupAssignmentItemRequest {

    private Long scorecardId;
    private Long playerId;
    private Integer groupNumber;
    private Integer seatOrder;
    private Long roundTeeId;

    public Long getScorecardId() {
        return scorecardId;
    }

    public void setScorecardId(Long scorecardId) {
        this.scorecardId = scorecardId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Integer getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(Integer groupNumber) {
        this.groupNumber = groupNumber;
    }

    public Integer getSeatOrder() {
        return seatOrder;
    }

    public void setSeatOrder(Integer seatOrder) {
        this.seatOrder = seatOrder;
    }

    public Long getRoundTeeId() {
        return roundTeeId;
    }

    public void setRoundTeeId(Long roundTeeId) {
        this.roundTeeId = roundTeeId;
    }
}
