package com.myrtletrip.games.dto;

import java.util.ArrayList;
import java.util.List;

public class TeamGameResult {

    private Long teamId;
    private String teamName;
    private Integer totalGross = 0;
    private Integer totalNet = 0;
    private Integer totalPoints = 0;
    private Integer placement;
    private List<HoleGameResult> holeResults = new ArrayList<>();

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Integer getTotalGross() {
        return totalGross;
    }

    public void setTotalGross(Integer totalGross) {
        this.totalGross = totalGross;
    }

    public Integer getTotalNet() {
        return totalNet;
    }

    public void setTotalNet(Integer totalNet) {
        this.totalNet = totalNet;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Integer getPlacement() {
        return placement;
    }

    public void setPlacement(Integer placement) {
        this.placement = placement;
    }

    public List<HoleGameResult> getHoleResults() {
        return holeResults;
    }

    public void setHoleResults(List<HoleGameResult> holeResults) {
        this.holeResults = holeResults;
    }
}