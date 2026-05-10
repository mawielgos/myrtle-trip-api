package com.myrtletrip.round.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RoundStatusResponse {

    private Long roundId;
    private Long tripId;
    private String courseName;
    private String teeName;
    private String format;
    private Integer scrambleTeamSize;
    private LocalDate roundDate;
    private Boolean finalized;
    private String tripStatus;
    private Boolean tripCorrectionMode;
    private Boolean tripLocked;
    private Boolean editable;
    private List<RoundPlayerStatusResponse> players = new ArrayList<>();

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getTeeName() { return teeName; }
    public void setTeeName(String teeName) { this.teeName = teeName; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Integer getScrambleTeamSize() { return scrambleTeamSize; }
    public void setScrambleTeamSize(Integer scrambleTeamSize) { this.scrambleTeamSize = scrambleTeamSize; }
    public LocalDate getRoundDate() { return roundDate; }
    public void setRoundDate(LocalDate roundDate) { this.roundDate = roundDate; }
    public Boolean getFinalized() { return finalized; }
    public void setFinalized(Boolean finalized) { this.finalized = finalized; }
    public String getTripStatus() { return tripStatus; }
    public void setTripStatus(String tripStatus) { this.tripStatus = tripStatus; }
    public Boolean getTripCorrectionMode() { return tripCorrectionMode; }
    public void setTripCorrectionMode(Boolean tripCorrectionMode) { this.tripCorrectionMode = tripCorrectionMode; }
    public Boolean getTripLocked() { return tripLocked; }
    public void setTripLocked(Boolean tripLocked) { this.tripLocked = tripLocked; }
    public Boolean getEditable() { return editable; }
    public void setEditable(Boolean editable) { this.editable = editable; }
    public List<RoundPlayerStatusResponse> getPlayers() { return players; }
    public void setPlayers(List<RoundPlayerStatusResponse> players) { this.players = players; }
}
