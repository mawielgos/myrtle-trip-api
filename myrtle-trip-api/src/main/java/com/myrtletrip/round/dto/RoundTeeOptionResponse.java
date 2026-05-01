package com.myrtletrip.round.dto;

import java.math.BigDecimal;

public class RoundTeeOptionResponse {

    private Long roundTeeId;
    private Long sourceCourseTeeId;
    private String teeName;

    private String displayName;
    private String displayNameForMen;
    private String displayNameForWomen;

    private Boolean eligibleForMen;
    private Boolean eligibleForWomen;

    private BigDecimal menCourseRating;
    private Integer menSlope;
    private Integer menParTotal;

    private BigDecimal womenCourseRating;
    private Integer womenSlope;
    private Integer womenParTotal;

    public Long getRoundTeeId() {
        return roundTeeId;
    }

    public void setRoundTeeId(Long roundTeeId) {
        this.roundTeeId = roundTeeId;
    }

    public Long getSourceCourseTeeId() {
        return sourceCourseTeeId;
    }

    public void setSourceCourseTeeId(Long sourceCourseTeeId) {
        this.sourceCourseTeeId = sourceCourseTeeId;
    }

    public String getTeeName() {
        return teeName;
    }

    public void setTeeName(String teeName) {
        this.teeName = teeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayNameForMen() {
        return displayNameForMen;
    }

    public void setDisplayNameForMen(String displayNameForMen) {
        this.displayNameForMen = displayNameForMen;
    }

    public String getDisplayNameForWomen() {
        return displayNameForWomen;
    }

    public void setDisplayNameForWomen(String displayNameForWomen) {
        this.displayNameForWomen = displayNameForWomen;
    }

    public Boolean getEligibleForMen() {
        return eligibleForMen;
    }

    public void setEligibleForMen(Boolean eligibleForMen) {
        this.eligibleForMen = eligibleForMen;
    }

    public Boolean getEligibleForWomen() {
        return eligibleForWomen;
    }

    public void setEligibleForWomen(Boolean eligibleForWomen) {
        this.eligibleForWomen = eligibleForWomen;
    }

    public BigDecimal getMenCourseRating() {
        return menCourseRating;
    }

    public void setMenCourseRating(BigDecimal menCourseRating) {
        this.menCourseRating = menCourseRating;
    }

    public Integer getMenSlope() {
        return menSlope;
    }

    public void setMenSlope(Integer menSlope) {
        this.menSlope = menSlope;
    }

    public Integer getMenParTotal() {
        return menParTotal;
    }

    public void setMenParTotal(Integer menParTotal) {
        this.menParTotal = menParTotal;
    }

    public BigDecimal getWomenCourseRating() {
        return womenCourseRating;
    }

    public void setWomenCourseRating(BigDecimal womenCourseRating) {
        this.womenCourseRating = womenCourseRating;
    }

    public Integer getWomenSlope() {
        return womenSlope;
    }

    public void setWomenSlope(Integer womenSlope) {
        this.womenSlope = womenSlope;
    }

    public Integer getWomenParTotal() {
        return womenParTotal;
    }

    public void setWomenParTotal(Integer womenParTotal) {
        this.womenParTotal = womenParTotal;
    }
}