package com.myrtletrip.strokes.dto;

import java.util.ArrayList;
import java.util.List;

public class StrokesPerDayResponse {

    private Long tripId;
    private String tripName;
    private String tripCode;
    private Integer tripYear;
    private List<StrokesPerDayRoundResponse> rounds = new ArrayList<>();
    private List<StrokesPerDayPlayerResponse> players = new ArrayList<>();

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getTripName() {
        return tripName;
    }

    public void setTripName(String tripName) {
        this.tripName = tripName;
    }

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }

    public Integer getTripYear() {
        return tripYear;
    }

    public void setTripYear(Integer tripYear) {
        this.tripYear = tripYear;
    }

    public List<StrokesPerDayRoundResponse> getRounds() {
        return rounds;
    }

    public void setRounds(List<StrokesPerDayRoundResponse> rounds) {
        this.rounds = rounds;
    }

    public List<StrokesPerDayPlayerResponse> getPlayers() {
        return players;
    }

    public void setPlayers(List<StrokesPerDayPlayerResponse> players) {
        this.players = players;
    }
}
