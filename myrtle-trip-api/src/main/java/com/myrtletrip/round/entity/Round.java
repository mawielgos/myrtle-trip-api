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
    @JoinColumn(name = "default_round_tee_id")
    private RoundTee defaultRoundTee;

    @Column(name = "round_date")
    private LocalDate roundDate;

    @Column(name = "handicap_percent", nullable = false)
    private Integer handicapPercent = 100;

    @Column(name = "finalized", nullable = false)
    private Boolean finalized = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 50)
    private RoundFormat format;

    @Column(name = "scramble_team_size")
    private Integer scrambleTeamSize = 4;

    @Column(name = "scramble_seeding_method", length = 40)
    private String scrambleSeedingMethod = "CURRENT_HANDICAP_INDEX";

    public Long getId() { return id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public RoundTee getDefaultRoundTee() { return defaultRoundTee; }
    public void setDefaultRoundTee(RoundTee defaultRoundTee) { this.defaultRoundTee = defaultRoundTee; }

    /** Backward-compatible alias while older services/pages are retired. */
    public RoundTee getStandardRoundTee() { return defaultRoundTee; }

    /** Backward-compatible alias while older services/pages are retired. */
    public void setStandardRoundTee(RoundTee standardRoundTee) { this.defaultRoundTee = standardRoundTee; }

    /** Alternate tee is no longer a round-level concept. Player tee overrides use Scorecard.roundTee. */
    public RoundTee getAlternateRoundTee() { return null; }

    /** Alternate tee is no longer a round-level concept. Player tee overrides use Scorecard.roundTee. */
    public void setAlternateRoundTee(RoundTee alternateRoundTee) { }

    public LocalDate getRoundDate() { return roundDate; }
    public void setRoundDate(LocalDate roundDate) { this.roundDate = roundDate; }

    public Integer getHandicapPercent() { return handicapPercent; }
    public void setHandicapPercent(Integer handicapPercent) { this.handicapPercent = handicapPercent; }

    public Boolean getFinalized() { return finalized; }
    public void setFinalized(Boolean finalized) { this.finalized = finalized; }

    public RoundFormat getFormat() { return format; }
    public void setFormat(RoundFormat format) { this.format = format; }

    public Integer getScrambleTeamSize() { return scrambleTeamSize; }
    public void setScrambleTeamSize(Integer scrambleTeamSize) { this.scrambleTeamSize = scrambleTeamSize; }

    public String getScrambleSeedingMethod() { return scrambleSeedingMethod; }
    public void setScrambleSeedingMethod(String scrambleSeedingMethod) { this.scrambleSeedingMethod = scrambleSeedingMethod; }
}
