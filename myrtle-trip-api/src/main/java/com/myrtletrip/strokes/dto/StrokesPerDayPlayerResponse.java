package com.myrtletrip.strokes.dto;

import java.util.ArrayList;
import java.util.List;

public class StrokesPerDayPlayerResponse {

    private Long playerId;
    private String playerName;
    private Integer displayOrder;
    private List<StrokesPerDayPlayerRoundResponse> rounds = new ArrayList<>();

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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public List<StrokesPerDayPlayerRoundResponse> getRounds() {
        return rounds;
    }

    public void setRounds(List<StrokesPerDayPlayerRoundResponse> rounds) {
        this.rounds = rounds;
    }
}
