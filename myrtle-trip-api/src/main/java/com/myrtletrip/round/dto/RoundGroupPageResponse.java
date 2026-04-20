package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class RoundGroupPageResponse {

    private Long roundId;
    private List<RoundGroupResponse> groups = new ArrayList<>();

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public List<RoundGroupResponse> getGroups() {
        return groups;
    }

    public void setGroups(List<RoundGroupResponse> groups) {
        this.groups = groups;
    }
}
