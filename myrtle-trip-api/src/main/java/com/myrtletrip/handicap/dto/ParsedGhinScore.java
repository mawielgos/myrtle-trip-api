package com.myrtletrip.handicap.dto;

import java.math.BigDecimal;

public class ParsedGhinScore {

    private Integer grossScore;
    private Integer holesPlayed;
    private String scoreType;
    private BigDecimal courseRating;
    private Integer slope;
    private BigDecimal differential;
    private Integer displayOrder;
    private Boolean manualDifferentialRequired = false;
    private String rawPeriodText;

    public Integer getGrossScore() {
        return grossScore;
    }

    public void setGrossScore(Integer grossScore) {
        this.grossScore = grossScore;
    }

    public Integer getHolesPlayed() {
        return holesPlayed;
    }

    public void setHolesPlayed(Integer holesPlayed) {
        this.holesPlayed = holesPlayed;
    }

    public String getScoreType() {
        return scoreType;
    }

    public void setScoreType(String scoreType) {
        this.scoreType = scoreType;
    }

    public BigDecimal getCourseRating() {
        return courseRating;
    }

    public void setCourseRating(BigDecimal courseRating) {
        this.courseRating = courseRating;
    }

    public Integer getSlope() {
        return slope;
    }

    public void setSlope(Integer slope) {
        this.slope = slope;
    }

    public BigDecimal getDifferential() {
        return differential;
    }

    public void setDifferential(BigDecimal differential) {
        this.differential = differential;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getManualDifferentialRequired() {
        return manualDifferentialRequired;
    }

    public void setManualDifferentialRequired(Boolean manualDifferentialRequired) {
        this.manualDifferentialRequired = manualDifferentialRequired;
    }

    public String getRawPeriodText() {
        return rawPeriodText;
    }

    public void setRawPeriodText(String rawPeriodText) {
        this.rawPeriodText = rawPeriodText;
    }
}