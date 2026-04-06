package com.myrtletrip.contest.entity;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "contest_result")
public class ContestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @Column(name = "contest_type", length = 50)
    private String contestType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column
    private Integer rank;

    @Column(name = "result_score", precision = 10, scale = 2)
    private BigDecimal resultScore;

    @Column(precision = 10, scale = 2)
    private BigDecimal payout;

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

    public String getContestType() {
        return contestType;
    }

    public void setContestType(String contestType) {
        this.contestType = contestType;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public BigDecimal getResultScore() {
        return resultScore;
    }

    public void setResultScore(BigDecimal resultScore) {
        this.resultScore = resultScore;
    }

    public BigDecimal getPayout() {
        return payout;
    }

    public void setPayout(BigDecimal payout) {
        this.payout = payout;
    }
}