package com.myrtletrip.round.dto;

import java.util.List;

public class PlayerBulkScoreDto {

    private Long playerId;
    private List<Integer> holes;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public List<Integer> getHoles() {
        return holes;
    }

    public void setHoles(List<Integer> holes) {
        this.holes = holes;
    }
}