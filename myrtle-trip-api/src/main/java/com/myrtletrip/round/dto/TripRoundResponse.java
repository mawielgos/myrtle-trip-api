package com.myrtletrip.round.dto;

import java.math.BigDecimal;

public class TripRoundResponse {

    private Long roundId;
    private Integer grossScore;
    private BigDecimal differential;
    private BigDecimal updatedTripIndex;

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public Integer getGrossScore() {
        return grossScore;
    }

    public void setGrossScore(Integer grossScore) {
        this.grossScore = grossScore;
    }

    public BigDecimal getDifferential() {
        return differential;
    }

    public void setDifferential(BigDecimal differential) {
        this.differential = differential;
    }

    public BigDecimal getUpdatedTripIndex() {
        return updatedTripIndex;
    }

    public void setUpdatedTripIndex(BigDecimal updatedTripIndex) {
        this.updatedTripIndex = updatedTripIndex;
    }
}