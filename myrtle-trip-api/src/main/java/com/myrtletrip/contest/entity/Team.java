package com.myrtletrip.contest.entity;

import com.myrtletrip.round.entity.Round;
import jakarta.persistence.*;

@Entity
@Table(
    name = "team",
    uniqueConstraints = @UniqueConstraint(name = "uq_team_round_number", columnNames = {"round_id", "team_number"})
)
public class Team {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Column(name = "team_number", nullable = false)
    private Integer teamNumber;

    @Column(name = "team_name", length = 100)
    private String teamName;

    @Column(name = "team_type", nullable = false, length = 40)
    private String teamType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }

    public Integer getTeamNumber() {
        return teamNumber;
    }

    public void setTeamNumber(Integer teamNumber) {
        this.teamNumber = teamNumber;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }
}