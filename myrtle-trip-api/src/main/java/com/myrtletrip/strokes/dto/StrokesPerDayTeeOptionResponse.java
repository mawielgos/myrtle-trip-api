package com.myrtletrip.strokes.dto;

import java.math.BigDecimal;

public class StrokesPerDayTeeOptionResponse {

    private Long roundTeeId;
    private Long sourceCourseTeeId;
    private String teeName;
    private String displayName;
    private BigDecimal courseRating;
    private Integer slope;
    private Integer parTotal;
    private Integer yardage;
    private Integer courseHandicap;
    private Integer playingHandicap;
    private Boolean selected;

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

    public Integer getParTotal() {
        return parTotal;
    }

    public void setParTotal(Integer parTotal) {
        this.parTotal = parTotal;
    }

    public Integer getYardage() {
        return yardage;
    }

    public void setYardage(Integer yardage) {
        this.yardage = yardage;
    }

    public Integer getCourseHandicap() {
        return courseHandicap;
    }

    public void setCourseHandicap(Integer courseHandicap) {
        this.courseHandicap = courseHandicap;
    }

    public Integer getPlayingHandicap() {
        return playingHandicap;
    }

    public void setPlayingHandicap(Integer playingHandicap) {
        this.playingHandicap = playingHandicap;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }
}
