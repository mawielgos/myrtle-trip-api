package com.myrtletrip.skins.dto;

public class PlayerSkinSummaryResponse {

    private Long playerId;
    private String playerName;
    private Integer skinsWon;

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

    public Integer getSkinsWon() {
        return skinsWon;
    }

    public void setSkinsWon(Integer skinsWon) {
        this.skinsWon = skinsWon;
    }
}