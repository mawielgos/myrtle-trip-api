package com.myrtletrip.scoreentry.dto;

import java.util.List;

public class ScorecardResponse {

    private Long scorecardId;
    private Long roundId;
    private Long playerId;
    private Integer playingHandicap;
    private Integer grossScore;
    private Integer netScore;
    private Integer adjustedGrossScore;
    private List<HoleScoreResponse> holes;

    public Long getScorecardId() {
        return scorecardId;
    }

    public void setScorecardId(Long scorecardId) {
        this.scorecardId = scorecardId;
    }

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
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

    public Integer getNetScore() {
        return netScore;
    }

    public void setNetScore(Integer netScore) {
        this.netScore = netScore;
    }

    public Integer getAdjustedGrossScore() {
        return adjustedGrossScore;
    }

    public void setAdjustedGrossScore(Integer adjustedGrossScore) {
        this.adjustedGrossScore = adjustedGrossScore;
    }

    public List<HoleScoreResponse> getHoles() {
        return holes;
    }

    public void setHoles(List<HoleScoreResponse> holes) {
        this.holes = holes;
    }
}