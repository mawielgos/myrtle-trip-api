package com.myrtletrip.round.dto;

import java.time.LocalDate;

import com.myrtletrip.round.model.RoundFormat;

public class RoundSetupRequest {

    private Long tripId;
    private Long courseId;
    private Long courseTeeId;
    private LocalDate roundDate;
    private RoundFormat format;
    
    public RoundFormat getFormat() {
        return format;
    }

    public void setFormat(RoundFormat format) {
        this.format = format;
    }
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

    public Long getCourseTeeId() {
        return courseTeeId;
    }

    public void setCourseTeeId(Long courseTeeId) {
        this.courseTeeId = courseTeeId;
    }

    public LocalDate getRoundDate() {
        return roundDate;
    }

    public void setRoundDate(LocalDate roundDate) {
        this.roundDate = roundDate;
    }
}