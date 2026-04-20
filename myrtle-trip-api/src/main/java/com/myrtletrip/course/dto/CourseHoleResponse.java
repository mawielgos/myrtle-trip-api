package com.myrtletrip.course.dto;

public class CourseHoleResponse {

    private Long holeId;
    private Integer holeNumber;
    private Integer par;
    private Integer handicap;
    private Integer yardage;

    public CourseHoleResponse() {
    }

    public CourseHoleResponse(Long holeId, Integer holeNumber, Integer par, Integer handicap, Integer yardage) {
        this.holeId = holeId;
        this.holeNumber = holeNumber;
        this.par = par;
        this.handicap = handicap;
        this.yardage = yardage;
    }

    public Long getHoleId() {
        return holeId;
    }

    public void setHoleId(Long holeId) {
        this.holeId = holeId;
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
