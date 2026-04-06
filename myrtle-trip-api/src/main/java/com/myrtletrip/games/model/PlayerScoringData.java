package com.myrtletrip.games.model;

import java.util.ArrayList;
import java.util.List;

public class PlayerScoringData {

    private Long playerId;
    private String playerName;
    private Long scorecardId;
    private Integer courseHandicap;
    private Integer playingHandicap;
    private List<PlayerHoleScoringData> holes = new ArrayList<>();

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

    public Long getScorecardId() {
        return scorecardId;
    }

    public void setScorecardId(Long scorecardId) {
        this.scorecardId = scorecardId;
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

    public List<PlayerHoleScoringData> getHoles() {
        return holes;
    }

    public void setHoles(List<PlayerHoleScoringData> holes) {
        this.holes = holes;
    }
}
