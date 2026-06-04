package com.myrtletrip.readiness.dto;

import java.util.ArrayList;
import java.util.List;

public class TripWorkflowReadinessResponse {

    private Long tripId;
    private String tripStatus;

    private Boolean canStartTrip;
    private Boolean canCompleteTrip;
    private Boolean canEditTournament;
    private Boolean canEditPrizeSetup;

    private Boolean rosterReady;
    private Boolean plannedRoundsReady;
    private Boolean handicapIndexesReady;
    private Boolean ghinFixesReady;
    private Boolean roundsCreatedReady;
    private Boolean allRoundsFinalizedReady;
    private Boolean tournamentReady;
    private Boolean prizeSetupReady;

    private Integer activePlayerCount;
    private Integer plannedRoundCount;
    private Integer completedPlannedRoundCount;
    private Integer roundCount;
    private Integer finalizedRoundCount;
    private Integer prizeScheduleCount;
    private Integer incompletePrizeScheduleCount;

    private List<ReadinessIssueResponse> issues = new ArrayList<ReadinessIssueResponse>();

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public String getTripStatus() { return tripStatus; }
    public void setTripStatus(String tripStatus) { this.tripStatus = tripStatus; }
    public Boolean getCanStartTrip() { return canStartTrip; }
    public void setCanStartTrip(Boolean canStartTrip) { this.canStartTrip = canStartTrip; }
    public Boolean getCanCompleteTrip() { return canCompleteTrip; }
    public void setCanCompleteTrip(Boolean canCompleteTrip) { this.canCompleteTrip = canCompleteTrip; }
    public Boolean getCanEditTournament() { return canEditTournament; }
    public void setCanEditTournament(Boolean canEditTournament) { this.canEditTournament = canEditTournament; }
    public Boolean getCanEditPrizeSetup() { return canEditPrizeSetup; }
    public void setCanEditPrizeSetup(Boolean canEditPrizeSetup) { this.canEditPrizeSetup = canEditPrizeSetup; }
    public Boolean getRosterReady() { return rosterReady; }
    public void setRosterReady(Boolean rosterReady) { this.rosterReady = rosterReady; }
    public Boolean getPlannedRoundsReady() { return plannedRoundsReady; }
    public void setPlannedRoundsReady(Boolean plannedRoundsReady) { this.plannedRoundsReady = plannedRoundsReady; }
    public Boolean getHandicapIndexesReady() { return handicapIndexesReady; }
    public void setHandicapIndexesReady(Boolean handicapIndexesReady) { this.handicapIndexesReady = handicapIndexesReady; }
    public Boolean getGhinFixesReady() { return ghinFixesReady; }
    public void setGhinFixesReady(Boolean ghinFixesReady) { this.ghinFixesReady = ghinFixesReady; }
    public Boolean getRoundsCreatedReady() { return roundsCreatedReady; }
    public void setRoundsCreatedReady(Boolean roundsCreatedReady) { this.roundsCreatedReady = roundsCreatedReady; }
    public Boolean getAllRoundsFinalizedReady() { return allRoundsFinalizedReady; }
    public void setAllRoundsFinalizedReady(Boolean allRoundsFinalizedReady) { this.allRoundsFinalizedReady = allRoundsFinalizedReady; }
    public Boolean getTournamentReady() { return tournamentReady; }
    public void setTournamentReady(Boolean tournamentReady) { this.tournamentReady = tournamentReady; }
    public Boolean getPrizeSetupReady() { return prizeSetupReady; }
    public void setPrizeSetupReady(Boolean prizeSetupReady) { this.prizeSetupReady = prizeSetupReady; }
    public Integer getActivePlayerCount() { return activePlayerCount; }
    public void setActivePlayerCount(Integer activePlayerCount) { this.activePlayerCount = activePlayerCount; }
    public Integer getPlannedRoundCount() { return plannedRoundCount; }
    public void setPlannedRoundCount(Integer plannedRoundCount) { this.plannedRoundCount = plannedRoundCount; }
    public Integer getCompletedPlannedRoundCount() { return completedPlannedRoundCount; }
    public void setCompletedPlannedRoundCount(Integer completedPlannedRoundCount) { this.completedPlannedRoundCount = completedPlannedRoundCount; }
    public Integer getRoundCount() { return roundCount; }
    public void setRoundCount(Integer roundCount) { this.roundCount = roundCount; }
    public Integer getFinalizedRoundCount() { return finalizedRoundCount; }
    public void setFinalizedRoundCount(Integer finalizedRoundCount) { this.finalizedRoundCount = finalizedRoundCount; }
    public Integer getPrizeScheduleCount() { return prizeScheduleCount; }
    public void setPrizeScheduleCount(Integer prizeScheduleCount) { this.prizeScheduleCount = prizeScheduleCount; }
    public Integer getIncompletePrizeScheduleCount() { return incompletePrizeScheduleCount; }
    public void setIncompletePrizeScheduleCount(Integer incompletePrizeScheduleCount) { this.incompletePrizeScheduleCount = incompletePrizeScheduleCount; }
    public List<ReadinessIssueResponse> getIssues() { return issues; }
    public void setIssues(List<ReadinessIssueResponse> issues) { this.issues = issues; }
}
