package com.myrtletrip.strokes.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class StrokesPerDayPlayerRoundResponse {

    private Long roundId;
    private Integer roundNumber;
    private BigDecimal tripIndex;

    /** Legacy fields retained so older UI builds do not break during rollout. */
    private Integer standardCourseHandicap;
    private Integer alternateCourseHandicap;
    private Boolean standardTeeSelected;
    private Boolean alternateTeeSelected;

    private Long selectedRoundTeeId;
    private String selectedTeeName;
    private BigDecimal selectedCourseRating;
    private Integer selectedSlope;
    private Integer selectedYardage;
    private Integer selectedCourseHandicap;
    private Integer selectedPlayingHandicap;
    private List<StrokesPerDayTeeOptionResponse> eligibleTeeOptions = new ArrayList<>();

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

    public Long getSelectedRoundTeeId() {
        return selectedRoundTeeId;
    }

    public void setSelectedRoundTeeId(Long selectedRoundTeeId) {
        this.selectedRoundTeeId = selectedRoundTeeId;
    }

    public String getSelectedTeeName() {
        return selectedTeeName;
    }

    public void setSelectedTeeName(String selectedTeeName) {
        this.selectedTeeName = selectedTeeName;
    }

    public BigDecimal getSelectedCourseRating() {
        return selectedCourseRating;
    }

    public void setSelectedCourseRating(BigDecimal selectedCourseRating) {
        this.selectedCourseRating = selectedCourseRating;
    }

    public Integer getSelectedSlope() {
        return selectedSlope;
    }

    public void setSelectedSlope(Integer selectedSlope) {
        this.selectedSlope = selectedSlope;
    }

    public Integer getSelectedYardage() {
        return selectedYardage;
    }

    public void setSelectedYardage(Integer selectedYardage) {
        this.selectedYardage = selectedYardage;
    }

    public Integer getSelectedCourseHandicap() {
        return selectedCourseHandicap;
    }

    public void setSelectedCourseHandicap(Integer selectedCourseHandicap) {
        this.selectedCourseHandicap = selectedCourseHandicap;
    }

    public Integer getSelectedPlayingHandicap() {
        return selectedPlayingHandicap;
    }

    public void setSelectedPlayingHandicap(Integer selectedPlayingHandicap) {
        this.selectedPlayingHandicap = selectedPlayingHandicap;
    }

    public List<StrokesPerDayTeeOptionResponse> getEligibleTeeOptions() {
        return eligibleTeeOptions;
    }

    public void setEligibleTeeOptions(List<StrokesPerDayTeeOptionResponse> eligibleTeeOptions) {
        this.eligibleTeeOptions = eligibleTeeOptions;
    }
}
