package com.myrtletrip.round.entity;

import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.round.model.RoundTeeRole;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "round_tee")
public class RoundTee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Keep optional=false false for now because Round also points to RoundTee,
    // which avoids awkward circular save ordering during the refactor phase.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_course_tee_id")
    private CourseTee sourceCourseTee;

    @Enumerated(EnumType.STRING)
    @Column(name = "tee_role", nullable = false, length = 20)
    private RoundTeeRole teeRole;

    @Column(name = "course_name", nullable = false, length = 150)
    private String courseName;

    @Column(name = "tee_name", nullable = false, length = 80)
    private String teeName;

    @Column(name = "course_rating", nullable = false, precision = 5, scale = 2)
    private BigDecimal courseRating;

    @Column(name = "slope", nullable = false)
    private Integer slope;

    @Column(name = "par_total", nullable = false)
    private Integer parTotal;

    public Long getId() {
        return id;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }

    public CourseTee getSourceCourseTee() {
        return sourceCourseTee;
    }

    public void setSourceCourseTee(CourseTee sourceCourseTee) {
        this.sourceCourseTee = sourceCourseTee;
    }

    public RoundTeeRole getTeeRole() {
        return teeRole;
    }

    public void setTeeRole(RoundTeeRole teeRole) {
        this.teeRole = teeRole;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeeName() {
        return teeName;
    }

    public void setTeeName(String teeName) {
        this.teeName = teeName;
    }

    public BigDecimal getCourseRating() {
        return courseRating;
    }

    public void setCourseRating(BigDecimal courseRating) {
        this.courseRating = courseRating;
    }

    public Integer getSlope() {
        return slope;
    }

    public void setSlope(Integer slope) {
        this.slope = slope;
    }

    public Integer getParTotal() {
        return parTotal;
    }

    public void setParTotal(Integer parTotal) {
        this.parTotal = parTotal;
    }
}
