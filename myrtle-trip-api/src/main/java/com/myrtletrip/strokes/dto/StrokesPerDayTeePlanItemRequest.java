package com.myrtletrip.strokes.dto;

public class StrokesPerDayTeePlanItemRequest {

    private Long playerId;
    private Long roundId;
    private Long roundTeeId;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public Long getRoundTeeId() {
        return roundTeeId;
    }

    public void setRoundTeeId(Long roundTeeId) {
        this.roundTeeId = roundTeeId;
    }
}
