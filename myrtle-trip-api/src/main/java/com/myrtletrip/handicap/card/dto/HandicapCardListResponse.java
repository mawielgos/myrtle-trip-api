package com.myrtletrip.handicap.card.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HandicapCardListResponse {

    private Long tripId;
    private String tripName;
    private String tripCode;
    private Integer tripYear;
    private LocalDate asOfDate;
    private List<HandicapCardPlayerSummaryResponse> players = new ArrayList<>();

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

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public List<HandicapCardPlayerSummaryResponse> getPlayers() {
        return players;
    }

    public void setPlayers(List<HandicapCardPlayerSummaryResponse> players) {
        this.players = players;
    }
}
