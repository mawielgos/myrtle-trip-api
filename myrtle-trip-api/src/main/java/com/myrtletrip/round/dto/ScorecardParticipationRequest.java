package com.myrtletrip.round.dto;

public class ScorecardParticipationRequest {
    private String participationStatus;
    private Integer withdrawalHoleNumber;

    public String getParticipationStatus() {
        return participationStatus;
    }

    public void setParticipationStatus(String participationStatus) {
        this.participationStatus = participationStatus;
    }

    public Integer getWithdrawalHoleNumber() {
        return withdrawalHoleNumber;
    }

    public void setWithdrawalHoleNumber(Integer withdrawalHoleNumber) {
        this.withdrawalHoleNumber = withdrawalHoleNumber;
    }
}
