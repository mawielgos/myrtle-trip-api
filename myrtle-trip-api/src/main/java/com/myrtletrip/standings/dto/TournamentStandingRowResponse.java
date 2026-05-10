package com.myrtletrip.standings.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TournamentStandingRowResponse {

    private Long playerId;
    private Integer tripNumber;
    private Integer position;
    private String playerName;

    private Integer totalScore;
    private Integer totalToPar;
    private Integer completedRounds;
    private Boolean tournamentComplete;

    private BigDecimal money;
    private List<TournamentStandingRoundResponse> rounds = new ArrayList<>();

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

    public Integer getCompletedRounds() {
        return completedRounds;
    }

    public void setCompletedRounds(Integer completedRounds) {
        this.completedRounds = completedRounds;
    }

    public Boolean getTournamentComplete() {
        return tournamentComplete;
    }

    public void setTournamentComplete(Boolean tournamentComplete) {
        this.tournamentComplete = tournamentComplete;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public List<TournamentStandingRoundResponse> getRounds() {
        return rounds;
    }

    public void setRounds(List<TournamentStandingRoundResponse> rounds) {
        this.rounds = rounds;
    }
}
