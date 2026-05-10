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

    @Column(name = "women_default_tee_id")
    private Long womenDefaultTeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format")
    private RoundFormat format;

    @Column(name = "include_in_four_day_standings", nullable = false)
    private Boolean includeInFourDayStandings = false;

    @Column(name = "scramble_team_size")
    private Integer scrambleTeamSize = 4;

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

public Long getWomenDefaultTeeId() {
        return womenDefaultTeeId;
    }

    public void setWomenDefaultTeeId(Long womenDefaultTeeId) {
        this.womenDefaultTeeId = womenDefaultTeeId;
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

public Integer getScrambleTeamSize() {
        return scrambleTeamSize;
    }

    public void setScrambleTeamSize(Integer scrambleTeamSize) {
        this.scrambleTeamSize = scrambleTeamSize;
    }
}
