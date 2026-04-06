package com.myrtletrip.course.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "course_hole",
    uniqueConstraints = @UniqueConstraint(name = "uq_course_hole", columnNames = {"course_tee_id", "hole_number"})
)
public class CourseHole {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_tee_id", nullable = false)
    private CourseTee courseTee;

    @Column(name = "hole_number", nullable = false)
    private Integer holeNumber;

    @Column(nullable = false)
    private Integer par;

    @Column(nullable = false)
    private Integer handicap;

    @Column
    private Integer yardage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseTee getCourseTee() {
        return courseTee;
    }

    public void setCourseTee(CourseTee courseTee) {
        this.courseTee = courseTee;
    }

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public Integer getPar() {
        return par;
    }

    public void setPar(Integer par) {
        this.par = par;
    }

    public Integer getHandicap() {
        return handicap;
    }

    public void setHandicap(Integer handicap) {
        this.handicap = handicap;
    }

    public Integer getYardage() {
        return yardage;
    }

    public void setYardage(Integer yardage) {
        this.yardage = yardage;
    }
}