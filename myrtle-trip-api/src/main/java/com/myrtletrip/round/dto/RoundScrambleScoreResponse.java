package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundScrambleScoreResponse {

    private Long roundId;
    private List<RoundScrambleTeamScoreResponse> teams = new ArrayList<>();

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public List<RoundScrambleTeamScoreResponse> getTeams() {
        return teams;
    }

    public void setTeams(List<RoundScrambleTeamScoreResponse> teams) {
        this.teams = teams;
    }
}
