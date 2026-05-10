package com.myrtletrip.trip.dto;

import java.time.LocalDate;

public class TripListResponse {

    private Long tripId;
    private String tripName;
    private String tripCode;
    private Integer tripYear;
    private Long playerCount;
    private Long roundCount;
    private Integer plannedRoundCount;
    private String status;
    private Boolean correctionMode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean canDelete;
    private Boolean archived;
    private Boolean canArchive;
    private Boolean canRestore;

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

    public Long getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(Long playerCount) {
        this.playerCount = playerCount;
    }

    public Long getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(Long roundCount) {
        this.roundCount = roundCount;
    }

    public Integer getPlannedRoundCount() {
        return plannedRoundCount;
    }

    public void setPlannedRoundCount(Integer plannedRoundCount) {
        this.plannedRoundCount = plannedRoundCount;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(Boolean canDelete) {
        this.canDelete = canDelete;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Boolean getCanArchive() {
        return canArchive;
    }

    public void setCanArchive(Boolean canArchive) {
        this.canArchive = canArchive;
    }

    public Boolean getCanRestore() {
        return canRestore;
    }

    public void setCanRestore(Boolean canRestore) {
        this.canRestore = canRestore;
    }
}
