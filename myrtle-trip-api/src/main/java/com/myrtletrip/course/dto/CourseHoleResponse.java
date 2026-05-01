package com.myrtletrip.course.dto;

public class CourseHoleResponse {

    private Long holeId;
    private Integer holeNumber;
    private Integer par;
    private Integer handicap;
    private Integer yardage;
    private Integer womenPar;
    private Integer womenHandicap;

    public CourseHoleResponse() {
    }

    public CourseHoleResponse(Long holeId, Integer holeNumber, Integer par, Integer handicap, Integer yardage,
                              Integer womenPar, Integer womenHandicap) {
        this.holeId = holeId;
        this.holeNumber = holeNumber;
        this.par = par;
        this.handicap = handicap;
        this.yardage = yardage;
        this.womenPar = womenPar;
        this.womenHandicap = womenHandicap;
    }

    public Long getHoleId() { return holeId; }
    public void setHoleId(Long holeId) { this.holeId = holeId; }

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
