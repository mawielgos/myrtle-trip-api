package com.myrtletrip.trip.dto;

import java.time.LocalDate;

public class TripRoundListResponse {

    private Long roundId;
    private Integer roundNumber;
    private LocalDate roundDate;
    private String courseName;
    private String teeName;
    private String gameFormat;
    private Boolean finalized;

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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeeName() {
        return teeName;
    }

    public void setTeeName(String teeName) {
        this.teeName = teeName;
    }

    public String getGameFormat() {
        return gameFormat;
    }

    public void setGameFormat(String gameFormat) {
        this.gameFormat = gameFormat;
    }

    public Boolean getFinalized() {
        return finalized;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }
}
