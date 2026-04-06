package com.myrtletrip.trip.dto;

import java.math.BigDecimal;

public class TripPlayerResponse {

    private Long playerId;
    private String displayName;
    private BigDecimal handicapIndex;
    private Boolean active;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public BigDecimal getHandicapIndex() {
        return handicapIndex;
    }

    public void setHandicapIndex(BigDecimal handicapIndex) {
        this.handicapIndex = handicapIndex;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
