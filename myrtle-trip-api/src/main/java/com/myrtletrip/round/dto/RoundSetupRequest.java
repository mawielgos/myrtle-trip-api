package com.myrtletrip.round.dto;

import com.myrtletrip.round.model.RoundFormat;

import java.time.LocalDate;

public class RoundSetupRequest {

    private Long tripId;
    private Long courseId;
    private Long standardCourseTeeId;
    private Long alternateCourseTeeId;
    private LocalDate roundDate;
    private RoundFormat format;
    private Integer handicapPercent = 100;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getStandardCourseTeeId() {
        return standardCourseTeeId;
    }

    public void setStandardCourseTeeId(Long standardCourseTeeId) {
        this.standardCourseTeeId = standardCourseTeeId;
    }

    public Long getAlternateCourseTeeId() {
        return alternateCourseTeeId;
    }

    public void setAlternateCourseTeeId(Long alternateCourseTeeId) {
        this.alternateCourseTeeId = alternateCourseTeeId;
    }

    public LocalDate getRoundDate() {
        return roundDate;
    }

    public void setRoundDate(LocalDate roundDate) {
        this.roundDate = roundDate;
    }

    public RoundFormat getFormat() {
        return format;
    }

    public void setFormat(RoundFormat format) {
        this.format = format;
    }

    public Integer getHandicapPercent() {
        return handicapPercent;
    }

    public void setHandicapPercent(Integer handicapPercent) {
        this.handicapPercent = handicapPercent;
    }
}
