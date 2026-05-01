package com.myrtletrip.scorehistory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ManualScoreHistoryEntryResponse {

    private Long scoreHistoryEntryId;
    private Long playerId;
    private String playerName;
    private LocalDate scoreDate;
    private String courseName;
    private BigDecimal courseRating;
    private Integer slope;
    private Integer grossScore;
    private Integer adjustedGrossScore;
    private BigDecimal differential;
    private Boolean includedInMyrtleCalc;
    private Integer holesPlayed;
    private Integer postingOrder;

    public Long getScoreHistoryEntryId() { return scoreHistoryEntryId; }
    public void setScoreHistoryEntryId(Long scoreHistoryEntryId) { this.scoreHistoryEntryId = scoreHistoryEntryId; }
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public LocalDate getScoreDate() { return scoreDate; }
    public void setScoreDate(LocalDate scoreDate) { this.scoreDate = scoreDate; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public BigDecimal getCourseRating() { return courseRating; }
    public void setCourseRating(BigDecimal courseRating) { this.courseRating = courseRating; }
    public Integer getSlope() { return slope; }
    public void setSlope(Integer slope) { this.slope = slope; }
    public Integer getGrossScore() { return grossScore; }
    public void setGrossScore(Integer grossScore) { this.grossScore = grossScore; }
    public Integer getAdjustedGrossScore() { return adjustedGrossScore; }
    public void setAdjustedGrossScore(Integer adjustedGrossScore) { this.adjustedGrossScore = adjustedGrossScore; }
    public BigDecimal getDifferential() { return differential; }
    public void setDifferential(BigDecimal differential) { this.differential = differential; }
    public Boolean getIncludedInMyrtleCalc() { return includedInMyrtleCalc; }
    public void setIncludedInMyrtleCalc(Boolean includedInMyrtleCalc) { this.includedInMyrtleCalc = includedInMyrtleCalc; }
    public Integer getHolesPlayed() { return holesPlayed; }
    public void setHolesPlayed(Integer holesPlayed) { this.holesPlayed = holesPlayed; }
    public Integer getPostingOrder() { return postingOrder; }
    public void setPostingOrder(Integer postingOrder) { this.postingOrder = postingOrder; }
}
