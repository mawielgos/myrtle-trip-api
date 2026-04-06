package com.myrtletrip.games.dto;

import com.myrtletrip.round.model.RoundFormat;

import java.util.ArrayList;
import java.util.List;

public class RoundGameResult {

    private Long roundId;
    private RoundFormat format;
    private List<TeamGameResult> teams = new ArrayList<>();

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

    public List<TeamGameResult> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamGameResult> teams) {
        this.teams = teams;
    }
}