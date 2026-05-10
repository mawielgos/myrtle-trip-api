package com.myrtletrip.trip.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class TripSetupRequest {

    private Long tripId;
    private String name;
    private Integer tripYear;
    private String tripCode;
    private Integer entryFee;
    private LocalDate tripStartDate;
    private LocalDate tripEndDate;
    private Integer plannedRoundCount;
    private Boolean handicapsEnabled;
    private String handicapMethod;
    private List<Long> playerIds;
    private Map<Long, BigDecimal> frozenHandicapIndexesByPlayerId;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTripYear() {
        return tripYear;
    }

    public void setTripYear(Integer tripYear) {
        this.tripYear = tripYear;
    }

    public String getTripCode() {
        return tripCode;
    }

    public void setTripCode(String tripCode) {
        this.tripCode = tripCode;
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

    public Map<Long, BigDecimal> getFrozenHandicapIndexesByPlayerId() {
        return frozenHandicapIndexesByPlayerId;
    }

    public void setFrozenHandicapIndexesByPlayerId(Map<Long, BigDecimal> frozenHandicapIndexesByPlayerId) {
        this.frozenHandicapIndexesByPlayerId = frozenHandicapIndexesByPlayerId;
    }

    public List<Long> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<Long> playerIds) {
        this.playerIds = playerIds;
    }
}
