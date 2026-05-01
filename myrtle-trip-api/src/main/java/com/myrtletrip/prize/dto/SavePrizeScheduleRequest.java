package com.myrtletrip.prize.dto;

import java.util.ArrayList;
import java.util.List;

public class SavePrizeScheduleRequest {
    private String gameKey;
    private String gameName;
    private String resultScope;
    private String payoutUnit;
    private List<SavePrizeSchedulePayoutRequest> payouts = new ArrayList<>();

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

    public List<SavePrizeSchedulePayoutRequest> getPayouts() {
        return payouts;
    }

    public void setPayouts(List<SavePrizeSchedulePayoutRequest> payouts) {
        this.payouts = payouts;
    }
}
