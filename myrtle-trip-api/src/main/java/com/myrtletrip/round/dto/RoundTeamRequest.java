package com.myrtletrip.round.dto;

import java.util.List;

public class RoundTeamRequest {

    private Integer teamNumber;
    private String teamName;
    private List<RoundTeamPlayerRequest> players;

    public Integer getTeamNumber() {
        return teamNumber;
    }

    public void setTeamNumber(Integer teamNumber) {
        this.teamNumber = teamNumber;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public List<RoundTeamPlayerRequest> getPlayers() {
        return players;
    }

    public void setPlayers(List<RoundTeamPlayerRequest> players) {
        this.players = players;
    }
}
