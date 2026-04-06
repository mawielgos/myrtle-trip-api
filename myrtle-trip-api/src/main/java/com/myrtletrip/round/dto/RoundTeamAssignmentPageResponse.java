package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundTeamAssignmentPageResponse {

    private Long roundId;
    private List<RoundTeamResponse> teams = new ArrayList<>();
    private List<RoundTeamPlayerResponse> unassignedPlayers = new ArrayList<>();

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public List<RoundTeamResponse> getTeams() {
        return teams;
    }

    public void setTeams(List<RoundTeamResponse> teams) {
        this.teams = teams;
    }

    public List<RoundTeamPlayerResponse> getUnassignedPlayers() {
        return unassignedPlayers;
    }

    public void setUnassignedPlayers(List<RoundTeamPlayerResponse> unassignedPlayers) {
        this.unassignedPlayers = unassignedPlayers;
    }
}
