package com.myrtletrip.trip.entity;

import com.myrtletrip.player.entity.Player;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "trip_player",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_player_trip_player", columnNames = {"trip_id", "player_id"})
        }
)
public class TripPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "frozen_handicap_index", precision = 5, scale = 1)
    private BigDecimal frozenHandicapIndex;

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public BigDecimal getFrozenHandicapIndex() {
        return frozenHandicapIndex;
    }

    public void setFrozenHandicapIndex(BigDecimal frozenHandicapIndex) {
        this.frozenHandicapIndex = frozenHandicapIndex;
    }
}