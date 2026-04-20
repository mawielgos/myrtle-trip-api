package com.myrtletrip.round.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RoundStatusResponse {

    private Long roundId;
    private Long tripId;
    private String courseName;
    private String teeName;
    private String alternateTeeName;
    private String format;
    private LocalDate roundDate;
    private Boolean finalized;
    private List<RoundPlayerStatusResponse> players = new ArrayList<>();

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
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

    public String getAlternateTeeName() {
        return alternateTeeName;
    }

    public void setAlternateTeeName(String alternateTeeName) {
        this.alternateTeeName = alternateTeeName;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public LocalDate getRoundDate() {
        return roundDate;
    }

    public void setRoundDate(LocalDate roundDate) {
        this.roundDate = roundDate;
    }

    public Boolean getFinalized() {
        return finalized;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }

    public List<RoundPlayerStatusResponse> getPlayers() {
        return players;
    }

    public void setPlayers(List<RoundPlayerStatusResponse> players) {
        this.players = players;
    }
}
