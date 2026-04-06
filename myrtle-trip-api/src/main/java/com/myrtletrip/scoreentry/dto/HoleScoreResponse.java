package com.myrtletrip.scoreentry.dto;

public class HoleScoreResponse {

    private Integer holeNumber;
    private Integer strokes;
    private Integer netStrokes;
    private Integer adjustedStrokes;

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public Integer getStrokes() {
        return strokes;
    }

    public void setStrokes(Integer strokes) {
        this.strokes = strokes;
    }

    public Integer getNetStrokes() {
        return netStrokes;
    }

    public void setNetStrokes(Integer netStrokes) {
        this.netStrokes = netStrokes;
    }

    public Integer getAdjustedStrokes() {
        return adjustedStrokes;
    }

    public void setAdjustedStrokes(Integer adjustedStrokes) {
        this.adjustedStrokes = adjustedStrokes;
    }
}