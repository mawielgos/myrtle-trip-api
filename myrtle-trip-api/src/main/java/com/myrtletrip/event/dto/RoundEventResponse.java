package com.myrtletrip.event.dto;

import com.myrtletrip.event.model.RoundEventType;

public class RoundEventResponse {

    private Long id;
    private Long roundId;
    private RoundEventType eventType;
    private String eventName;
    private Integer eventOrder;
    private Boolean active;
    private Boolean usesGross;
    private Boolean usesNet;
    private Boolean usesTeams;
    private Integer teamSize;
    private Integer handicapPercent;
    private Boolean teamBased;
    private Boolean individualEvent;
    private Boolean usesHandicap;
    private Boolean payoutEligible;
    private Boolean tournamentEligible;
    private String scoringModeLabel;
    private String defaultEventName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    public RoundEventType getEventType() { return eventType; }
    public void setEventType(RoundEventType eventType) { this.eventType = eventType; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public Integer getEventOrder() { return eventOrder; }
    public void setEventOrder(Integer eventOrder) { this.eventOrder = eventOrder; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getUsesGross() { return usesGross; }
    public void setUsesGross(Boolean usesGross) { this.usesGross = usesGross; }

    public Boolean getUsesNet() { return usesNet; }
    public void setUsesNet(Boolean usesNet) { this.usesNet = usesNet; }

    public Boolean getUsesTeams() { return usesTeams; }
    public void setUsesTeams(Boolean usesTeams) { this.usesTeams = usesTeams; }

    public Integer getTeamSize() { return teamSize; }
    public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }

    public Integer getHandicapPercent() { return handicapPercent; }
    public void setHandicapPercent(Integer handicapPercent) { this.handicapPercent = handicapPercent; }

    public Boolean getTeamBased() { return teamBased; }
    public void setTeamBased(Boolean teamBased) { this.teamBased = teamBased; }

    public Boolean getIndividualEvent() { return individualEvent; }
    public void setIndividualEvent(Boolean individualEvent) { this.individualEvent = individualEvent; }

    public Boolean getUsesHandicap() { return usesHandicap; }
    public void setUsesHandicap(Boolean usesHandicap) { this.usesHandicap = usesHandicap; }

    public Boolean getPayoutEligible() { return payoutEligible; }
    public void setPayoutEligible(Boolean payoutEligible) { this.payoutEligible = payoutEligible; }

    public Boolean getTournamentEligible() { return tournamentEligible; }
    public void setTournamentEligible(Boolean tournamentEligible) { this.tournamentEligible = tournamentEligible; }

    public String getScoringModeLabel() { return scoringModeLabel; }
    public void setScoringModeLabel(String scoringModeLabel) { this.scoringModeLabel = scoringModeLabel; }

    public String getDefaultEventName() { return defaultEventName; }
    public void setDefaultEventName(String defaultEventName) { this.defaultEventName = defaultEventName; }
}
