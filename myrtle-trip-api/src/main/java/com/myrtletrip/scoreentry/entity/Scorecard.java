package com.myrtletrip.scoreentry.entity;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTee;
import jakarta.persistence.*;

@Entity
@Table(name = "scorecard")
public class Scorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private RoundTeam team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_tee_id")
    private RoundTee roundTee;

    @Column(name = "course_handicap")
    private Integer courseHandicap;

    @Column(name = "playing_handicap")
    private Integer playingHandicap;

    @Column(name = "gross_score")
    private Integer grossScore;

    @Column(name = "adjusted_gross_score")
    private Integer adjustedGrossScore;

    @Column(name = "net_score")
    private Integer netScore;

    @Column(name = "thru_hole")
    private Integer thruHole;

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

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public RoundTeam getTeam() {
        return team;
    }

    public void setTeam(RoundTeam team) {
        this.team = team;
    }

    public RoundTee getRoundTee() {
        return roundTee;
    }

    public void setRoundTee(RoundTee roundTee) {
        this.roundTee = roundTee;
    }

    public Integer getCourseHandicap() {
        return courseHandicap;
    }

    public void setCourseHandicap(Integer courseHandicap) {
        this.courseHandicap = courseHandicap;
    }

    public Integer getPlayingHandicap() {
        return playingHandicap;
    }

    public void setPlayingHandicap(Integer playingHandicap) {
        this.playingHandicap = playingHandicap;
    }

    public Integer getGrossScore() {
        return grossScore;
    }

    public void setGrossScore(Integer grossScore) {
        this.grossScore = grossScore;
    }

    public Integer getAdjustedGrossScore() {
        return adjustedGrossScore;
    }

    public void setAdjustedGrossScore(Integer adjustedGrossScore) {
        this.adjustedGrossScore = adjustedGrossScore;
    }

    public Integer getNetScore() {
        return netScore;
    }

    public void setNetScore(Integer netScore) {
        this.netScore = netScore;
    }

    public Integer getThruHole() {
        return thruHole;
    }

    public void setThruHole(Integer thruHole) {
        this.thruHole = thruHole;
    }
}