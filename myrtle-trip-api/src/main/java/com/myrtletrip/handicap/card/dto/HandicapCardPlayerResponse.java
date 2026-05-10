package com.myrtletrip.handicap.card.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HandicapCardPlayerResponse {

    private Long tripId;
    private String tripName;
    private String tripCode;
    private Integer tripYear;
    private LocalDate asOfDate;
    private Long playerId;
    private String playerName;
    private String handicapMethod;
    private BigDecimal tripIndex;
    private BigDecimal pendingTripIndex;
    private Integer pendingScoreCount;
    private Integer eligibleScoreCount;
    private Integer windowScoreCount;
    private Integer usedScoreCount;
    private String calculationLabel;
    private String pendingCalculationLabel;
    private List<HandicapCardScoreResponse> scores = new ArrayList<>();

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getTripName() {
        return tripName;
    }

    public void setTripName(String tripName) {
        this.tripName = tripName;
    }

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }

    public Integer getTripYear() {
        return tripYear;
    }

    public void setTripYear(Integer tripYear) {
        this.tripYear = tripYear;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
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

    public BigDecimal getPendingTripIndex() {
        return pendingTripIndex;
    }

    public void setPendingTripIndex(BigDecimal pendingTripIndex) {
        this.pendingTripIndex = pendingTripIndex;
    }

    public Integer getPendingScoreCount() {
        return pendingScoreCount;
    }

    public void setPendingScoreCount(Integer pendingScoreCount) {
        this.pendingScoreCount = pendingScoreCount;
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

    public String getCalculationLabel() {
        return calculationLabel;
    }

    public void setCalculationLabel(String calculationLabel) {
        this.calculationLabel = calculationLabel;
    }

    public String getPendingCalculationLabel() {
        return pendingCalculationLabel;
    }

    public void setPendingCalculationLabel(String pendingCalculationLabel) {
        this.pendingCalculationLabel = pendingCalculationLabel;
    }

    public List<HandicapCardScoreResponse> getScores() {
        return scores;
    }

    public void setScores(List<HandicapCardScoreResponse> scores) {
        this.scores = scores;
    }
}
