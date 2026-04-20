package com.myrtletrip.trip.dto;

import java.math.BigDecimal;

public class GhinFixRowResponse {

    private Long scoreHistoryEntryId;
    private Long playerId;
    private String playerName;
    private String ghinNumber;
    private Integer postingOrder;
    private String scoreType;
    private Integer holesPlayed;
    private Integer grossScore;
    private BigDecimal courseRating;
    private Integer slope;
    private BigDecimal differential;
    private Boolean manualDifferentialRequired;

    public Long getScoreHistoryEntryId() {
        return scoreHistoryEntryId;
    }

    public void setScoreHistoryEntryId(Long scoreHistoryEntryId) {
        this.scoreHistoryEntryId = scoreHistoryEntryId;
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

    public String getGhinNumber() {
        return ghinNumber;
    }

    public void setGhinNumber(String ghinNumber) {
        this.ghinNumber = ghinNumber;
    }

    public Integer getPostingOrder() {
        return postingOrder;
    }

    public void setPostingOrder(Integer postingOrder) {
        this.postingOrder = postingOrder;
    }

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public Integer getHolesPlayed() {
        return holesPlayed;
    }

    public void setHolesPlayed(Integer holesPlayed) {
        this.holesPlayed = holesPlayed;
    }

    public Integer getGrossScore() {
        return grossScore;
    }

    public void setGrossScore(Integer grossScore) {
        this.grossScore = grossScore;
    }

    public BigDecimal getCourseRating() {
        return courseRating;
    }

    public void setCourseRating(BigDecimal courseRating) {
        this.courseRating = courseRating;
    }

    public Integer getSlope() {
        return slope;
    }

    public void setSlope(Integer slope) {
        this.slope = slope;
    }

    public BigDecimal getDifferential() {
        return differential;
    }

    public void setDifferential(BigDecimal differential) {
        this.differential = differential;
    }

    public Boolean getManualDifferentialRequired() {
        return manualDifferentialRequired;
    }

    public void setManualDifferentialRequired(Boolean manualDifferentialRequired) {
        this.manualDifferentialRequired = manualDifferentialRequired;
    }
}
