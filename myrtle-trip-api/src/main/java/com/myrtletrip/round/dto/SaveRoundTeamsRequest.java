package com.myrtletrip.round.dto;

import java.util.List;

public class SaveRoundTeamsRequest {

    private List<RoundTeamRequest> teams;

    public List<RoundTeamRequest> getTeams() {
        return teams;
    }

    public void setTeams(List<RoundTeamRequest> teams) {
        this.teams = teams;
    }
}
