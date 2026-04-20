package com.myrtletrip.trip.entity;

import com.myrtletrip.round.model.RoundFormat;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "trip_planned_round",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_trip_planned_round_trip_round",
                        columnNames = {"trip_id", "round_number"}
                )
        }
)
public class TripPlannedRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "round_date")
    private LocalDate roundDate;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "standard_tee_id")
    private Long standardTeeId;

    @Column(name = "alternate_tee_id")
    private Long alternateTeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format")
    private RoundFormat format;

    @Column(name = "include_in_four_day_standings", nullable = false)
    private Boolean includeInFourDayStandings = false;

    @Column(name = "include_in_scramble_seeding", nullable = false)
    private Boolean includeInScrambleSeeding = false;

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

    public LocalDate getRoundDate() {
        return roundDate;
    }

    public void setRoundDate(LocalDate roundDate) {
        this.roundDate = roundDate;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getStandardTeeId() {
        return standardTeeId;
    }

    public void setStandardTeeId(Long standardTeeId) {
        this.standardTeeId = standardTeeId;
    }

    public Long getAlternateTeeId() {
        return alternateTeeId;
    }

    public void setAlternateTeeId(Long alternateTeeId) {
        this.alternateTeeId = alternateTeeId;
    }

    public RoundFormat getFormat() {
        return format;
    }

    public void setFormat(RoundFormat format) {
        this.format = format;
    }

    public Boolean getIncludeInFourDayStandings() {
        return includeInFourDayStandings;
    }

    public void setIncludeInFourDayStandings(Boolean includeInFourDayStandings) {
        this.includeInFourDayStandings = includeInFourDayStandings;
    }

    public Boolean getIncludeInScrambleSeeding() {
        return includeInScrambleSeeding;
    }

    public void setIncludeInScrambleSeeding(Boolean includeInScrambleSeeding) {
        this.includeInScrambleSeeding = includeInScrambleSeeding;
    }
}
