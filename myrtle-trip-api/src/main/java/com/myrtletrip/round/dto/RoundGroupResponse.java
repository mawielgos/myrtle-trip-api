package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundGroupResponse {

    private Long groupId;
    private Integer groupNumber;
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

    public List<RoundGroupPlayerResponse> getPlayers() {
        return players;
    }

    public void setPlayers(List<RoundGroupPlayerResponse> players) {
        this.players = players;
    }
}
