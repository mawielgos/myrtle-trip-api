package com.myrtletrip.games.dto;

import java.util.List;

public class TeamGameResultResponse {

    private Long teamId;
    private Integer teamNumber;
    private String teamName;
    private Integer totalScore;
    private Integer rank;
    private List<String> playerNames;
    private List<HoleGameResultResponse> holes;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Integer getTeamNumber() {
        return teamNumber;
    }

    public void setTeamNumber(Integer teamNumber) {
        this.teamNumber = teamNumber;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public void setPlayerNames(List<String> playerNames) {
        this.playerNames = playerNames;
    }

    public List<HoleGameResultResponse> getHoles() {
        return holes;
    }

    public void setHoles(List<HoleGameResultResponse> holes) {
        this.holes = holes;
    }
}
