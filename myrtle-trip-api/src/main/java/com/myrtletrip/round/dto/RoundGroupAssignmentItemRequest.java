package com.myrtletrip.round.dto;

public class RoundGroupAssignmentItemRequest {

    private Long scorecardId;
    private Long playerId;
    private Integer groupNumber;
    private Integer seatOrder;
    private Boolean useAlternateTee;

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

    public Boolean getUseAlternateTee() {
        return useAlternateTee;
    }

    public void setUseAlternateTee(Boolean useAlternateTee) {
        this.useAlternateTee = useAlternateTee;
    }
}