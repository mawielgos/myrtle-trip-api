package com.myrtletrip.round.entity;

import com.myrtletrip.player.entity.Player;
import jakarta.persistence.*;

@Entity
@Table(name = "round_team_player")
public class RoundTeamPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_team_id", nullable = false)
    private RoundTeam roundTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "player_order")
    private Integer playerOrder;

    public Long getId() {
        return id;
    }

    public RoundTeam getRoundTeam() {
        return roundTeam;
    }

    public void setRoundTeam(RoundTeam roundTeam) {
        this.roundTeam = roundTeam;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Integer getPlayerOrder() {
        return playerOrder;
    }

    public void setPlayerOrder(Integer playerOrder) {
        this.playerOrder = playerOrder;
    }
}
