package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundGroupAssignmentRequest {

    private List<RoundGroupAssignmentItemRequest> assignments = new ArrayList<>();
    private List<RoundGroupTeeTimeRequest> groupTeeTimes = new ArrayList<>();

    public List<RoundGroupTeeTimeRequest> getGroupTeeTimes() {
        return groupTeeTimes;
    }

    public void setGroupTeeTimes(List<RoundGroupTeeTimeRequest> groupTeeTimes) {
        this.groupTeeTimes = groupTeeTimes;
    }

    public List<RoundGroupAssignmentItemRequest> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<RoundGroupAssignmentItemRequest> assignments) {
        this.assignments = assignments;
    }
}
