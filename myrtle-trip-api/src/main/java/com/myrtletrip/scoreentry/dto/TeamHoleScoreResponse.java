package com.myrtletrip.scoreentry.dto;

public class TeamHoleScoreResponse {

    private Long roundTeamId;
    private Integer teamNumber;
    private String teamName;
    private Integer holeNumber;
    private Integer strokes;

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

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public Integer getStrokes() {
        return strokes;
    }

    public void setStrokes(Integer strokes) {
        this.strokes = strokes;
    }
}
