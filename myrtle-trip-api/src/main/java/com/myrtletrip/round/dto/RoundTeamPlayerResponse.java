package com.myrtletrip.round.dto;

public class RoundTeamPlayerResponse {

    private Long scorecardId;
    private Long playerId;
    private String playerName;
    private Integer playerOrder;
    private java.math.BigDecimal tripIndex;
    private Long roundTeeId;
    private String roundTeeName;
    private Boolean teeOverride;
    private String gender;

    public Long getScorecardId() { return scorecardId; }
    public void setScorecardId(Long scorecardId) { this.scorecardId = scorecardId; }
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public Integer getPlayerOrder() { return playerOrder; }
    public void setPlayerOrder(Integer playerOrder) { this.playerOrder = playerOrder; }
    public java.math.BigDecimal getTripIndex() { return tripIndex; }
    public void setTripIndex(java.math.BigDecimal tripIndex) { this.tripIndex = tripIndex; }
    public Long getRoundTeeId() { return roundTeeId; }
    public void setRoundTeeId(Long roundTeeId) { this.roundTeeId = roundTeeId; }
    public String getRoundTeeName() { return roundTeeName; }
    public void setRoundTeeName(String roundTeeName) { this.roundTeeName = roundTeeName; }
    public Boolean getTeeOverride() { return teeOverride; }
    public void setTeeOverride(Boolean teeOverride) { this.teeOverride = teeOverride; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}
