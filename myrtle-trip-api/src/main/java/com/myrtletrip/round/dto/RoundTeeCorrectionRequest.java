package com.myrtletrip.round.dto;

public class RoundTeeCorrectionRequest {

    private Long scorecardId;
    private Long roundTeeId;

    /** Legacy field retained for older callers. */
    private Boolean useAlternateTee;

    public Long getScorecardId() { return scorecardId; }
    public void setScorecardId(Long scorecardId) { this.scorecardId = scorecardId; }

    public Long getRoundTeeId() { return roundTeeId; }
    public void setRoundTeeId(Long roundTeeId) { this.roundTeeId = roundTeeId; }

    public Boolean getUseAlternateTee() { return useAlternateTee; }
    public void setUseAlternateTee(Boolean useAlternateTee) { this.useAlternateTee = useAlternateTee; }
}
