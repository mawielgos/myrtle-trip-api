package com.myrtletrip.tournament.dto;

import java.time.LocalDate;

public class TripTournamentRoundResponse {
    private Long plannedRoundId;
    private Integer roundNumber;
    private LocalDate roundDate;
    private String format;
    private Long courseId;
    private String courseName;
    private Boolean configured;
    private Boolean included;
    private Integer sortOrder;

    public Long getPlannedRoundId() { return plannedRoundId; }
    public void setPlannedRoundId(Long plannedRoundId) { this.plannedRoundId = plannedRoundId; }
    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }
    public LocalDate getRoundDate() { return roundDate; }
    public void setRoundDate(LocalDate roundDate) { this.roundDate = roundDate; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public Boolean getConfigured() { return configured; }
    public void setConfigured(Boolean configured) { this.configured = configured; }
    public Boolean getIncluded() { return included; }
    public void setIncluded(Boolean included) { this.included = included; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
