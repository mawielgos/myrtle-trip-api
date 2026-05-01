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

    @Column
    private Integer par;

    @Column
    private Integer handicap;

    @Column
    private Integer yardage;

    @Column(name = "women_par")
    private Integer womenPar;

    @Column(name = "women_handicap")
    private Integer womenHandicap;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CourseTee getCourseTee() { return courseTee; }
    public void setCourseTee(CourseTee courseTee) { this.courseTee = courseTee; }

    public Integer getHoleNumber() { return holeNumber; }
    public void setHoleNumber(Integer holeNumber) { this.holeNumber = holeNumber; }

    public Integer getPar() { return par; }
    public void setPar(Integer par) { this.par = par; }

    public Integer getHandicap() { return handicap; }
    public void setHandicap(Integer handicap) { this.handicap = handicap; }

    public Integer getYardage() { return yardage; }
    public void setYardage(Integer yardage) { this.yardage = yardage; }

    public Integer getWomenPar() { return womenPar; }
    public void setWomenPar(Integer womenPar) { this.womenPar = womenPar; }

    public Integer getWomenHandicap() { return womenHandicap; }
    public void setWomenHandicap(Integer womenHandicap) { this.womenHandicap = womenHandicap; }
}
