package com.myrtletrip.round.dto;

public class RoundTeamPlayerRequest {

    private Long playerId;
    private Integer playerOrder;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Integer getPlayerOrder() {
        return playerOrder;
    }

    public void setPlayerOrder(Integer playerOrder) {
        this.playerOrder = playerOrder;
    }
}
