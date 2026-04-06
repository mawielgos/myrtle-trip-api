package com.myrtletrip.round.entity;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.trip.entity.Trip;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "round")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_tee_id", nullable = false)
    private CourseTee courseTee;

    @Column(name = "round_date", nullable = false)
    private LocalDate roundDate;

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

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public CourseTee getCourseTee() {
        return courseTee;
    }

    public void setCourseTee(CourseTee courseTee) {
        this.courseTee = courseTee;
    }

    public LocalDate getRoundDate() {
        return roundDate;
    }

    public void setRoundDate(LocalDate roundDate) {
        this.roundDate = roundDate;
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