package com.myrtletrip.prize.dto;

import java.util.ArrayList;
import java.util.List;

public class PrizeScheduleResponse {
    private Long scheduleId;
    private Long tripId;
    private Long roundId;
    private Integer roundNumber;
    private String gameKey;
    private String gameName;
    private String resultScope;
    private String payoutUnit;
    private List<PrizeSchedulePayoutResponse> payouts = new ArrayList<>();

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getGameKey() {
        return gameKey;
    }

    public void setGameKey(String gameKey) {
        this.gameKey = gameKey;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getResultScope() {
        return resultScope;
    }

    public void setResultScope(String resultScope) {
        this.resultScope = resultScope;
    }

    public String getPayoutUnit() {
        return payoutUnit;
    }

    public void setPayoutUnit(String payoutUnit) {
        this.payoutUnit = payoutUnit;
    }

    public List<PrizeSchedulePayoutResponse> getPayouts() {
        return payouts;
    }

    public void setPayouts(List<PrizeSchedulePayoutResponse> payouts) {
        this.payouts = payouts;
    }
}
