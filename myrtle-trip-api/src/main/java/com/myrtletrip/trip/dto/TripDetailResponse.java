package com.myrtletrip.trip.dto;

public class TripDetailResponse {

    private Long tripId;
    private String tripName;
    private String tripCode;
    private Integer tripYear;

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
}
