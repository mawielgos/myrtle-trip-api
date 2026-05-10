package com.myrtletrip.trip.dto;

import java.time.LocalDate;

public class TripPlannedRoundResponse {

    private Long plannedRoundId;
    private Integer roundNumber;
    private LocalDate roundDate;
    private Long courseId;
    private Long defaultTeeId;
    private Long womenDefaultTeeId;
    private String courseName;
    private String defaultTeeDisplay;
    private String womenDefaultTeeDisplay;
    private String format;
    private Boolean includeInFourDayStandings;
    private Integer scrambleTeamSize;

    public Long getPlannedRoundId() { return plannedRoundId; }
    public void setPlannedRoundId(Long plannedRoundId) { this.plannedRoundId = plannedRoundId; }
    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }
    public LocalDate getRoundDate() { return roundDate; }
    public void setRoundDate(LocalDate roundDate) { this.roundDate = roundDate; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getDefaultTeeId() { return defaultTeeId; }
    public void setDefaultTeeId(Long defaultTeeId) { this.defaultTeeId = defaultTeeId; }
    public Long getWomenDefaultTeeId() { return womenDefaultTeeId; }
    public void setWomenDefaultTeeId(Long womenDefaultTeeId) { this.womenDefaultTeeId = womenDefaultTeeId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getDefaultTeeDisplay() { return defaultTeeDisplay; }
    public void setDefaultTeeDisplay(String defaultTeeDisplay) { this.defaultTeeDisplay = defaultTeeDisplay; }
    public String getWomenDefaultTeeDisplay() { return womenDefaultTeeDisplay; }
    public void setWomenDefaultTeeDisplay(String womenDefaultTeeDisplay) { this.womenDefaultTeeDisplay = womenDefaultTeeDisplay; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Boolean getIncludeInFourDayStandings() { return includeInFourDayStandings; }
    public void setIncludeInFourDayStandings(Boolean includeInFourDayStandings) { this.includeInFourDayStandings = includeInFourDayStandings; }
    public Integer getScrambleTeamSize() { return scrambleTeamSize; }
    public void setScrambleTeamSize(Integer scrambleTeamSize) { this.scrambleTeamSize = scrambleTeamSize; }
}
