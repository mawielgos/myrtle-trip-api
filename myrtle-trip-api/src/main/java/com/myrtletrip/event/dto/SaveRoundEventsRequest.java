package com.myrtletrip.event.dto;

import com.myrtletrip.event.model.RoundEventType;

import java.util.ArrayList;
import java.util.List;

public class SaveRoundEventsRequest {

    private List<SaveRoundEventItemRequest> events = new ArrayList<>();

    public List<SaveRoundEventItemRequest> getEvents() { return events; }
    public void setEvents(List<SaveRoundEventItemRequest> events) { this.events = events; }

    public static class SaveRoundEventItemRequest {
        private RoundEventType eventType;
        private String eventName;
        private Integer eventOrder;
        private Integer teamSize;
        private Integer handicapPercent;

        public RoundEventType getEventType() { return eventType; }
        public void setEventType(RoundEventType eventType) { this.eventType = eventType; }

        public String getEventName() { return eventName; }
        public void setEventName(String eventName) { this.eventName = eventName; }

        public Integer getEventOrder() { return eventOrder; }
        public void setEventOrder(Integer eventOrder) { this.eventOrder = eventOrder; }

        public Integer getTeamSize() { return teamSize; }
        public void setTeamSize(Integer teamSize) { this.teamSize = teamSize; }

        public Integer getHandicapPercent() { return handicapPercent; }
        public void setHandicapPercent(Integer handicapPercent) { this.handicapPercent = handicapPercent; }
    }
}
