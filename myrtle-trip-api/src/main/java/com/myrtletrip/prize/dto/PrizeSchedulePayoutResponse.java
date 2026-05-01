package com.myrtletrip.prize.dto;

import java.math.BigDecimal;

public class PrizeSchedulePayoutResponse {
    private Long payoutId;
    private Integer finishingPlace;
    private BigDecimal amountPerPlayer;

    public Long getPayoutId() {
        return payoutId;
    }

    public void setPayoutId(Long payoutId) {
        this.payoutId = payoutId;
    }

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
