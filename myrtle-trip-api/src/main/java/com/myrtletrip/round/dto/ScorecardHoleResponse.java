package com.myrtletrip.round.dto;

public class ScorecardHoleResponse {

    private Integer holeNumber;
    private Integer par;
    private Integer handicap;
    private Integer strokes;
    private Integer netStrokes;
    private Integer adjustedStrokes;

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public Integer getPar() {
        return par;
    }

    public void setPar(Integer par) {
        this.par = par;
    }

    public Integer getHandicap() {
        return handicap;
    }

    public void setHandicap(Integer handicap) {
        this.handicap = handicap;
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
