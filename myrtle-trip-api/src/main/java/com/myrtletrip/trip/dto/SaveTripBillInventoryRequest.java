package com.myrtletrip.trip.dto;

public class SaveTripBillInventoryRequest {

    private Long tripId;
    private Integer hundredsCount;
    private Integer fiftiesCount;
    private Integer twentiesCount;
    private Integer tensCount;
    private Integer fivesCount;
    private Integer onesCount;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Integer getHundredsCount() {
        return hundredsCount;
    }

    public void setHundredsCount(Integer hundredsCount) {
        this.hundredsCount = hundredsCount;
    }

    public Integer getFiftiesCount() {
        return fiftiesCount;
    }

    public void setFiftiesCount(Integer fiftiesCount) {
        this.fiftiesCount = fiftiesCount;
    }

    public Integer getTwentiesCount() {
        return twentiesCount;
    }

    public void setTwentiesCount(Integer twentiesCount) {
        this.twentiesCount = twentiesCount;
    }

    public Integer getTensCount() {
        return tensCount;
    }

    public void setTensCount(Integer tensCount) {
        this.tensCount = tensCount;
    }

    public Integer getFivesCount() {
        return fivesCount;
    }

    public void setFivesCount(Integer fivesCount) {
        this.fivesCount = fivesCount;
    }

    public Integer getOnesCount() {
        return onesCount;
    }

    public void setOnesCount(Integer onesCount) {
        this.onesCount = onesCount;
    }
}
