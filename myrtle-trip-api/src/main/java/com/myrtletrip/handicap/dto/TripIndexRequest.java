package com.myrtletrip.handicap.dto;

import java.util.List;

public class TripIndexRequest {

    private String groupCode;
    private List<Long> playerIds;

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public List<Long> getPlayerIds() {
        return playerIds;
    }

    public void setPlayerIds(List<Long> playerIds) {
        this.playerIds = playerIds;
    }
}