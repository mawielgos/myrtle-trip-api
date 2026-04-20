package com.myrtletrip.standings.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FourDayStandingRowResponse {

    private Long playerId;
    private Integer tripNumber;
    private Integer position;
    private String playerName;

    private Integer totalScore;
    private Integer totalToPar;

    private BigDecimal money;
    private List<FourDayStandingRoundResponse> rounds = new ArrayList<>();

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Integer getTripNumber() {
        return tripNumber;
    }

    public void setTripNumber(Integer tripNumber) {
        this.tripNumber = tripNumber;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getTotalToPar() {
        return totalToPar;
    }

    public void setTotalToPar(Integer totalToPar) {
        this.totalToPar = totalToPar;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public List<FourDayStandingRoundResponse> getRounds() {
        return rounds;
    }

    public void setRounds(List<FourDayStandingRoundResponse> rounds) {
        this.rounds = rounds;
    }
}
