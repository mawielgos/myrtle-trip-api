package com.myrtletrip.course.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "course_tee_combo_hole",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_course_tee_combo_hole",
        columnNames = {"combo_tee_id", "hole_number"}
    )
)
public class CourseTeeComboHole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "combo_tee_id", nullable = false)
    private CourseTee comboTee;

    @Column(name = "hole_number", nullable = false)
    private Integer holeNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_tee_id", nullable = false)
    private CourseTee sourceTee;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseTee getComboTee() {
        return comboTee;
    }

    public void setComboTee(CourseTee comboTee) {
        this.comboTee = comboTee;
    }

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public CourseTee getSourceTee() {
        return sourceTee;
    }

    public void setSourceTee(CourseTee sourceTee) {
        this.sourceTee = sourceTee;
    }
}
