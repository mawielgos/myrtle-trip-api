package com.myrtletrip.trip.entity;

import com.myrtletrip.trip.model.TripHandicapMethod;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;

    @Column(name = "trip_year", nullable = false)
    private Integer tripYear;

    @Column(name = "trip_code", nullable = false, unique = true)
    private String tripCode;

    @Column(name = "initialized", nullable = false)
    private Boolean initialized = false;

    @Column(name = "entry_fee")
    private Integer entryFee;

    @Column(name = "trip_start_date")
    private LocalDate tripStartDate;

    @Column(name = "trip_end_date")
    private LocalDate tripEndDate;

    @Column(name = "planned_round_count", nullable = false)
    private Integer plannedRoundCount = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TripStatus status = TripStatus.PLANNING;

    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "correction_mode", nullable = false)
    private Boolean correctionMode = false;

    @Column(name = "handicaps_enabled", nullable = false)
    private Boolean handicapsEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "handicap_method", nullable = false, length = 40)
    private TripHandicapMethod handicapMethod = TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY;

    public Long getId() {
        return id;
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

    public Boolean getInitialized() {
        return initialized;
    }

    public void setInitialized(Boolean initialized) {
        this.initialized = initialized;
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

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public boolean isArchived() {
        return Boolean.TRUE.equals(archived);
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Boolean getCorrectionMode() {
        return correctionMode;
    }

    public void setCorrectionMode(Boolean correctionMode) {
        this.correctionMode = correctionMode;
    }

    public boolean isCorrectionMode() {
        return Boolean.TRUE.equals(correctionMode);
    }

    public Boolean getHandicapsEnabled() {
        return handicapsEnabled;
    }

    public void setHandicapsEnabled(Boolean handicapsEnabled) {
        this.handicapsEnabled = handicapsEnabled;
    }

    public boolean isHandicapsEnabled() {
        return Boolean.TRUE.equals(handicapsEnabled);
    }

    public TripHandicapMethod getHandicapMethod() {
        return handicapMethod;
    }

    public void setHandicapMethod(TripHandicapMethod handicapMethod) {
        this.handicapMethod = handicapMethod;
    }
}

