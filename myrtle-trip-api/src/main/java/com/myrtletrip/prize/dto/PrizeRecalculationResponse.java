package com.myrtletrip.prize.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PrizeRecalculationResponse {

    private Long tripId;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private List<PrizeWinningResponse> winnings = new ArrayList<>();
    private List<PrizePlayerTotalResponse> playerTotals = new ArrayList<>();

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<PrizeWinningResponse> getWinnings() {
        return winnings;
    }

    public void setWinnings(List<PrizeWinningResponse> winnings) {
        this.winnings = winnings;
    }

    public List<PrizePlayerTotalResponse> getPlayerTotals() {
        return playerTotals;
    }

    public void setPlayerTotals(List<PrizePlayerTotalResponse> playerTotals) {
        this.playerTotals = playerTotals;
    }
}
