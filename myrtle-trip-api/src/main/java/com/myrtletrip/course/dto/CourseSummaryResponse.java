package com.myrtletrip.course.dto;

public class CourseSummaryResponse {

    private Long courseId;
    private Integer legacyCourseNumber;
    private String courseName;
    private String location;
    private Integer teeCount;
    private Boolean active;

    public CourseSummaryResponse() {
    }

    public CourseSummaryResponse(
            Long courseId,
            Integer legacyCourseNumber,
            String courseName,
            String location,
            Integer teeCount,
            Boolean active) {
        this.courseId = courseId;
        this.legacyCourseNumber = legacyCourseNumber;
        this.courseName = courseName;
        this.location = location;
        this.teeCount = teeCount;
        this.active = active;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Integer getLegacyCourseNumber() {
        return legacyCourseNumber;
    }

    public void setLegacyCourseNumber(Integer legacyCourseNumber) {
        this.legacyCourseNumber = legacyCourseNumber;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getTeeCount() {
        return teeCount;
    }

    public void setTeeCount(Integer teeCount) {
        this.teeCount = teeCount;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
