package com.myrtletrip.course.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CourseTeeListResponse {

    private Long courseTeeId;
    private Long courseId;
    private String teeName;
    private String teeType;
    private LocalDate effectiveDate;
    private LocalDate retiredDate;
    private BigDecimal courseRating;
    private Integer slope;
    private Integer parTotal;
    private Integer yardageTotal;
    private BigDecimal womenCourseRating;
    private Integer womenSlope;
    private Integer womenParTotal;
    private Long holeCount;

    public Long getCourseTeeId() { return courseTeeId; }
    public void setCourseTeeId(Long courseTeeId) { this.courseTeeId = courseTeeId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getTeeName() { return teeName; }
    public void setTeeName(String teeName) { this.teeName = teeName; }

    public String getTeeType() { return teeType; }
    public void setTeeType(String teeType) { this.teeType = teeType; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

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

    public Long getHoleCount() { return holeCount; }
    public void setHoleCount(Long holeCount) { this.holeCount = holeCount; }
}
