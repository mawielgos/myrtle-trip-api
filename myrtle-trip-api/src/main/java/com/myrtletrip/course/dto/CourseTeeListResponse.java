package com.myrtletrip.course.dto;

import java.math.BigDecimal;

public class CourseTeeListResponse {

    private Long courseTeeId;
    private Long courseId;
    private String teeName;
    private BigDecimal courseRating;
    private Integer slope;
    private Integer parTotal;

    public Long getCourseTeeId() {
        return courseTeeId;
    }

    public void setCourseTeeId(Long courseTeeId) {
        this.courseTeeId = courseTeeId;
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
}
