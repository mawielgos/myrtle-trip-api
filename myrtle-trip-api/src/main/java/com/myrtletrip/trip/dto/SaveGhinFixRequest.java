package com.myrtletrip.trip.dto;

import java.math.BigDecimal;

public class SaveGhinFixRequest {

    private BigDecimal differential;

    public BigDecimal getDifferential() {
        return differential;
    }

    public void setDifferential(BigDecimal differential) {
        this.differential = differential;
    }
}
