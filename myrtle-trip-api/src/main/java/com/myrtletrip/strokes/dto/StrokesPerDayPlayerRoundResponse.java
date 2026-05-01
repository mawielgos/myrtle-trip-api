package com.myrtletrip.strokes.dto;

import java.math.BigDecimal;

public class StrokesPerDayPlayerRoundResponse {

    private Long roundId;
    private Integer roundNumber;
    private BigDecimal tripIndex;
    private Integer standardCourseHandicap;
    private Integer alternateCourseHandicap;
    private Boolean standardTeeSelected;
    private Boolean alternateTeeSelected;

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public BigDecimal getTripIndex() {
        return tripIndex;
    }

    public void setTripIndex(BigDecimal tripIndex) {
        this.tripIndex = tripIndex;
    }

    public Integer getStandardCourseHandicap() {
        return standardCourseHandicap;
    }

    public void setStandardCourseHandicap(Integer standardCourseHandicap) {
        this.standardCourseHandicap = standardCourseHandicap;
    }

    public Integer getAlternateCourseHandicap() {
        return alternateCourseHandicap;
    }

    public void setAlternateCourseHandicap(Integer alternateCourseHandicap) {
        this.alternateCourseHandicap = alternateCourseHandicap;
    }

    public Boolean getStandardTeeSelected() {
        return standardTeeSelected;
    }

    public void setStandardTeeSelected(Boolean standardTeeSelected) {
        this.standardTeeSelected = standardTeeSelected;
    }

    public Boolean getAlternateTeeSelected() {
        return alternateTeeSelected;
    }

    public void setAlternateTeeSelected(Boolean alternateTeeSelected) {
        this.alternateTeeSelected = alternateTeeSelected;
    }
}
