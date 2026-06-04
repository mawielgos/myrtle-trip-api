package com.myrtletrip.round.exceptionmodel.dto;

import com.myrtletrip.round.exceptionmodel.entity.RoundTeamExceptionSelectionMethod;
import com.myrtletrip.round.exceptionmodel.entity.RoundTeamExceptionType;

import java.math.BigDecimal;

public class RoundTeamExceptionResponse {
    private Long id;
    private Long roundId;
    private Long roundTeamId;
    private Integer teamNumber;
    private String teamName;
    private RoundTeamExceptionType exceptionType;
    private Long ghostPlayerId;
    private String ghostPlayerName;
    private Long ghostSourceTeamId;
    private Integer ghostSourceTeamNumber;
    private String ghostSourceTeamName;
    private BigDecimal indexMin;
    private BigDecimal indexMax;
    private RoundTeamExceptionSelectionMethod selectionMethod;
    private String rotationPattern;
    private Boolean active;
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    public Long getRoundTeamId() { return roundTeamId; }
    public void setRoundTeamId(Long roundTeamId) { this.roundTeamId = roundTeamId; }

    public Integer getTeamNumber() { return teamNumber; }
    public void setTeamNumber(Integer teamNumber) { this.teamNumber = teamNumber; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public RoundTeamExceptionType getExceptionType() { return exceptionType; }
    public void setExceptionType(RoundTeamExceptionType exceptionType) { this.exceptionType = exceptionType; }

    public Long getGhostPlayerId() { return ghostPlayerId; }
    public void setGhostPlayerId(Long ghostPlayerId) { this.ghostPlayerId = ghostPlayerId; }

    public String getGhostPlayerName() { return ghostPlayerName; }
    public void setGhostPlayerName(String ghostPlayerName) { this.ghostPlayerName = ghostPlayerName; }

    public Long getGhostSourceTeamId() { return ghostSourceTeamId; }
    public void setGhostSourceTeamId(Long ghostSourceTeamId) { this.ghostSourceTeamId = ghostSourceTeamId; }

    public Integer getGhostSourceTeamNumber() { return ghostSourceTeamNumber; }
    public void setGhostSourceTeamNumber(Integer ghostSourceTeamNumber) { this.ghostSourceTeamNumber = ghostSourceTeamNumber; }

    public String getGhostSourceTeamName() { return ghostSourceTeamName; }
    public void setGhostSourceTeamName(String ghostSourceTeamName) { this.ghostSourceTeamName = ghostSourceTeamName; }

    public BigDecimal getIndexMin() { return indexMin; }
    public void setIndexMin(BigDecimal indexMin) { this.indexMin = indexMin; }

    public BigDecimal getIndexMax() { return indexMax; }
    public void setIndexMax(BigDecimal indexMax) { this.indexMax = indexMax; }

    public RoundTeamExceptionSelectionMethod getSelectionMethod() { return selectionMethod; }
    public void setSelectionMethod(RoundTeamExceptionSelectionMethod selectionMethod) { this.selectionMethod = selectionMethod; }

    public String getRotationPattern() { return rotationPattern; }
    public void setRotationPattern(String rotationPattern) { this.rotationPattern = rotationPattern; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
