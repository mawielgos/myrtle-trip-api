package com.myrtletrip.course.dto;

public class CourseTeeComboHoleResponse {

    private Long comboHoleId;
    private Integer holeNumber;
    private Long sourceTeeId;
    private String sourceTeeName;

    public CourseTeeComboHoleResponse() {
    }

    public CourseTeeComboHoleResponse(Long comboHoleId, Integer holeNumber, Long sourceTeeId, String sourceTeeName) {
        this.comboHoleId = comboHoleId;
        this.holeNumber = holeNumber;
        this.sourceTeeId = sourceTeeId;
        this.sourceTeeName = sourceTeeName;
    }

    public Long getComboHoleId() {
        return comboHoleId;
    }

    public void setComboHoleId(Long comboHoleId) {
        this.comboHoleId = comboHoleId;
    }

    public Integer getHoleNumber() {
        return holeNumber;
    }

    public void setHoleNumber(Integer holeNumber) {
        this.holeNumber = holeNumber;
    }

    public Long getSourceTeeId() {
        return sourceTeeId;
    }

    public void setSourceTeeId(Long sourceTeeId) {
        this.sourceTeeId = sourceTeeId;
    }

    public String getSourceTeeName() {
        return sourceTeeName;
    }

    public void setSourceTeeName(String sourceTeeName) {
        this.sourceTeeName = sourceTeeName;
    }
}
