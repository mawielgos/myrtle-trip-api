package com.myrtletrip.handicap.card.dto;

import java.math.BigDecimal;

public class HandicapCardPlayerSummaryResponse {

    private Long playerId;
    private String playerName;
    private Integer displayOrder;
    private String handicapMethod;
    private BigDecimal tripIndex;
    private Integer eligibleScoreCount;
    private Integer windowScoreCount;
    private Integer usedScoreCount;
    private String statusCode;
    private String statusLabel;

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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
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

    public Integer getEligibleScoreCount() {
        return eligibleScoreCount;
    }

    public void setEligibleScoreCount(Integer eligibleScoreCount) {
        this.eligibleScoreCount = eligibleScoreCount;
    }

    public Integer getWindowScoreCount() {
        return windowScoreCount;
    }

    public void setWindowScoreCount(Integer windowScoreCount) {
        this.windowScoreCount = windowScoreCount;
    }

    public Integer getUsedScoreCount() {
        return usedScoreCount;
    }

    public void setUsedScoreCount(Integer usedScoreCount) {
        this.usedScoreCount = usedScoreCount;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }
}
