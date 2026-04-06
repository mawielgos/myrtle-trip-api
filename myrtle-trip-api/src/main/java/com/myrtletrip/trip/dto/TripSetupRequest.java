package com.myrtletrip.trip.dto;

import java.util.List;

public class TripSetupRequest {

    private String tripCode;
    private String name;
    private Integer tripYear;
    private List<Long> playerIds;

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTripYear() {
        return tripYear;
    }

    public void setTripYear(Integer tripYear) {
        this.tripYear = tripYear;
    }

    public List<Long> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<Long> playerIds) {
        this.playerIds = playerIds;
    }
}