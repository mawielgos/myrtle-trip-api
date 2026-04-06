package com.myrtletrip.games.model;

import java.util.ArrayList;
import java.util.List;

public class TeamScoringData {

    private Long teamId;
    private String teamName;
    private List<PlayerScoringData> players = new ArrayList<>();
    private List<TeamHoleScoringData> scrambleHoleScores = new ArrayList<>();

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public List<PlayerScoringData> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerScoringData> players) {
        this.players = players;
    }

    public List<TeamHoleScoringData> getScrambleHoleScores() {
        return scrambleHoleScores;
    }

    public void setScrambleHoleScores(List<TeamHoleScoringData> scrambleHoleScores) {
        this.scrambleHoleScores = scrambleHoleScores;
    }
}
