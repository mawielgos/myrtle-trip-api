package com.myrtletrip.strokes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StrokesPerDayRoundResponse {

    private Long roundId;
    private Integer roundNumber;
    private LocalDate roundDate;
    private String dayName;
    private String courseName;
    private String courseWebsiteUrl;
    private String standardTeeName;
    private String alternateTeeName;
    private BigDecimal standardCourseRating;
    private BigDecimal alternateCourseRating;
    private Integer standardSlope;
    private Integer alternateSlope;
    private Integer standardYardage;
    private Integer alternateYardage;
    private String statusCode;
    private String statusLabel;
    private Boolean teePlanningLocked;

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public LocalDate getRoundDate() {
        return roundDate;
    }

    public void setRoundDate(LocalDate roundDate) {
        this.roundDate = roundDate;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseWebsiteUrl() {
        return courseWebsiteUrl;
    }

    public void setCourseWebsiteUrl(String courseWebsiteUrl) {
        this.courseWebsiteUrl = courseWebsiteUrl;
    }

    public String getStandardTeeName() {
        return standardTeeName;
    }

    public void setStandardTeeName(String standardTeeName) {
        this.standardTeeName = standardTeeName;
    }

    public String getAlternateTeeName() {
        return alternateTeeName;
    }

    public void setAlternateTeeName(String alternateTeeName) {
        this.alternateTeeName = alternateTeeName;
    }

    public BigDecimal getStandardCourseRating() {
        return standardCourseRating;
    }

    public void setStandardCourseRating(BigDecimal standardCourseRating) {
        this.standardCourseRating = standardCourseRating;
    }

    public BigDecimal getAlternateCourseRating() {
        return alternateCourseRating;
    }

    public void setAlternateCourseRating(BigDecimal alternateCourseRating) {
        this.alternateCourseRating = alternateCourseRating;
    }

    public Integer getStandardSlope() {
        return standardSlope;
    }

    public void setStandardSlope(Integer standardSlope) {
        this.standardSlope = standardSlope;
    }

    public Integer getAlternateSlope() {
        return alternateSlope;
    }

    public void setAlternateSlope(Integer alternateSlope) {
        this.alternateSlope = alternateSlope;
    }

    public Integer getStandardYardage() {
        return standardYardage;
    }

    public void setStandardYardage(Integer standardYardage) {
        this.standardYardage = standardYardage;
    }

    public Integer getAlternateYardage() {
        return alternateYardage;
    }

    public void setAlternateYardage(Integer alternateYardage) {
        this.alternateYardage = alternateYardage;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public Boolean getTeePlanningLocked() {
        return teePlanningLocked;
    }

    public void setTeePlanningLocked(Boolean teePlanningLocked) {
        this.teePlanningLocked = teePlanningLocked;
    }
}

