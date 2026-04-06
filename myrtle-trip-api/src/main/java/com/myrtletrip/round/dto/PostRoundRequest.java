package com.myrtletrip.round.dto;

import java.time.LocalDate;
import java.util.List;

public class PostRoundRequest {

    private Long tripId;
    private Long playerId;
    private Long courseId;
    private Long courseTeeId;
    private LocalDate roundDate;

    // Must contain 18 values (hole 1 → 18)
    private List<Integer> holeScores;

    // Optional future use
    private String notes;

    // -------------------
    // GETTERS / SETTERS
    // -------------------

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getCourseTeeId() {
        return courseTeeId;
    }

    public void setCourseTeeId(Long courseTeeId) {
        this.courseTeeId = courseTeeId;
    }

    public LocalDate getRoundDate() {
        return roundDate;
    }

    public void setRoundDate(LocalDate roundDate) {
        this.roundDate = roundDate;
    }

    public List<Integer> getHoleScores() {
        return holeScores;
    }

    public void setHoleScores(List<Integer> holeScores) {
        this.holeScores = holeScores;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}