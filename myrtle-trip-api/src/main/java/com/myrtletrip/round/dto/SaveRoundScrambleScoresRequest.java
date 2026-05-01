package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class SaveRoundScrambleScoresRequest {

    private String entryMode;
    private List<SaveRoundScrambleTeamScoreRequest> teams = new ArrayList<>();

    public String getEntryMode() {
        return entryMode;
    }

    public void setEntryMode(String entryMode) {
        this.entryMode = entryMode;
    }

    public List<SaveRoundScrambleTeamScoreRequest> getTeams() {
        return teams;
    }

    public void setTeams(List<SaveRoundScrambleTeamScoreRequest> teams) {
        this.teams = teams;
    }
}
