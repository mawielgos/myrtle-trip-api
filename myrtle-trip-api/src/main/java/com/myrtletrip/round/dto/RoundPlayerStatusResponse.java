package com.myrtletrip.round.dto;

public class RoundPlayerStatusResponse {

    private Long scorecardId;
    private Long playerId;
    private String playerName;
    private String gender;

    private Boolean useAlternateTee;

    private Long roundTeeId;
    private String roundTeeName;

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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Boolean getUseAlternateTee() {
        return useAlternateTee;
    }

    public void setUseAlternateTee(Boolean useAlternateTee) {
        this.useAlternateTee = useAlternateTee;
    }

    public Long getRoundTeeId() {
        return roundTeeId;
    }

    public void setRoundTeeId(Long roundTeeId) {
        this.roundTeeId = roundTeeId;
    }

    public String getRoundTeeName() {
        return roundTeeName;
    }

    public void setRoundTeeName(String roundTeeName) {
        this.roundTeeName = roundTeeName;
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
