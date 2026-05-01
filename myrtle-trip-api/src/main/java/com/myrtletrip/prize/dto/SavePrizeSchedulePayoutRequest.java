package com.myrtletrip.prize.dto;

import java.math.BigDecimal;

public class SavePrizeSchedulePayoutRequest {
    private Integer finishingPlace;
    private BigDecimal amountPerPlayer;

    public Integer getFinishingPlace() {
        return finishingPlace;
    }

    public void setFinishingPlace(Integer finishingPlace) {
        this.finishingPlace = finishingPlace;
    }

    public BigDecimal getAmountPerPlayer() {
        return amountPerPlayer;
    }

    public void setAmountPerPlayer(BigDecimal amountPerPlayer) {
        this.amountPerPlayer = amountPerPlayer;
    }
}
