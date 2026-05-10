package com.myrtletrip.strokes.dto;

import java.util.ArrayList;
import java.util.List;

public class StrokesPerDayTeePlanSaveRequest {

    private List<StrokesPerDayTeePlanItemRequest> changes = new ArrayList<>();

    public List<StrokesPerDayTeePlanItemRequest> getChanges() {
        return changes;
    }

    public void setChanges(List<StrokesPerDayTeePlanItemRequest> changes) {
        this.changes = changes;
    }
}
