package com.myrtletrip.trip.entity;

import com.myrtletrip.event.model.RoundEventType;
import jakarta.persistence.*;

@Entity
@Table(
        name = "trip_planned_round_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_planned_round_event_round_type", columnNames = {"planned_round_id", "event_type"}),
                @UniqueConstraint(name = "uk_trip_planned_round_event_round_order", columnNames = {"planned_round_id", "event_order"})
        }
)
public class TripPlannedRoundEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planned_round_id", nullable = false)
    private TripPlannedRound plannedRound;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private RoundEventType eventType;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "event_order", nullable = false)
    private Integer eventOrder = 1;

    @Column(name = "team_size")
    private Integer teamSize;

    @Column(name = "handicap_percent")
    private Integer handicapPercent;

    public Long getId() { return id; }
    public TripPlannedRound getPlannedRound() { return plannedRound; }
    public void setPlannedRound(TripPlannedRound plannedRound) { this.plannedRound = plannedRound; }
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
