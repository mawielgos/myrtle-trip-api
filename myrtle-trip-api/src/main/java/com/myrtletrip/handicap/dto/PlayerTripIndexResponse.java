package com.myrtletrip.handicap.dto;

import java.math.BigDecimal;

public class PlayerTripIndexResponse {

    private Long playerId;
    private String playerName;
    private String handicapMethod;
    private BigDecimal tripIndex;

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

    public String getHandicapMethod() {
        return handicapMethod;
    }

    public void setHandicapMethod(String handicapMethod) {
        this.handicapMethod = handicapMethod;
    }

    public BigDecimal getTripIndex() {
        return tripIndex;
    }

    public void setTripIndex(BigDecimal tripIndex) {
        this.tripIndex = tripIndex;
    }
}