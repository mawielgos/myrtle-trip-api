package com.myrtletrip.trip.dto;

import java.math.BigDecimal;

public class SaveTripPlayerFrozenIndexRequest {

    private BigDecimal frozenHandicapIndex;

    public BigDecimal getFrozenHandicapIndex() {
        return frozenHandicapIndex;
    }

    public void setFrozenHandicapIndex(BigDecimal frozenHandicapIndex) {
        this.frozenHandicapIndex = frozenHandicapIndex;
    }
}
