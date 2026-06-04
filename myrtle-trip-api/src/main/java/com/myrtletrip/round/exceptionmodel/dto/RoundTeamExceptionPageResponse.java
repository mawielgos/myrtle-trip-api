package com.myrtletrip.round.exceptionmodel.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundTeamExceptionPageResponse {
    private Long roundId;
    private List<RoundTeamExceptionResponse> exceptions = new ArrayList<>();

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    public List<RoundTeamExceptionResponse> getExceptions() { return exceptions; }
    public void setExceptions(List<RoundTeamExceptionResponse> exceptions) { this.exceptions = exceptions; }
}
