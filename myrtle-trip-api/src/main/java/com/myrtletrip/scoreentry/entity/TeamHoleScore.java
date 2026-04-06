package com.myrtletrip.scoreentry.entity;

import com.myrtletrip.round.entity.RoundTeam;
import jakarta.persistence.*;

@Entity
@Table(
        name = "team_hole_score",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_hole_score_team_hole",
                        columnNames = {"round_team_id", "hole_number"}
                )
        }
)
public class TeamHoleScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_team_id", nullable = false)
    private RoundTeam roundTeam;

    @Column(name = "hole_number", nullable = false)
    private Integer holeNumber;

    @Column(name = "strokes", nullable = false)
    private Integer strokes;

    public Long getId() {
        return id;
    }

    public RoundTeam getRoundTeam() {
        return roundTeam;
    }

    public void setRoundTeam(RoundTeam roundTeam) {
        this.roundTeam = roundTeam;
    }

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public Integer getStrokes() {
        return strokes;
    }

    public void setStrokes(Integer strokes) {
        this.strokes = strokes;
    }
}
