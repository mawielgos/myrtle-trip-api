package com.myrtletrip.course.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "course_tee",
    uniqueConstraints = @UniqueConstraint(name = "uq_course_tee", columnNames = {"course_id", "tee_name"})
)
public class CourseTee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "tee_name", nullable = false, length = 80)
    private String teeName;

    @Column(name = "course_rating", nullable = false, precision = 5, scale = 2)
    private BigDecimal courseRating;

    @Column(nullable = false)
    private Integer slope;

    @Column(name = "alternate_course_rating", precision = 5, scale = 2)
    private BigDecimal alternateCourseRating;

    @Column(name = "alternate_slope")
    private Integer alternateSlope;

    @Column(name = "par_total", nullable = false)
    private Integer parTotal;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
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

    public BigDecimal getAlternateCourseRating() {
        return alternateCourseRating;
    }

    public void setAlternateCourseRating(BigDecimal alternateCourseRating) {
        this.alternateCourseRating = alternateCourseRating;
    }

    public Integer getAlternateSlope() {
        return alternateSlope;
    }

    public void setAlternateSlope(Integer alternateSlope) {
        this.alternateSlope = alternateSlope;
    }

    public Integer getParTotal() {
        return parTotal;
    }

    public void setParTotal(Integer parTotal) {
        this.parTotal = parTotal;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}