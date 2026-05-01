package com.myrtletrip.round.dto;

public class RoundTeamPlayerRequest {

    private Long scorecardId;
    private Long playerId;
    private Integer playerOrder;
    private Long roundTeeId;

    /** Legacy field retained for older callers; ignored by V1.1 UI. */
    private Boolean useAlternateTee;

    public Long getScorecardId() { return scorecardId; }
    public void setScorecardId(Long scorecardId) { this.scorecardId = scorecardId; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Integer getPlayerOrder() { return playerOrder; }
    public void setPlayerOrder(Integer playerOrder) { this.playerOrder = playerOrder; }

    public Long getRoundTeeId() { return roundTeeId; }
    public void setRoundTeeId(Long roundTeeId) { this.roundTeeId = roundTeeId; }

    public Boolean getUseAlternateTee() { return useAlternateTee; }
    public void setUseAlternateTee(Boolean useAlternateTee) { this.useAlternateTee = useAlternateTee; }
}
