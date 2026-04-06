package com.myrtletrip.round.dto;

public class RoundPlayerStatusResponse {

    private Long scorecardId;
    private Long playerId;
    private String playerName;
    private Integer courseHandicap;
    private Integer playingHandicap;

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
}
