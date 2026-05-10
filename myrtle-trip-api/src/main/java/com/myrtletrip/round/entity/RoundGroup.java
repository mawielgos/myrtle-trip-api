package com.myrtletrip.round.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "grouping",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_grouping_round_group", columnNames = {"round_id", "group_number"})
        }
)
public class RoundGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Column(name = "group_number", nullable = false)
    private Integer groupNumber;

    @Column(name = "tee_time")
    private LocalTime teeTime;

    @OneToMany(
            mappedBy = "roundGroup",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("seatOrder ASC")
    private List<RoundGroupPlayer> players = new ArrayList<>();

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }

    public Integer getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(Integer groupNumber) {
        this.groupNumber = groupNumber;
    }

    public LocalTime getTeeTime() {
        return teeTime;
    }

    public void setTeeTime(LocalTime teeTime) {
        this.teeTime = teeTime;
    }

    public List<RoundGroupPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<RoundGroupPlayer> players) {
        this.players = players;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void addPlayer(RoundGroupPlayer player) {
        this.players.add(player);
        player.setRoundGroup(this);
    }

    public void clearPlayers() {
        for (RoundGroupPlayer player : players) {
            player.setRoundGroup(null);
        }
        players.clear();
    }
}
