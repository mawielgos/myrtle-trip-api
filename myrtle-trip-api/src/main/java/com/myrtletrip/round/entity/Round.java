package com.myrtletrip.round.entity;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.trip.entity.Trip;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "round",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_round_trip_number", columnNames = {"trip_id", "round_number"})
    }
)
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_round_tee_id")
    private RoundTee standardRoundTee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alternate_round_tee_id")
    private RoundTee alternateRoundTee;

    @Column(name = "round_date")
    private LocalDate roundDate;

    @Column(name = "handicap_percent", nullable = false)
    private Integer handicapPercent = 100;

    @Column(name = "finalized", nullable = false)
    private Boolean finalized = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 50)
    private RoundFormat format;

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public RoundTee getStandardRoundTee() {
        return standardRoundTee;
    }

    public void setStandardRoundTee(RoundTee standardRoundTee) {
        this.standardRoundTee = standardRoundTee;
    }

    public RoundTee getAlternateRoundTee() {
        return alternateRoundTee;
    }

    public void setAlternateRoundTee(RoundTee alternateRoundTee) {
        this.alternateRoundTee = alternateRoundTee;
    }

    public LocalDate getRoundDate() {
        return roundDate;
    }

    public void setRoundDate(LocalDate roundDate) {
        this.roundDate = roundDate;
    }

    public Integer getHandicapPercent() {
        return handicapPercent;
    }

    public void setHandicapPercent(Integer handicapPercent) {
        this.handicapPercent = handicapPercent;
    }

    public Boolean getFinalized() {
        return finalized;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }

    public RoundFormat getFormat() {
        return format;
    }

    public void setFormat(RoundFormat format) {
        this.format = format;
    }
}