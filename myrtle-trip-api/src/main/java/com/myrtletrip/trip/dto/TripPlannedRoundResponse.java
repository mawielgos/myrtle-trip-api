package com.myrtletrip.trip.dto;

import com.myrtletrip.event.model.RoundEventType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TripPlannedRoundResponse {

    private Long plannedRoundId;
    private Integer roundNumber;
    private LocalDate roundDate;
    private Long courseId;
    private Long defaultTeeId;
    private Long womenDefaultTeeId;
    private String courseName;
    private String defaultTeeDisplay;
    private String womenDefaultTeeDisplay;
    private String format;
    private Boolean includeInFourDayStandings;
    private Integer scrambleTeamSize;
    private List<TripPlannedRoundEventResponse> events = new ArrayList<>();

    public Long getPlannedRoundId() { return plannedRoundId; }
    public void setPlannedRoundId(Long plannedRoundId) { this.plannedRoundId = plannedRoundId; }
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
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getDefaultTeeDisplay() { return defaultTeeDisplay; }
    public void setDefaultTeeDisplay(String defaultTeeDisplay) { this.defaultTeeDisplay = defaultTeeDisplay; }
    public String getWomenDefaultTeeDisplay() { return womenDefaultTeeDisplay; }
    public void setWomenDefaultTeeDisplay(String womenDefaultTeeDisplay) { this.womenDefaultTeeDisplay = womenDefaultTeeDisplay; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Boolean getIncludeInFourDayStandings() { return includeInFourDayStandings; }
    public void setIncludeInFourDayStandings(Boolean includeInFourDayStandings) { this.includeInFourDayStandings = includeInFourDayStandings; }
    public Integer getScrambleTeamSize() { return scrambleTeamSize; }
    public void setScrambleTeamSize(Integer scrambleTeamSize) { this.scrambleTeamSize = scrambleTeamSize; }
    public List<TripPlannedRoundEventResponse> getEvents() { return events; }
    public void setEvents(List<TripPlannedRoundEventResponse> events) { this.events = events; }

    public static class TripPlannedRoundEventResponse {
        private Long id;
        private RoundEventType eventType;
        private String eventName;
        private Integer eventOrder;
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
}
