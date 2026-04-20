package com.myrtletrip.round.entity;

import com.myrtletrip.player.entity.Player;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "grouping_player",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_grouping_player", columnNames = {"grouping_id", "player_id"}),
                @UniqueConstraint(name = "uq_grouping_seat_order", columnNames = {"grouping_id", "seat_order"})
        }
)
public class RoundGroupPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grouping_id", nullable = false)
    private RoundGroup roundGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "seat_order", nullable = false)
    private Integer seatOrder;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public RoundGroup getRoundGroup() {
        return roundGroup;
    }

    public void setRoundGroup(RoundGroup roundGroup) {
        this.roundGroup = roundGroup;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Integer getSeatOrder() {
        return seatOrder;
    }

    public void setSeatOrder(Integer seatOrder) {
        this.seatOrder = seatOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
