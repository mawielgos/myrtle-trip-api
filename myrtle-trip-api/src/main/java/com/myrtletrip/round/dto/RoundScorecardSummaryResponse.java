package com.myrtletrip.round.dto;

public class RoundScorecardSummaryResponse {

    private Long scorecardId;
    private Long playerId;
    private String playerName;
    private Long teamId;
    private String teamName;
    private Integer courseHandicap;
    private Integer playingHandicap;
    private Integer grossScore;
    private Integer adjustedGrossScore;
    private Integer netScore;
    private String teeName;
    private String alternateTeeName;
    private String currentTeeName;

    public Long getScorecardId() {
        return scorecardId;
    }

    public void setScorecardId(Long scorecardId) {
        this.scorecardId = scorecardId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

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

    public Integer getCourseHandicap() {
        return courseHandicap;
    }

    public void setCourseHandicap(Integer courseHandicap) {
        this.courseHandicap = courseHandicap;
    }

    public Integer getPlayingHandicap() {
        return playingHandicap;
    }

    public void setPlayingHandicap(Integer playingHandicap) {
        this.playingHandicap = playingHandicap;
    }

    public Integer getGrossScore() {
        return grossScore;
    }

    public void setGrossScore(Integer grossScore) {
        this.grossScore = grossScore;
    }

    public Integer getAdjustedGrossScore() {
        return adjustedGrossScore;
    }

    public void setAdjustedGrossScore(Integer adjustedGrossScore) {
        this.adjustedGrossScore = adjustedGrossScore;
    }

    public Integer getNetScore() {
        return netScore;
    }

    public void setNetScore(Integer netScore) {
        this.netScore = netScore;
    }

    public String getTeeName() {
        return teeName;
    }

    public void setTeeName(String teeName) {
        this.teeName = teeName;
    }

    public String getAlternateTeeName() {
        return alternateTeeName;
    }

    public void setAlternateTeeName(String alternateTeeName) {
        this.alternateTeeName = alternateTeeName;
    }

    public String getCurrentTeeName() {
        return currentTeeName;
    }

    public void setCurrentTeeName(String currentTeeName) {
        this.currentTeeName = currentTeeName;
    }
}
