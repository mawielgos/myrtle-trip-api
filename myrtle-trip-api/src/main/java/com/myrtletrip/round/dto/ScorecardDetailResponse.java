package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class ScorecardDetailResponse {

    private Long scorecardId;
    private Long playerId;
    private String playerName;
    private java.math.BigDecimal tripIndex;
    private java.time.LocalDate handicapAsOfDate;
    private String handicapMethod;
    private String handicapLabel;
    private Integer courseHandicap;
    private Integer playingHandicap;
    private Integer grossScore;
    private Integer adjustedGrossScore;
    private Integer netScore;
    private String teeName;
    private String currentTeeName;
    private Long roundTeeId;
    private List<ScorecardHoleResponse> holes = new ArrayList<>();

    public Long getScorecardId() { return scorecardId; }
    public void setScorecardId(Long scorecardId) { this.scorecardId = scorecardId; }
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public java.math.BigDecimal getTripIndex() { return tripIndex; }
    public void setTripIndex(java.math.BigDecimal tripIndex) { this.tripIndex = tripIndex; }
    public java.time.LocalDate getHandicapAsOfDate() { return handicapAsOfDate; }
    public void setHandicapAsOfDate(java.time.LocalDate handicapAsOfDate) { this.handicapAsOfDate = handicapAsOfDate; }
    public String getHandicapMethod() { return handicapMethod; }
    public void setHandicapMethod(String handicapMethod) { this.handicapMethod = handicapMethod; }
    public String getHandicapLabel() { return handicapLabel; }
    public void setHandicapLabel(String handicapLabel) { this.handicapLabel = handicapLabel; }
    public Integer getCourseHandicap() { return courseHandicap; }
    public void setCourseHandicap(Integer courseHandicap) { this.courseHandicap = courseHandicap; }
    public Integer getPlayingHandicap() { return playingHandicap; }
    public void setPlayingHandicap(Integer playingHandicap) { this.playingHandicap = playingHandicap; }
    public Integer getGrossScore() { return grossScore; }
    public void setGrossScore(Integer grossScore) { this.grossScore = grossScore; }
    public Integer getAdjustedGrossScore() { return adjustedGrossScore; }
    public void setAdjustedGrossScore(Integer adjustedGrossScore) { this.adjustedGrossScore = adjustedGrossScore; }
    public Integer getNetScore() { return netScore; }
    public void setNetScore(Integer netScore) { this.netScore = netScore; }
    public String getTeeName() { return teeName; }
    public void setTeeName(String teeName) { this.teeName = teeName; }
    public String getCurrentTeeName() { return currentTeeName; }
    public void setCurrentTeeName(String currentTeeName) { this.currentTeeName = currentTeeName; }
    public Long getRoundTeeId() { return roundTeeId; }
    public void setRoundTeeId(Long roundTeeId) { this.roundTeeId = roundTeeId; }
    public List<ScorecardHoleResponse> getHoles() { return holes; }
    public void setHoles(List<ScorecardHoleResponse> holes) { this.holes = holes; }
}
