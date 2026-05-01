package com.myrtletrip.prize.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "prize_schedule_payout",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_prize_schedule_place", columnNames = {"prize_schedule_id", "finishing_place"})
    }
)
public class PrizeSchedulePayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prize_schedule_id", nullable = false)
    private PrizeSchedule prizeSchedule;

    @Column(name = "finishing_place", nullable = false)
    private Integer finishingPlace;

    @Column(name = "amount_per_player", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPerPlayer = BigDecimal.ZERO;

    public Long getId() {
        return id;
    }

    public PrizeSchedule getPrizeSchedule() {
        return prizeSchedule;
    }

    public void setPrizeSchedule(PrizeSchedule prizeSchedule) {
        this.prizeSchedule = prizeSchedule;
    }

    public Integer getFinishingPlace() {
        return finishingPlace;
    }

    public void setFinishingPlace(Integer finishingPlace) {
        this.finishingPlace = finishingPlace;
    }

    public BigDecimal getAmountPerPlayer() {
        return amountPerPlayer;
    }

    public void setAmountPerPlayer(BigDecimal amountPerPlayer) {
        this.amountPerPlayer = amountPerPlayer;
    }
}
