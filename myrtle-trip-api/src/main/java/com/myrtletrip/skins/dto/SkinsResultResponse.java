package com.myrtletrip.skins.dto;

import java.util.List;

public class SkinsResultResponse {

    private List<SkinHoleResultResponse> holes;
    private List<PlayerSkinSummaryResponse> playerSummaries;

    public List<SkinHoleResultResponse> getHoles() {
        return holes;
    }

    public void setHoles(List<SkinHoleResultResponse> holes) {
        this.holes = holes;
    }

    public List<PlayerSkinSummaryResponse> getPlayerSummaries() {
        return playerSummaries;
    }

    public void setPlayerSummaries(List<PlayerSkinSummaryResponse> playerSummaries) {
        this.playerSummaries = playerSummaries;
    }
}