package com.myrtletrip.round.dto;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class RoundGroupResponse {

    private Long groupId;
    private Integer groupNumber;
    private LocalTime teeTime;
    private List<RoundGroupPlayerResponse> players = new ArrayList<>();

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

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

    public List<RoundGroupPlayerResponse> getPlayers() {
        return players;
    }

    public void setPlayers(List<RoundGroupPlayerResponse> players) {
        this.players = players;
    }
}
