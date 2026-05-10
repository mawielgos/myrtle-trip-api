package com.myrtletrip.round.dto;

import java.time.LocalDate;

public class RoundScrambleSeedingRoundResponse {

    private Long plannedRoundId;
    private Integer roundNumber;
    private LocalDate roundDate;
    private String format;
    private String courseName;
    private Boolean included;
    private Boolean eligible;

    public Long getPlannedRoundId() { return plannedRoundId; }
    public void setPlannedRoundId(Long plannedRoundId) { this.plannedRoundId = plannedRoundId; }

    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }

    public LocalDate getRoundDate() { return roundDate; }
    public void setRoundDate(LocalDate roundDate) { this.roundDate = roundDate; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Boolean getIncluded() { return included; }
    public void setIncluded(Boolean included) { this.included = included; }

    public Boolean getEligible() { return eligible; }
    public void setEligible(Boolean eligible) { this.eligible = eligible; }
}
