package com.myrtletrip.round.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "round_tee_hole",
    uniqueConstraints = @UniqueConstraint(name = "uq_round_tee_hole", columnNames = {"round_tee_id", "hole_number"})
)
public class RoundTeeHole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_tee_id", nullable = false)
    private RoundTee roundTee;

    @Column(name = "hole_number", nullable = false)
    private Integer holeNumber;

    @Column(name = "par", nullable = false)
    private Integer par;

    @Column(name = "handicap", nullable = false)
    private Integer handicap;

    @Column(name = "yardage")
    private Integer yardage;

    public Long getId() {
        return id;
    }

    public RoundTee getRoundTee() {
        return roundTee;
    }

    public void setRoundTee(RoundTee roundTee) {
        this.roundTee = roundTee;
    }

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public Integer getPar() {
        return par;
    }

    public void setPar(Integer par) {
        this.par = par;
    }

    public Integer getHandicap() {
        return handicap;
    }

    public void setHandicap(Integer handicap) {
        this.handicap = handicap;
    }

    public Integer getYardage() {
        return yardage;
    }

    public void setYardage(Integer yardage) {
        this.yardage = yardage;
    }
}
