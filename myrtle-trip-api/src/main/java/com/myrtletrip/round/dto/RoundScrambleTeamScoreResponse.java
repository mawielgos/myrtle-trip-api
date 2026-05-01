package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundScrambleTeamScoreResponse {

    private Long roundTeamId;
    private Integer teamNumber;
    private String teamName;
    private Integer totalScore;
    private List<Integer> holes = new ArrayList<>();

    public Long getRoundTeamId() {
        return roundTeamId;
    }

    public void setRoundTeamId(Long roundTeamId) {
        this.roundTeamId = roundTeamId;
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

    public List<Integer> getHoles() {
        return holes;
    }

    public void setHoles(List<Integer> holes) {
        this.holes = holes;
    }
}
