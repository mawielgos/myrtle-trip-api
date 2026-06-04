package com.myrtletrip.event.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundEventsResultResponse {

    private Long roundId;
    private List<RoundEventResultResponse> events = new ArrayList<>();
    private List<RoundEventSnapshotRow> snapshotRows = new ArrayList<>();

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    public List<RoundEventResultResponse> getEvents() { return events; }
    public void setEvents(List<RoundEventResultResponse> events) { this.events = events; }

    public List<RoundEventSnapshotRow> getSnapshotRows() { return snapshotRows; }
    public void setSnapshotRows(List<RoundEventSnapshotRow> snapshotRows) { this.snapshotRows = snapshotRows; }
}
