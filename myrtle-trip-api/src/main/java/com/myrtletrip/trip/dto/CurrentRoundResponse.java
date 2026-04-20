package com.myrtletrip.trip.dto;

public class CurrentRoundResponse {

    private Long roundId;
    private Integer roundNumber;
    private String roundDate;
    private String format;
    private String courseName;
    private String teeName;
    private boolean finalized;

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getRoundDate() {
		return roundDate;
	}

	public void setRoundDate(String roundDate) {
		this.roundDate = roundDate;
	}

	public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeeName() {
        return teeName;
    }

    public void setTeeName(String teeName) {
        this.teeName = teeName;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }
}
