package com.myrtletrip.round.dto;

import java.util.List;

public class BulkRoundScoreRequest {

    private List<PlayerBulkScoreDto> scorecards;

    public List<PlayerBulkScoreDto> getScorecards() {
        return scorecards;
    }

    public void setScorecards(List<PlayerBulkScoreDto> scorecards) {
        this.scorecards = scorecards;
    }
}