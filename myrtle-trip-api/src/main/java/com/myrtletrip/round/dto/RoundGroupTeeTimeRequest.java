package com.myrtletrip.round.dto;

import java.time.LocalTime;

public class RoundGroupTeeTimeRequest {

    private Integer groupNumber;
    private LocalTime teeTime;

    public Integer getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(Integer groupNumber) {
        this.groupNumber = groupNumber;
    }

    public LocalTime getTeeTime() {
        return teeTime;
    }

    public void setTeeTime(LocalTime teeTime) {
        this.teeTime = teeTime;
    }
}
