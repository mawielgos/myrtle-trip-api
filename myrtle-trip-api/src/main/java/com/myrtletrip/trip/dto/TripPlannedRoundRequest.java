package com.myrtletrip.trip.dto;

import java.time.LocalDate;

public class TripPlannedRoundRequest {

    private Integer roundNumber;
    private LocalDate roundDate;
    private Long courseId;
    private Long standardTeeId;
    private Long alternateTeeId;
    private String format;
    private Boolean includeInFourDayStandings;
    private Boolean includeInScrambleSeeding;

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

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getStandardTeeId() {
        return standardTeeId;
    }

    public void setStandardTeeId(Long standardTeeId) {
        this.standardTeeId = standardTeeId;
    }

    public Long getAlternateTeeId() {
        return alternateTeeId;
    }

    public void setAlternateTeeId(Long alternateTeeId) {
        this.alternateTeeId = alternateTeeId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Boolean getIncludeInFourDayStandings() {
        return includeInFourDayStandings;
    }

    public void setIncludeInFourDayStandings(Boolean includeInFourDayStandings) {
        this.includeInFourDayStandings = includeInFourDayStandings;
    }

    public Boolean getIncludeInScrambleSeeding() {
        return includeInScrambleSeeding;
    }

    public void setIncludeInScrambleSeeding(Boolean includeInScrambleSeeding) {
        this.includeInScrambleSeeding = includeInScrambleSeeding;
    }
}
