package com.myrtletrip.event.entity;

import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.round.entity.Round;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "round_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_round_event_round_order", columnNames = {"round_id", "event_order"}),
                @UniqueConstraint(name = "uk_round_event_round_type", columnNames = {"round_id", "event_type"})
        }
)
public class RoundEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private RoundEventType eventType;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "event_order", nullable = false)
    private Integer eventOrder = 1;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "uses_gross", nullable = false)
    private Boolean usesGross = false;

    @Column(name = "uses_net", nullable = false)
    private Boolean usesNet = false;

    @Column(name = "uses_teams", nullable = false)
    private Boolean usesTeams = false;

    @Column(name = "team_size")
    private Integer teamSize;

    @Column(name = "handicap_percent")
    private Integer handicapPercent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Round getRound() { return round; }
    public void setRound(Round round) { this.round = round; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
