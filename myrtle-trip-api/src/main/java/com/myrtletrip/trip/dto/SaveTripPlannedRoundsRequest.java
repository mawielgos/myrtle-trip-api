package com.myrtletrip.trip.dto;

import java.util.List;

public class SaveTripPlannedRoundsRequest {

    private List<TripPlannedRoundRequest> rounds;

    public List<TripPlannedRoundRequest> getRounds() {
        return rounds;
    }

    public void setRounds(List<TripPlannedRoundRequest> rounds) {
        this.rounds = rounds;
    }
}
