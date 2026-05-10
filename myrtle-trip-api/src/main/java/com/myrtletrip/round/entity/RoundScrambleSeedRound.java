package com.myrtletrip.round.entity;

import com.myrtletrip.trip.entity.TripPlannedRound;
import jakarta.persistence.*;

@Entity
@Table(
        name = "round_scramble_seed_round",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_round_scramble_seed_round",
                        columnNames = {"scramble_round_id", "planned_round_id"}
                )
        }
)
public class RoundScrambleSeedRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scramble_round_id", nullable = false)
    private Round scrambleRound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planned_round_id", nullable = false)
    private TripPlannedRound plannedRound;

    public Long getId() {
        return id;
    }

    public Round getScrambleRound() {
        return scrambleRound;
    }

    public void setScrambleRound(Round scrambleRound) {
        this.scrambleRound = scrambleRound;
    }

    public TripPlannedRound getPlannedRound() {
        return plannedRound;
    }

    public void setPlannedRound(TripPlannedRound plannedRound) {
        this.plannedRound = plannedRound;
    }
}
