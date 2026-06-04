package com.myrtletrip.round.exceptionmodel.entity;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "round_team_exception")
public class RoundTeamException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_team_id", nullable = false)
    private RoundTeam roundTeam;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 40)
    private RoundTeamExceptionType exceptionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ghost_player_id")
    private Player ghostPlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ghost_source_team_id")
    private RoundTeam ghostSourceTeam;

    @Column(name = "index_min", precision = 5, scale = 2)
    private BigDecimal indexMin;

    @Column(name = "index_max", precision = 5, scale = 2)
    private BigDecimal indexMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_method", length = 20)
    private RoundTeamExceptionSelectionMethod selectionMethod;

    @Column(name = "rotation_pattern", length = 200)
    private String rotationPattern;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "notes", length = 500)
    private String notes;

    public Long getId() { return id; }

    public Round getRound() { return round; }
    public void setRound(Round round) { this.round = round; }

    public RoundTeam getRoundTeam() { return roundTeam; }
    public void setRoundTeam(RoundTeam roundTeam) { this.roundTeam = roundTeam; }

    public RoundTeamExceptionType getExceptionType() { return exceptionType; }
    public void setExceptionType(RoundTeamExceptionType exceptionType) { this.exceptionType = exceptionType; }

    public Player getGhostPlayer() { return ghostPlayer; }
    public void setGhostPlayer(Player ghostPlayer) { this.ghostPlayer = ghostPlayer; }

    public RoundTeam getGhostSourceTeam() { return ghostSourceTeam; }
    public void setGhostSourceTeam(RoundTeam ghostSourceTeam) { this.ghostSourceTeam = ghostSourceTeam; }

    public BigDecimal getIndexMin() { return indexMin; }
    public void setIndexMin(BigDecimal indexMin) { this.indexMin = indexMin; }

    public BigDecimal getIndexMax() { return indexMax; }
    public void setIndexMax(BigDecimal indexMax) { this.indexMax = indexMax; }

    public RoundTeamExceptionSelectionMethod getSelectionMethod() { return selectionMethod; }
    public void setSelectionMethod(RoundTeamExceptionSelectionMethod selectionMethod) { this.selectionMethod = selectionMethod; }

    public String getRotationPattern() { return rotationPattern; }
    public void setRotationPattern(String rotationPattern) { this.rotationPattern = rotationPattern; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
