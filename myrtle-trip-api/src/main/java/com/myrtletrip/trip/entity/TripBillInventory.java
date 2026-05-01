package com.myrtletrip.trip.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "trip_bill_inventory",
        uniqueConstraints = @UniqueConstraint(name = "uk_trip_bill_inventory_trip", columnNames = "trip_id")
)
public class TripBillInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "hundreds_count", nullable = false)
    private Integer hundredsCount = 0;

    @Column(name = "fifties_count", nullable = false)
    private Integer fiftiesCount = 0;

    @Column(name = "twenties_count", nullable = false)
    private Integer twentiesCount = 0;

    @Column(name = "tens_count", nullable = false)
    private Integer tensCount = 0;

    @Column(name = "fives_count", nullable = false)
    private Integer fivesCount = 0;

    @Column(name = "ones_count", nullable = false)
    private Integer onesCount = 0;

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Integer getHundredsCount() {
        return hundredsCount;
    }

    public void setHundredsCount(Integer hundredsCount) {
        this.hundredsCount = cleanCount(hundredsCount);
    }

    public Integer getFiftiesCount() {
        return fiftiesCount;
    }

    public void setFiftiesCount(Integer fiftiesCount) {
        this.fiftiesCount = cleanCount(fiftiesCount);
    }

    public Integer getTwentiesCount() {
        return twentiesCount;
    }

    public void setTwentiesCount(Integer twentiesCount) {
        this.twentiesCount = cleanCount(twentiesCount);
    }

    public Integer getTensCount() {
        return tensCount;
    }

    public void setTensCount(Integer tensCount) {
        this.tensCount = cleanCount(tensCount);
    }

    public Integer getFivesCount() {
        return fivesCount;
    }

    public void setFivesCount(Integer fivesCount) {
        this.fivesCount = cleanCount(fivesCount);
    }

    public Integer getOnesCount() {
        return onesCount;
    }

    public void setOnesCount(Integer onesCount) {
        this.onesCount = cleanCount(onesCount);
    }

    private Integer cleanCount(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }
        return value;
    }
}
