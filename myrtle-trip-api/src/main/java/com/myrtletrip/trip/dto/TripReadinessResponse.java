package com.myrtletrip.trip.dto;

import java.util.ArrayList;
import java.util.List;

public class TripReadinessResponse {

    private Integer activePlayerCount;
    private Integer plannedRoundCount;
    private Integer completedPlannedRoundCount;
    private Long unresolvedGhinFixCount;

    private Boolean rosterReady;
    private Boolean plannedRoundsReady;
    private Boolean ghinFixesReady;
    private Boolean canStartTrip;

    private List<String> blockingItems = new ArrayList<String>();

    public Integer getActivePlayerCount() {
        return activePlayerCount;
    }

    public void setActivePlayerCount(Integer activePlayerCount) {
        this.activePlayerCount = activePlayerCount;
    }

    public Integer getPlannedRoundCount() {
        return plannedRoundCount;
    }

    public void setPlannedRoundCount(Integer plannedRoundCount) {
        this.plannedRoundCount = plannedRoundCount;
    }

    public Integer getCompletedPlannedRoundCount() {
        return completedPlannedRoundCount;
    }

    public void setCompletedPlannedRoundCount(Integer completedPlannedRoundCount) {
        this.completedPlannedRoundCount = completedPlannedRoundCount;
    }

    public Long getUnresolvedGhinFixCount() {
        return unresolvedGhinFixCount;
    }

    public void setUnresolvedGhinFixCount(Long unresolvedGhinFixCount) {
        this.unresolvedGhinFixCount = unresolvedGhinFixCount;
    }

    public Boolean getRosterReady() {
        return rosterReady;
    }

    public void setRosterReady(Boolean rosterReady) {
        this.rosterReady = rosterReady;
    }

    public Boolean getPlannedRoundsReady() {
        return plannedRoundsReady;
    }

    public void setPlannedRoundsReady(Boolean plannedRoundsReady) {
        this.plannedRoundsReady = plannedRoundsReady;
    }

    public Boolean getGhinFixesReady() {
        return ghinFixesReady;
    }

    public void setGhinFixesReady(Boolean ghinFixesReady) {
        this.ghinFixesReady = ghinFixesReady;
    }

    public Boolean getCanStartTrip() {
        return canStartTrip;
    }

    public void setCanStartTrip(Boolean canStartTrip) {
        this.canStartTrip = canStartTrip;
    }

    public List<String> getBlockingItems() {
        return blockingItems;
    }

    public void setBlockingItems(List<String> blockingItems) {
        this.blockingItems = blockingItems;
    }
}
