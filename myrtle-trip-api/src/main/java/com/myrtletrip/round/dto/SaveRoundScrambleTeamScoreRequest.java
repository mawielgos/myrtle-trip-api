package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class SaveRoundScrambleTeamScoreRequest {

    private Long roundTeamId;
    private Integer totalScore;
    private List<Integer> holes = new ArrayList<>();

    public Long getRoundTeamId() {
        return roundTeamId;
    }

    public void setRoundTeamId(Long roundTeamId) {
        this.roundTeamId = roundTeamId;
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
