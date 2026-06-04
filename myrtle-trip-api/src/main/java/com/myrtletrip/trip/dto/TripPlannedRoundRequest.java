package com.myrtletrip.trip.dto;

import com.myrtletrip.event.model.RoundEventType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TripPlannedRoundRequest {

    private Integer roundNumber;
    private LocalDate roundDate;
    private Long courseId;
    private Long defaultTeeId;
    private Long womenDefaultTeeId;
    private String format;
    private Boolean includeInFourDayStandings;
    private Integer scrambleTeamSize;
    private List<TripPlannedRoundEventRequest> events = new ArrayList<>();

    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }
    public LocalDate getRoundDate() { return roundDate; }
    public void setRoundDate(LocalDate roundDate) { this.roundDate = roundDate; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getDefaultTeeId() { return defaultTeeId; }
    public void setDefaultTeeId(Long defaultTeeId) { this.defaultTeeId = defaultTeeId; }
    public Long getWomenDefaultTeeId() { return womenDefaultTeeId; }
    public void setWomenDefaultTeeId(Long womenDefaultTeeId) { this.womenDefaultTeeId = womenDefaultTeeId; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Boolean getIncludeInFourDayStandings() { return includeInFourDayStandings; }
    public void setIncludeInFourDayStandings(Boolean includeInFourDayStandings) { this.includeInFourDayStandings = includeInFourDayStandings; }
    public Integer getScrambleTeamSize() { return scrambleTeamSize; }
    public void setScrambleTeamSize(Integer scrambleTeamSize) { this.scrambleTeamSize = scrambleTeamSize; }
    public List<TripPlannedRoundEventRequest> getEvents() { return events; }
    public void setEvents(List<TripPlannedRoundEventRequest> events) { this.events = events; }

    public static class TripPlannedRoundEventRequest {
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
