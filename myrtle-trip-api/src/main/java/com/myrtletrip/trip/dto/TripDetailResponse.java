package com.myrtletrip.trip.dto;

public class TripDetailResponse {

    private Long tripId;
    private String tripName;
    private String tripCode;
    private Integer tripYear;
    private Integer entryFee;
    private Boolean initialized;
    private String status;
    private CurrentRoundResponse currentRound;
    private Long unresolvedGhinFixCount;
    private TripReadinessResponse readiness;

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
}
