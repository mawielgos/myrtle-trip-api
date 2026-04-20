package com.myrtletrip.trip.dto;

import java.time.LocalDate;

public class TripPlannedRoundResponse {

    private Long plannedRoundId;
    private Integer roundNumber;
    private LocalDate roundDate;
    private Long courseId;
    private Long standardTeeId;
    private Long alternateTeeId;
    private String courseName;
    private String standardTeeDisplay;
    private String alternateTeeDisplay;
    private String format;
    private Boolean includeInFourDayStandings;
    private Boolean includeInScrambleSeeding;

    public Long getPlannedRoundId() {
        return plannedRoundId;
    }

    public void setPlannedRoundId(Long plannedRoundId) {
        this.plannedRoundId = plannedRoundId;
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getStandardTeeDisplay() {
        return standardTeeDisplay;
    }

    public void setStandardTeeDisplay(String standardTeeDisplay) {
        this.standardTeeDisplay = standardTeeDisplay;
    }

    public String getAlternateTeeDisplay() {
        return alternateTeeDisplay;
    }

    public void setAlternateTeeDisplay(String alternateTeeDisplay) {
        this.alternateTeeDisplay = alternateTeeDisplay;
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
