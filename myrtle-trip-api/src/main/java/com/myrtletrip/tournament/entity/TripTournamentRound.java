package com.myrtletrip.tournament.entity;

import com.myrtletrip.trip.entity.TripPlannedRound;
import jakarta.persistence.*;

@Entity
@Table(
        name = "trip_tournament_round",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_tournament_round_planned", columnNames = {"tournament_id", "planned_round_id"}),
                @UniqueConstraint(name = "uk_trip_tournament_round_sort", columnNames = {"tournament_id", "sort_order"})
        }
)
public class TripTournamentRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private TripTournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planned_round_id", nullable = false)
    private TripPlannedRound plannedRound;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public TripTournament getTournament() {
        return tournament;
    }

    public void setTournament(TripTournament tournament) {
        this.tournament = tournament;
    }

    public TripPlannedRound getPlannedRound() {
        return plannedRound;
    }

    public void setPlannedRound(TripPlannedRound plannedRound) {
        this.plannedRound = plannedRound;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
