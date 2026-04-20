package com.myrtletrip.course.dto;

import java.math.BigDecimal;

public class CourseTeeResponse {

    private Long teeId;
    private Long courseId;
    private String teeName;
    private BigDecimal courseRating;
    private Integer slope;
    private Integer parTotal;
    private Boolean active;

    public CourseTeeResponse() {
    }

    public CourseTeeResponse(
            Long teeId,
            Long courseId,
            String teeName,
            BigDecimal courseRating,
            Integer slope,
            Integer parTotal,
            Boolean active) {
        this.teeId = teeId;
        this.courseId = courseId;
        this.teeName = teeName;
        this.courseRating = courseRating;
        this.slope = slope;
        this.parTotal = parTotal;
        this.active = active;
    }

    public Long getTeeId() {
        return teeId;
    }

    public void setTeeId(Long teeId) {
        this.teeId = teeId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getTeeName() {
        return teeName;
    }

    public void setTeeName(String teeName) {
        this.teeName = teeName;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
