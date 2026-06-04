package com.myrtletrip.event.dto;

public class IndividualEventResult {

    private Long playerId;
    private String playerName;
    private Integer grossTotal;
    private Integer netTotal;
    private Integer rank;

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public Integer getGrossTotal() { return grossTotal; }
    public void setGrossTotal(Integer grossTotal) { this.grossTotal = grossTotal; }

    public Integer getNetTotal() { return netTotal; }
    public void setNetTotal(Integer netTotal) { this.netTotal = netTotal; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
}
