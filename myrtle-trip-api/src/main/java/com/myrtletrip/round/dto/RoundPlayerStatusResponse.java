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
    private Long teamId;
    private String teamName;
    private Integer teamNumber;
    private Integer playerOrder;
    private String participationStatus;
    private Integer withdrawalHoleNumber;

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
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public Integer getTeamNumber() { return teamNumber; }
    public void setTeamNumber(Integer teamNumber) { this.teamNumber = teamNumber; }
    public Integer getPlayerOrder() { return playerOrder; }
    public void setPlayerOrder(Integer playerOrder) { this.playerOrder = playerOrder; }
    public String getParticipationStatus() { return participationStatus; }
    public void setParticipationStatus(String participationStatus) { this.participationStatus = participationStatus; }
    public Integer getWithdrawalHoleNumber() { return withdrawalHoleNumber; }
    public void setWithdrawalHoleNumber(Integer withdrawalHoleNumber) { this.withdrawalHoleNumber = withdrawalHoleNumber; }
}
