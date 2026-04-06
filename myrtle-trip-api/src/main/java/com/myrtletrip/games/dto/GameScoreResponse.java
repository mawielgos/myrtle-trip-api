package com.myrtletrip.games.dto;

import java.util.List;

public class GameScoreResponse {

    private Long roundId;
    private String format;
    private List<TeamGameResultResponse> teams;

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<TeamGameResultResponse> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamGameResultResponse> teams) {
        this.teams = teams;
    }
}
