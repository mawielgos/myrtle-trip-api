package com.myrtletrip.course.dto;

public class SaveCourseTeeComboHoleRequest {

    private Integer holeNumber;
    private Long sourceTeeId;

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
}
