package com.myrtletrip.scorehistory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DbScoreHistoryImportCandidateResponse {
    private Long sourceScoreHistoryEntryId;
    private Long playerId;
    private String playerName;
    private Long sourceTripId;
    private String sourceTripName;
    private String sourceTripCode;
    private Integer sourceRoundNumber;
    private LocalDate scoreDate;
    private String courseName;
    private BigDecimal courseRating;
    private Integer slope;
    private Integer grossScore;
    private Integer adjustedGrossScore;
    private BigDecimal differential;
    private Boolean includedInMyrtleCalc;
    private Integer holesPlayed;

    public Long getSourceScoreHistoryEntryId() { return sourceScoreHistoryEntryId; }
    public void setSourceScoreHistoryEntryId(Long sourceScoreHistoryEntryId) { this.sourceScoreHistoryEntryId = sourceScoreHistoryEntryId; }
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public Long getSourceTripId() { return sourceTripId; }
    public void setSourceTripId(Long sourceTripId) { this.sourceTripId = sourceTripId; }
    public String getSourceTripName() { return sourceTripName; }
    public void setSourceTripName(String sourceTripName) { this.sourceTripName = sourceTripName; }
    public String getSourceTripCode() { return sourceTripCode; }
    public void setSourceTripCode(String sourceTripCode) { this.sourceTripCode = sourceTripCode; }
    public Integer getSourceRoundNumber() { return sourceRoundNumber; }
    public void setSourceRoundNumber(Integer sourceRoundNumber) { this.sourceRoundNumber = sourceRoundNumber; }
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
}
