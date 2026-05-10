package com.myrtletrip.round.dto;

public class RoundTeeCorrectionRequest {

    private Long scorecardId;
    private Long roundTeeId;

    public Long getScorecardId() { return scorecardId; }
    public void setScorecardId(Long scorecardId) { this.scorecardId = scorecardId; }
    public Long getRoundTeeId() { return roundTeeId; }
    public void setRoundTeeId(Long roundTeeId) { this.roundTeeId = roundTeeId; }
}
