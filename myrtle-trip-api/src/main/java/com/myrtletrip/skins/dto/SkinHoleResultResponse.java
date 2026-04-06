package com.myrtletrip.skins.dto;

public class SkinHoleResultResponse {

    private Integer holeNumber;
    private Integer skinValue;
    private boolean won;
    private boolean carryover;
    private Long winningPlayerId;
    private String winningPlayerName;
    private Integer winningNetStrokes;

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public Integer getSkinValue() {
        return skinValue;
    }

    public void setSkinValue(Integer skinValue) {
        this.skinValue = skinValue;
    }

    public boolean isWon() {
        return won;
    }

    public void setWon(boolean won) {
        this.won = won;
    }

    public boolean isCarryover() {
        return carryover;
    }

    public void setCarryover(boolean carryover) {
        this.carryover = carryover;
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

    public Integer getWinningNetStrokes() {
        return winningNetStrokes;
    }

    public void setWinningNetStrokes(Integer winningNetStrokes) {
        this.winningNetStrokes = winningNetStrokes;
    }
}