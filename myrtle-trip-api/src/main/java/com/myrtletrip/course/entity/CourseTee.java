package com.myrtletrip.course.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.myrtletrip.course.model.TeeType;

@Entity
@Table(
    name = "course_tee",
    uniqueConstraints = @UniqueConstraint(name = "uq_course_tee_version", columnNames = {"course_id", "tee_name", "effective_date"})
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

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate = LocalDate.of(1900, 1, 1);

    @Enumerated(EnumType.STRING)
    @Column(name = "tee_type", nullable = false, length = 10)
    private TeeType teeType = TeeType.REGULAR;

    @Column(name = "retired_date")
    private LocalDate retiredDate;

    @Column(name = "course_rating", precision = 5, scale = 2)
    private BigDecimal courseRating;

    @Column(name = "slope")
    private Integer slope;

    @Column(name = "par_total", nullable = false)
    private Integer parTotal;

    @Column(name = "yardage_total")
    private Integer yardageTotal;

    @Column(name = "women_course_rating", precision = 5, scale = 2)
    private BigDecimal womenCourseRating;

    @Column(name = "women_slope")
    private Integer womenSlope;

    @Column(name = "women_par_total")
    private Integer womenParTotal;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public String getTeeName() { return teeName; }
    public void setTeeName(String teeName) { this.teeName = teeName; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public TeeType getTeeType() { return teeType; }
    public void setTeeType(TeeType teeType) { this.teeType = teeType; }

    public LocalDate getRetiredDate() { return retiredDate; }
    public void setRetiredDate(LocalDate retiredDate) { this.retiredDate = retiredDate; }

    public BigDecimal getCourseRating() { return courseRating; }
    public void setCourseRating(BigDecimal courseRating) { this.courseRating = courseRating; }

    public Integer getSlope() { return slope; }
    public void setSlope(Integer slope) { this.slope = slope; }

    public Integer getParTotal() { return parTotal; }
    public void setParTotal(Integer parTotal) { this.parTotal = parTotal; }

    public Integer getYardageTotal() { return yardageTotal; }
    public void setYardageTotal(Integer yardageTotal) { this.yardageTotal = yardageTotal; }

    public BigDecimal getWomenCourseRating() { return womenCourseRating; }
    public void setWomenCourseRating(BigDecimal womenCourseRating) { this.womenCourseRating = womenCourseRating; }

    public Integer getWomenSlope() { return womenSlope; }
    public void setWomenSlope(Integer womenSlope) { this.womenSlope = womenSlope; }

    public Integer getWomenParTotal() { return womenParTotal; }
    public void setWomenParTotal(Integer womenParTotal) { this.womenParTotal = womenParTotal; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isEligibleForGender(String gender) {
        String normalizedGender = normalizeGender(gender);

        if ("F".equals(normalizedGender)) {
            return womenCourseRating != null && womenSlope != null && womenParTotal != null;
        }

        return courseRating != null && slope != null && parTotal != null;
    }

    public BigDecimal getRatingForGender(String gender) {
        if ("F".equals(normalizeGender(gender)) && womenCourseRating != null) {
            return womenCourseRating;
        }
        return courseRating;
    }

    public Integer getSlopeForGender(String gender) {
        if ("F".equals(normalizeGender(gender)) && womenSlope != null) {
            return womenSlope;
        }
        return slope;
    }

    public Integer getParForGender(String gender) {
        if ("F".equals(normalizeGender(gender)) && womenParTotal != null) {
            return womenParTotal;
        }
        return parTotal;
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return "M";
        }
        return gender.trim().toUpperCase();
    }
}
