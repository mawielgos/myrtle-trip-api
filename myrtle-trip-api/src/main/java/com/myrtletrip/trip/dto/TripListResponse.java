package com.myrtletrip.trip.dto;

public class TripListResponse {

    private Long tripId;
    private String tripName;
    private String tripCode;
    private Integer tripYear;
    private Long playerCount;
    private Long roundCount;

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

    public Long getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(Long playerCount) {
        this.playerCount = playerCount;
    }

    public Long getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(Long roundCount) {
        this.roundCount = roundCount;
    }
}
