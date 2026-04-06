package com.myrtletrip.contest.dto;

public class TwoManBestBallHoleResponse {

    private Integer holeNumber;
    private Long winningPlayerId;
    private String winningPlayerName;
    private Integer bestNetStrokes;
    private boolean tie;

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public Long getWinningPlayerId() {
        return winningPlayerId;
    }

    public void setWinningPlayerId(Long winningPlayerId) {
        this.winningPlayerId = winningPlayerId;
    }

    public String getWinningPlayerName() {
        return winningPlayerName;
    }

    public void setWinningPlayerName(String winningPlayerName) {
        this.winningPlayerName = winningPlayerName;
    }

    public Integer getBestNetStrokes() {
        return bestNetStrokes;
    }

    public void setBestNetStrokes(Integer bestNetStrokes) {
        this.bestNetStrokes = bestNetStrokes;
    }

    public boolean isTie() {
        return tie;
    }

    public void setTie(boolean tie) {
        this.tie = tie;
    }
}