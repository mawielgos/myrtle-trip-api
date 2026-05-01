package com.myrtletrip.prize.entity;

import com.myrtletrip.prize.model.PrizePayoutUnit;
import com.myrtletrip.prize.model.PrizeResultScope;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.trip.entity.Trip;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "prize_schedule",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_prize_schedule_trip_game", columnNames = {"trip_id", "game_key"})
    }
)
public class PrizeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id")
    private Round round;

    @Column(name = "game_key", nullable = false, length = 80)
    private String gameKey;

    @Column(name = "game_name", nullable = false, length = 160)
    private String gameName;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_scope", nullable = false, length = 20)
    private PrizeResultScope resultScope = PrizeResultScope.TEAM;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_unit", nullable = false, length = 20)
    private PrizePayoutUnit payoutUnit = PrizePayoutUnit.PLAYER;

    @OneToMany(mappedBy = "prizeSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("finishingPlace ASC")
    private List<PrizeSchedulePayout> payouts = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }

    public String getGameKey() {
        return gameKey;
    }

    public void setGameKey(String gameKey) {
        this.gameKey = gameKey;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public PrizeResultScope getResultScope() {
        return resultScope;
    }

    public void setResultScope(PrizeResultScope resultScope) {
        this.resultScope = resultScope;
    }

    public PrizePayoutUnit getPayoutUnit() {
        return payoutUnit;
    }

    public void setPayoutUnit(PrizePayoutUnit payoutUnit) {
        this.payoutUnit = payoutUnit;
    }

    public List<PrizeSchedulePayout> getPayouts() {
        return payouts;
    }

    public void setPayouts(List<PrizeSchedulePayout> payouts) {
        this.payouts = payouts;
    }
}
