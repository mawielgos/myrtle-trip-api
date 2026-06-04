package com.myrtletrip.event.dto;

import com.myrtletrip.event.model.RoundEventType;

public class RoundEventSnapshotRow {

    private Long eventId;
    private RoundEventType eventType;
    private String eventName;
    private String resultKind;
    private String winnerName;
    private Integer winningTotal;
    private Integer rank;
    private Boolean tied;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public RoundEventType getEventType() { return eventType; }
    public void setEventType(RoundEventType eventType) { this.eventType = eventType; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getResultKind() { return resultKind; }
    public void setResultKind(String resultKind) { this.resultKind = resultKind; }

    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }

    public Integer getWinningTotal() { return winningTotal; }
    public void setWinningTotal(Integer winningTotal) { this.winningTotal = winningTotal; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public Boolean getTied() { return tied; }
    public void setTied(Boolean tied) { this.tied = tied; }
}
