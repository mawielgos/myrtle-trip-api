package com.myrtletrip.games.model;

import com.myrtletrip.round.model.RoundFormat;

import java.util.ArrayList;
import java.util.List;

public class RoundScoringData {

    private Long roundId;
    private RoundFormat format;
    private List<TeamScoringData> teams = new ArrayList<>();

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public RoundFormat getFormat() {
        return format;
    }

    public void setFormat(RoundFormat format) {
        this.format = format;
    }

    public List<TeamScoringData> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamScoringData> teams) {
        this.teams = teams;
    }
}
