package com.myrtletrip.trip.dto;

import java.math.BigDecimal;

public class TripPlayerResponse {

    private Long playerId;
    private String displayName;
    private String ghinNumber;
    private BigDecimal handicapIndex;
    private BigDecimal frozenHandicapIndex;
    private Boolean active;
    private Long ghinHistoryCount;
    private Long dbScoreHistoryCount;
    private Long tripScoreCount;
    private Boolean usableHandicapIndex;

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

    public String getGhinNumber() {
        return ghinNumber;
    }

    public void setGhinNumber(String ghinNumber) {
        this.ghinNumber = ghinNumber;
    }

    public BigDecimal getHandicapIndex() {
        return handicapIndex;
    }

    public void setHandicapIndex(BigDecimal handicapIndex) {
        this.handicapIndex = handicapIndex;
    }

    public BigDecimal getFrozenHandicapIndex() {
        return frozenHandicapIndex;
    }

    public void setFrozenHandicapIndex(BigDecimal frozenHandicapIndex) {
        this.frozenHandicapIndex = frozenHandicapIndex;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getGhinHistoryCount() {
        return ghinHistoryCount;
    }

    public void setGhinHistoryCount(Long ghinHistoryCount) {
        this.ghinHistoryCount = ghinHistoryCount;
    }

    public Long getDbScoreHistoryCount() {
        return dbScoreHistoryCount;
    }

    public void setDbScoreHistoryCount(Long dbScoreHistoryCount) {
        this.dbScoreHistoryCount = dbScoreHistoryCount;
    }

    public Long getTripScoreCount() {
        return tripScoreCount;
    }

    public void setTripScoreCount(Long tripScoreCount) {
        this.tripScoreCount = tripScoreCount;
    }

    public Boolean getUsableHandicapIndex() {
        return usableHandicapIndex;
    }

    public void setUsableHandicapIndex(Boolean usableHandicapIndex) {
        this.usableHandicapIndex = usableHandicapIndex;
    }
}
