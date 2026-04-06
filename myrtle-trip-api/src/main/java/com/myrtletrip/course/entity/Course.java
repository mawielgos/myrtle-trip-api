package com.myrtletrip.course.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legacy_course_number", unique = true)
    private Integer legacyCourseNumber;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "active", nullable = false)
    private Boolean active;

    public Long getId() {
        return id;
    }

    public Integer getLegacyCourseNumber() {
        return legacyCourseNumber;
    }

    public void setLegacyCourseNumber(Integer legacyCourseNumber) {
        this.legacyCourseNumber = legacyCourseNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Course{id=" + id +
                ", legacyCourseNumber=" + legacyCourseNumber +
                ", name='" + name + '\'' +
                '}';
    }
}