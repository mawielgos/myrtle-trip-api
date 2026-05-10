package com.myrtletrip.round.dto;

public class RoundPlayerStatusResponse {

    private Long scorecardId;
    private Long playerId;
    private String playerName;
    private java.math.BigDecimal tripIndex;
    private java.time.LocalDate handicapAsOfDate;
    private String handicapMethod;
    private String handicapLabel;
    private String gender;
    private Long roundTeeId;
    private String roundTeeName;
    private Integer courseHandicap;
    private Integer playingHandicap;

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
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Long getRoundTeeId() { return roundTeeId; }
    public void setRoundTeeId(Long roundTeeId) { this.roundTeeId = roundTeeId; }
    public String getRoundTeeName() { return roundTeeName; }
    public void setRoundTeeName(String roundTeeName) { this.roundTeeName = roundTeeName; }
    public Integer getCourseHandicap() { return courseHandicap; }
    public void setCourseHandicap(Integer courseHandicap) { this.courseHandicap = courseHandicap; }
    public Integer getPlayingHandicap() { return playingHandicap; }
    public void setPlayingHandicap(Integer playingHandicap) { this.playingHandicap = playingHandicap; }
}
