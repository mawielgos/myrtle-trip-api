package com.myrtletrip.standings.dto;

import java.util.ArrayList;
import java.util.List;

public class TournamentStandingsResponse {

    private Long tripId;
    private String tripName;
    private String tournamentName;
    private String standingsLabel;
    private Integer completedRounds;
    private Integer requiredRounds;
    private Boolean leaderboardFinal;
    private Integer leaderboardParTotal;
    private List<String> roundLabels = new ArrayList<>();
    private List<TournamentStandingRowResponse> rows = new ArrayList<>();

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

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public String getStandingsLabel() {
        return standingsLabel;
    }

    public void setStandingsLabel(String standingsLabel) {
        this.standingsLabel = standingsLabel;
    }

    public Integer getCompletedRounds() {
        return completedRounds;
    }

    public void setCompletedRounds(Integer completedRounds) {
        this.completedRounds = completedRounds;
    }

    public Integer getRequiredRounds() {
        return requiredRounds;
    }

    public void setRequiredRounds(Integer requiredRounds) {
        this.requiredRounds = requiredRounds;
    }

    public Boolean getLeaderboardFinal() {
        return leaderboardFinal;
    }

    public void setLeaderboardFinal(Boolean leaderboardFinal) {
        this.leaderboardFinal = leaderboardFinal;
    }

    public Integer getLeaderboardParTotal() {
        return leaderboardParTotal;
    }

    public void setLeaderboardParTotal(Integer leaderboardParTotal) {
        this.leaderboardParTotal = leaderboardParTotal;
    }

    public List<String> getRoundLabels() {
        return roundLabels;
    }

    public void setRoundLabels(List<String> roundLabels) {
        this.roundLabels = roundLabels;
    }

    public List<TournamentStandingRowResponse> getRows() {
        return rows;
    }

    public void setRows(List<TournamentStandingRowResponse> rows) {
        this.rows = rows;
    }
}
