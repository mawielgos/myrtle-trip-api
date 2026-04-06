package com.myrtletrip.round.dto;

import java.util.List;

public class RoundTeamResponse {

    private Long roundTeamId;
    private Integer teamNumber;
    private String teamName;
    private List<RoundTeamPlayerResponse> players;

    public Long getRoundTeamId() {
        return roundTeamId;
    }

    public void setRoundTeamId(Long roundTeamId) {
        this.roundTeamId = roundTeamId;
    }

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

    public List<RoundTeamPlayerResponse> getPlayers() {
        return players;
    }

    public void setPlayers(List<RoundTeamPlayerResponse> players) {
        this.players = players;
    }
}
