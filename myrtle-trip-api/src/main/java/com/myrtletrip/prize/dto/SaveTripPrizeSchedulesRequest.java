package com.myrtletrip.prize.dto;

import java.util.ArrayList;
import java.util.List;

public class SaveTripPrizeSchedulesRequest {
    private List<SavePrizeScheduleRequest> schedules = new ArrayList<>();

    public List<SavePrizeScheduleRequest> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<SavePrizeScheduleRequest> schedules) {
        this.schedules = schedules;
    }
}
