package com.myrtletrip.tournament.entity;

import com.myrtletrip.trip.entity.Trip;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "trip_tournament",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_tournament_trip", columnNames = {"trip_id"})
        }
)
public class TripTournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @Column(name = "name", nullable = false, length = 160)
    private String name = "Multi-Round Tournament";

    @Column(name = "standings_label", nullable = false, length = 160)
    private String standingsLabel = "Tournament Standings";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TripTournamentRound> rounds = new ArrayList<TripTournamentRound>();

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStandingsLabel() {
        return standingsLabel;
    }

    public void setStandingsLabel(String standingsLabel) {
        this.standingsLabel = standingsLabel;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TripTournamentRound> getRounds() {
        return rounds;
    }

    public void setRounds(List<TripTournamentRound> rounds) {
        this.rounds = rounds;
    }
}
