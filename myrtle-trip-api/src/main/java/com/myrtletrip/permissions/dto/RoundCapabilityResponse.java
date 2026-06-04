package com.myrtletrip.permissions.dto;

public class RoundCapabilityResponse {

    private Long roundId;
    private Long tripId;
    private Boolean tripComplete;
    private Boolean tripCorrectionMode;
    private Boolean tripLocked;
    private Boolean roundFinalized;

    private Boolean canAssignTeams;
    private String assignTeamsReason;

    private Boolean canChangeTeeBeforeFinalization;
    private String changeTeeBeforeFinalizationReason;

    private Boolean canCorrectTeeAfterFinalization;
    private String correctTeeAfterFinalizationReason;

    private Boolean readinessReadyForScoring;
    private String readinessReason;

    private Boolean canSaveScores;
    private String saveScoresReason;

    private Boolean canEditCorrections;
    private String editCorrectionsReason;

    private Boolean canEditScrambleSetup;
    private String editScrambleSetupReason;

    private Boolean canViewScoring;
    private String viewScoringReason;

    private Boolean canViewResults;
    private String viewResultsReason;

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }

    public Boolean getTripComplete() { return tripComplete; }
    public void setTripComplete(Boolean tripComplete) { this.tripComplete = tripComplete; }

    public Boolean getTripCorrectionMode() { return tripCorrectionMode; }
    public void setTripCorrectionMode(Boolean tripCorrectionMode) { this.tripCorrectionMode = tripCorrectionMode; }

    public Boolean getTripLocked() { return tripLocked; }
    public void setTripLocked(Boolean tripLocked) { this.tripLocked = tripLocked; }

    public Boolean getRoundFinalized() { return roundFinalized; }
    public void setRoundFinalized(Boolean roundFinalized) { this.roundFinalized = roundFinalized; }

    public Boolean getCanAssignTeams() { return canAssignTeams; }
    public void setCanAssignTeams(Boolean canAssignTeams) { this.canAssignTeams = canAssignTeams; }

    public String getAssignTeamsReason() { return assignTeamsReason; }
    public void setAssignTeamsReason(String assignTeamsReason) { this.assignTeamsReason = assignTeamsReason; }

    public Boolean getCanChangeTeeBeforeFinalization() { return canChangeTeeBeforeFinalization; }
    public void setCanChangeTeeBeforeFinalization(Boolean canChangeTeeBeforeFinalization) { this.canChangeTeeBeforeFinalization = canChangeTeeBeforeFinalization; }

    public String getChangeTeeBeforeFinalizationReason() { return changeTeeBeforeFinalizationReason; }
    public void setChangeTeeBeforeFinalizationReason(String changeTeeBeforeFinalizationReason) { this.changeTeeBeforeFinalizationReason = changeTeeBeforeFinalizationReason; }

    public Boolean getCanCorrectTeeAfterFinalization() { return canCorrectTeeAfterFinalization; }
    public void setCanCorrectTeeAfterFinalization(Boolean canCorrectTeeAfterFinalization) { this.canCorrectTeeAfterFinalization = canCorrectTeeAfterFinalization; }

    public String getCorrectTeeAfterFinalizationReason() { return correctTeeAfterFinalizationReason; }
    public void setCorrectTeeAfterFinalizationReason(String correctTeeAfterFinalizationReason) { this.correctTeeAfterFinalizationReason = correctTeeAfterFinalizationReason; }

    public Boolean getReadinessReadyForScoring() { return readinessReadyForScoring; }
    public void setReadinessReadyForScoring(Boolean readinessReadyForScoring) { this.readinessReadyForScoring = readinessReadyForScoring; }

    public String getReadinessReason() { return readinessReason; }
    public void setReadinessReason(String readinessReason) { this.readinessReason = readinessReason; }

    public Boolean getCanSaveScores() { return canSaveScores; }
    public void setCanSaveScores(Boolean canSaveScores) { this.canSaveScores = canSaveScores; }

    public String getSaveScoresReason() { return saveScoresReason; }
    public void setSaveScoresReason(String saveScoresReason) { this.saveScoresReason = saveScoresReason; }

    public Boolean getCanEditCorrections() { return canEditCorrections; }
    public void setCanEditCorrections(Boolean canEditCorrections) { this.canEditCorrections = canEditCorrections; }

    public String getEditCorrectionsReason() { return editCorrectionsReason; }
    public void setEditCorrectionsReason(String editCorrectionsReason) { this.editCorrectionsReason = editCorrectionsReason; }

    public Boolean getCanEditScrambleSetup() { return canEditScrambleSetup; }
    public void setCanEditScrambleSetup(Boolean canEditScrambleSetup) { this.canEditScrambleSetup = canEditScrambleSetup; }

    public String getEditScrambleSetupReason() { return editScrambleSetupReason; }
    public void setEditScrambleSetupReason(String editScrambleSetupReason) { this.editScrambleSetupReason = editScrambleSetupReason; }

    public Boolean getCanViewScoring() { return canViewScoring; }
    public void setCanViewScoring(Boolean canViewScoring) { this.canViewScoring = canViewScoring; }

    public String getViewScoringReason() { return viewScoringReason; }
    public void setViewScoringReason(String viewScoringReason) { this.viewScoringReason = viewScoringReason; }

    public Boolean getCanViewResults() { return canViewResults; }
    public void setCanViewResults(Boolean canViewResults) { this.canViewResults = canViewResults; }

    public String getViewResultsReason() { return viewResultsReason; }
    public void setViewResultsReason(String viewResultsReason) { this.viewResultsReason = viewResultsReason; }
}
