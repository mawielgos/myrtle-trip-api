package com.myrtletrip.games.dto;

public class HoleGameResult {

    private Integer holeNumber;
    private Integer grossScore;
    private Integer netScore;
    private Integer points = 0;

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public Integer getGrossScore() {
        return grossScore;
    }

    public void setGrossScore(Integer grossScore) {
        this.grossScore = grossScore;
    }

    public Integer getNetScore() {
        return netScore;
    }

    public void setNetScore(Integer netScore) {
        this.netScore = netScore;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }
}