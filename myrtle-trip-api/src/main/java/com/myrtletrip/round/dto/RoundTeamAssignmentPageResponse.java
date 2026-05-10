package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundTeamAssignmentPageResponse {

    private Long roundId;
    private Long defaultRoundTeeId;
    private Integer scrambleTeamSize;
    private String scrambleSeedingMethod;
    private java.time.LocalDate seedingAsOfDate;
    private String seedingLabel;
    private List<RoundScrambleSeedingRoundResponse> scrambleSeedingRounds = new ArrayList<>();
    private List<RoundTeeOptionResponse> teeOptions = new ArrayList<>();
    private List<RoundTeamResponse> teams = new ArrayList<>();
    private List<RoundTeamPlayerResponse> unassignedPlayers = new ArrayList<>();

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    public Long getDefaultRoundTeeId() { return defaultRoundTeeId; }
    public void setDefaultRoundTeeId(Long defaultRoundTeeId) { this.defaultRoundTeeId = defaultRoundTeeId; }

    public Integer getScrambleTeamSize() { return scrambleTeamSize; }
    public void setScrambleTeamSize(Integer scrambleTeamSize) { this.scrambleTeamSize = scrambleTeamSize; }

    public String getScrambleSeedingMethod() { return scrambleSeedingMethod; }
    public void setScrambleSeedingMethod(String scrambleSeedingMethod) { this.scrambleSeedingMethod = scrambleSeedingMethod; }

    public java.time.LocalDate getSeedingAsOfDate() { return seedingAsOfDate; }
    public void setSeedingAsOfDate(java.time.LocalDate seedingAsOfDate) { this.seedingAsOfDate = seedingAsOfDate; }

    public String getSeedingLabel() { return seedingLabel; }
    public void setSeedingLabel(String seedingLabel) { this.seedingLabel = seedingLabel; }

    public List<RoundScrambleSeedingRoundResponse> getScrambleSeedingRounds() { return scrambleSeedingRounds; }
    public void setScrambleSeedingRounds(List<RoundScrambleSeedingRoundResponse> scrambleSeedingRounds) { this.scrambleSeedingRounds = scrambleSeedingRounds; }

    public List<RoundTeeOptionResponse> getTeeOptions() { return teeOptions; }
    public void setTeeOptions(List<RoundTeeOptionResponse> teeOptions) { this.teeOptions = teeOptions; }

    public List<RoundTeamResponse> getTeams() { return teams; }
    public void setTeams(List<RoundTeamResponse> teams) { this.teams = teams; }

    public List<RoundTeamPlayerResponse> getUnassignedPlayers() { return unassignedPlayers; }
    public void setUnassignedPlayers(List<RoundTeamPlayerResponse> unassignedPlayers) { this.unassignedPlayers = unassignedPlayers; }
}
