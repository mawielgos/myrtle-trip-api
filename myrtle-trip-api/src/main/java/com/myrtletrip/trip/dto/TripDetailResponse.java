package com.myrtletrip.trip.dto;

import java.time.LocalDate;

public class TripDetailResponse {

    private Long tripId;
    private String tripName;
    private String tripCode;
    private Integer tripYear;
    private Integer entryFee;
    private LocalDate tripStartDate;
    private LocalDate tripEndDate;
    private Integer plannedRoundCount;
    private Boolean handicapsEnabled;
    private String handicapMethod;
    private Boolean initialized;
    private String status;
    private Boolean correctionMode;
    private Boolean archived;
    private CurrentRoundResponse currentRound;
    private Long unresolvedGhinFixCount;
    private TripReadinessResponse readiness;
    private Boolean hasFemalePlayers;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getTripName() {
        return tripName;
    }

    public void setTripName(String tripName) {
        this.tripName = tripName;
    }

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
    }

    public Integer getTripYear() {
        return tripYear;
    }

    public void setTripYear(Integer tripYear) {
        this.tripYear = tripYear;
    }

    public Integer getEntryFee() {
        return entryFee;
    }

    public void setEntryFee(Integer entryFee) {
        this.entryFee = entryFee;
    }

    public LocalDate getTripStartDate() {
        return tripStartDate;
    }

    public void setTripStartDate(LocalDate tripStartDate) {
        this.tripStartDate = tripStartDate;
    }

    public LocalDate getTripEndDate() {
        return tripEndDate;
    }

    public void setTripEndDate(LocalDate tripEndDate) {
        this.tripEndDate = tripEndDate;
    }

    public Integer getPlannedRoundCount() {
        return plannedRoundCount;
    }

    public void setPlannedRoundCount(Integer plannedRoundCount) {
        this.plannedRoundCount = plannedRoundCount;
    }

    public Boolean getHandicapsEnabled() {
        return handicapsEnabled;
    }

    public void setHandicapsEnabled(Boolean handicapsEnabled) {
        this.handicapsEnabled = handicapsEnabled;
    }

    public String getHandicapMethod() {
        return handicapMethod;
    }

    public void setHandicapMethod(String handicapMethod) {
        this.handicapMethod = handicapMethod;
    }

    public Boolean getInitialized() {
        return initialized;
    }

    public void setInitialized(Boolean initialized) {
        this.initialized = initialized;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getCorrectionMode() {
        return correctionMode;
    }

    public void setCorrectionMode(Boolean correctionMode) {
        this.correctionMode = correctionMode;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public CurrentRoundResponse getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(CurrentRoundResponse currentRound) {
        this.currentRound = currentRound;
    }

    public Long getUnresolvedGhinFixCount() {
        return unresolvedGhinFixCount;
    }

    public void setUnresolvedGhinFixCount(Long unresolvedGhinFixCount) {
        this.unresolvedGhinFixCount = unresolvedGhinFixCount;
    }

    public TripReadinessResponse getReadiness() {
        return readiness;
    }

    public void setReadiness(TripReadinessResponse readiness) {
        this.readiness = readiness;
    }

    public Boolean getHasFemalePlayers() {
        return hasFemalePlayers;
    }

    public void setHasFemalePlayers(Boolean hasFemalePlayers) {
        this.hasFemalePlayers = hasFemalePlayers;
    }
}
