package com.myrtletrip.round.dto;

import com.myrtletrip.round.model.RoundFormat;

import java.time.LocalDate;

public class RoundSetupRequest {

    private Long tripId;
    private Long courseId;
    private Long defaultCourseTeeId;
    private LocalDate roundDate;
    private RoundFormat format;
    private Integer handicapPercent = 100;

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getDefaultCourseTeeId() { return defaultCourseTeeId; }
    public void setDefaultCourseTeeId(Long defaultCourseTeeId) { this.defaultCourseTeeId = defaultCourseTeeId; }

    /** Backward-compatible JSON/property alias for older UI payloads. */
    @Deprecated
    public Long getStandardCourseTeeId() { return defaultCourseTeeId; }

    /** Backward-compatible JSON/property alias for older UI payloads. */
    @Deprecated
    public void setStandardCourseTeeId(Long standardCourseTeeId) { this.defaultCourseTeeId = standardCourseTeeId; }

    /** Alternate tee is no longer configured at round setup. */
    @Deprecated
    public Long getAlternateCourseTeeId() { return null; }

    /** Alternate tee is no longer configured at round setup. */
    @Deprecated
    public void setAlternateCourseTeeId(Long alternateCourseTeeId) { }

    public LocalDate getRoundDate() { return roundDate; }
    public void setRoundDate(LocalDate roundDate) { this.roundDate = roundDate; }

    public RoundFormat getFormat() { return format; }
    public void setFormat(RoundFormat format) { this.format = format; }

    public Integer getHandicapPercent() { return handicapPercent; }
    public void setHandicapPercent(Integer handicapPercent) { this.handicapPercent = handicapPercent; }
}
