package com.myrtletrip.contest.entity;

import com.myrtletrip.player.entity.Player;
import jakarta.persistence.*;

@Entity
@Table(
    name = "team_player",
    uniqueConstraints = @UniqueConstraint(name = "uq_team_player", columnNames = {"team_id", "player_id"})
)
public class TeamPlayer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}