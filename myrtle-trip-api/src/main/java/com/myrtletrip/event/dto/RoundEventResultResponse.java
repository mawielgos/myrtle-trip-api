package com.myrtletrip.event.dto;

import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.games.dto.TeamGameResult;

import java.util.ArrayList;
import java.util.List;

public class RoundEventResultResponse {

    private Long eventId;
    private Long roundId;
    private RoundEventType eventType;
    private String eventName;
    private Integer eventOrder;
    private String resultKind;
    private List<IndividualEventResult> individualResults = new ArrayList<>();
    private List<TeamGameResult> teamResults = new ArrayList<>();

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    public RoundEventType getEventType() { return eventType; }
    public void setEventType(RoundEventType eventType) { this.eventType = eventType; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public Integer getEventOrder() { return eventOrder; }
    public void setEventOrder(Integer eventOrder) { this.eventOrder = eventOrder; }

    public String getResultKind() { return resultKind; }
    public void setResultKind(String resultKind) { this.resultKind = resultKind; }

    public List<IndividualEventResult> getIndividualResults() { return individualResults; }
    public void setIndividualResults(List<IndividualEventResult> individualResults) { this.individualResults = individualResults; }

    public List<TeamGameResult> getTeamResults() { return teamResults; }
    public void setTeamResults(List<TeamGameResult> teamResults) { this.teamResults = teamResults; }
}
