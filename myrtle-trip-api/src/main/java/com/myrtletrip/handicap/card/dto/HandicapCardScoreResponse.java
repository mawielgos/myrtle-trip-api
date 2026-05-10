package com.myrtletrip.handicap.card.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HandicapCardScoreResponse {

    private Long scoreHistoryEntryId;
    private Integer displaySortOrder;
    private String scoreSection;
    private Boolean pendingForCalculationDate;
    private Boolean usedInPendingIndex;
    private LocalDate scoreDate;
    private String courseName;
    private BigDecimal courseRating;
    private Integer slope;
    private Integer grossScore;
    private Integer adjustedGrossScore;
    private BigDecimal differential;
    private String sourceType;
    private String scoreType;
    private Integer holesPlayed;
    private Integer postingOrder;
    private Boolean manualDifferentialRequired;
    private Boolean eligibleForWindow;
    private Boolean usedInIndex;
    private String exclusionReason;

    public Long getScoreHistoryEntryId() { return scoreHistoryEntryId; }
    public void setScoreHistoryEntryId(Long scoreHistoryEntryId) { this.scoreHistoryEntryId = scoreHistoryEntryId; }

    public Integer getDisplaySortOrder() { return displaySortOrder; }
    public void setDisplaySortOrder(Integer displaySortOrder) { this.displaySortOrder = displaySortOrder; }

    public String getScoreSection() { return scoreSection; }
    public void setScoreSection(String scoreSection) { this.scoreSection = scoreSection; }

    public Boolean getPendingForCalculationDate() { return pendingForCalculationDate; }
    public void setPendingForCalculationDate(Boolean pendingForCalculationDate) { this.pendingForCalculationDate = pendingForCalculationDate; }

    public Boolean getUsedInPendingIndex() { return usedInPendingIndex; }
    public void setUsedInPendingIndex(Boolean usedInPendingIndex) { this.usedInPendingIndex = usedInPendingIndex; }

    public LocalDate getScoreDate() { return scoreDate; }
    public void setScoreDate(LocalDate scoreDate) { this.scoreDate = scoreDate; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public BigDecimal getCourseRating() { return courseRating; }
    public void setCourseRating(BigDecimal courseRating) { this.courseRating = courseRating; }

    public Integer getSlope() { return slope; }
    public void setSlope(Integer slope) { this.slope = slope; }

    public Integer getGrossScore() { return grossScore; }
    public void setGrossScore(Integer grossScore) { this.grossScore = grossScore; }

    public Integer getAdjustedGrossScore() { return adjustedGrossScore; }
    public void setAdjustedGrossScore(Integer adjustedGrossScore) { this.adjustedGrossScore = adjustedGrossScore; }

    public BigDecimal getDifferential() { return differential; }
    public void setDifferential(BigDecimal differential) { this.differential = differential; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getScoreType() { return scoreType; }
    public void setScoreType(String scoreType) { this.scoreType = scoreType; }

    public Integer getHolesPlayed() { return holesPlayed; }
    public void setHolesPlayed(Integer holesPlayed) { this.holesPlayed = holesPlayed; }

    public Integer getPostingOrder() { return postingOrder; }
    public void setPostingOrder(Integer postingOrder) { this.postingOrder = postingOrder; }

    public Boolean getManualDifferentialRequired() { return manualDifferentialRequired; }
    public void setManualDifferentialRequired(Boolean manualDifferentialRequired) { this.manualDifferentialRequired = manualDifferentialRequired; }

    public Boolean getEligibleForWindow() { return eligibleForWindow; }
    public void setEligibleForWindow(Boolean eligibleForWindow) { this.eligibleForWindow = eligibleForWindow; }

    public Boolean getUsedInIndex() { return usedInIndex; }
    public void setUsedInIndex(Boolean usedInIndex) { this.usedInIndex = usedInIndex; }

    public String getExclusionReason() { return exclusionReason; }
    public void setExclusionReason(String exclusionReason) { this.exclusionReason = exclusionReason; }
}
