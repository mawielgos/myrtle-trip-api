package com.myrtletrip.scoreentry.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "hole_score",
    uniqueConstraints = @UniqueConstraint(name = "uq_hole_score_scorecard_hole", columnNames = {"scorecard_id", "hole_number"})
)
public class HoleScore {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scorecard_id", nullable = false)
    private Scorecard scorecard;

    @Column(name = "hole_number", nullable = false)
    private Integer holeNumber;

    @Column
    private Integer strokes;

    @Column(name = "adjusted_strokes")
    private Integer adjustedStrokes;

    @Column(name = "net_strokes")
    private Integer netStrokes;

    @Column(name = "used_in_team_game")
    private Boolean usedInTeamGame;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Scorecard getScorecard() {
        return scorecard;
    }

    public void setScorecard(Scorecard scorecard) {
        this.scorecard = scorecard;
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

    public Integer getAdjustedStrokes() {
        return adjustedStrokes;
    }

    public void setAdjustedStrokes(Integer adjustedStrokes) {
        this.adjustedStrokes = adjustedStrokes;
    }

    public Integer getNetStrokes() {
        return netStrokes;
    }

    public void setNetStrokes(Integer netStrokes) {
        this.netStrokes = netStrokes;
    }

    public Boolean getUsedInTeamGame() {
        return usedInTeamGame;
    }

    public void setUsedInTeamGame(Boolean usedInTeamGame) {
        this.usedInTeamGame = usedInTeamGame;
    }
}