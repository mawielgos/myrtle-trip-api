package com.myrtletrip.round.dto;

public class RoundTeamPlayerResponse {

    private Long scorecardId;
    private Long playerId;
    private String playerName;
    private Integer playerOrder;
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

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Integer getPlayerOrder() {
        return playerOrder;
    }

    public void setPlayerOrder(Integer playerOrder) {
        this.playerOrder = playerOrder;
    }

    public Boolean getUseAlternateTee() {
        return useAlternateTee;
    }

    public void setUseAlternateTee(Boolean useAlternateTee) {
        this.useAlternateTee = useAlternateTee;
    }
}