package com.myrtletrip.tournament.dto;

import java.util.ArrayList;
import java.util.List;

public class TripTournamentSetupResponse {
    private Long tournamentId;
    private Long tripId;
    private Boolean enabled;
    private String name;
    private String standingsLabel;
    private Boolean readOnly;
    private List<TripTournamentRoundResponse> rounds = new ArrayList<TripTournamentRoundResponse>();

    public Long getTournamentId() { return tournamentId; }
    public void setTournamentId(Long tournamentId) { this.tournamentId = tournamentId; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStandingsLabel() { return standingsLabel; }
    public void setStandingsLabel(String standingsLabel) { this.standingsLabel = standingsLabel; }
    public Boolean getReadOnly() { return readOnly; }
    public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }
    public List<TripTournamentRoundResponse> getRounds() { return rounds; }
    public void setRounds(List<TripTournamentRoundResponse> rounds) { this.rounds = rounds; }
}
