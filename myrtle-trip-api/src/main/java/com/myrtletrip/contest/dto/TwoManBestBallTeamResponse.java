package com.myrtletrip.contest.dto;

import java.util.List;

public class TwoManBestBallTeamResponse {

    private Long teamId;
    private Integer teamNumber;
    private String teamName;
    private Integer totalBestBall;
    private Integer rank;
    private Boolean tied;
    private List<String> players;
    private List<TwoManBestBallHoleResponse> holes;

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Integer getTeamNumber() {
        return teamNumber;
    }

    public void setTeamNumber(Integer teamNumber) {
        this.teamNumber = teamNumber;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Integer getTotalBestBall() {
        return totalBestBall;
    }

    public void setTotalBestBall(Integer totalBestBall) {
        this.totalBestBall = totalBestBall;
    }

    public List<String> getPlayers() {
        return players;
    }

    public Integer getRank() {
		return rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

	public Boolean getTied() {
		return tied;
	}

	public void setTied(Boolean tied) {
		this.tied = tied;
	}

	public void setPlayers(List<String> players) {
        this.players = players;
    }

    public List<TwoManBestBallHoleResponse> getHoles() {
        return holes;
    }

    public void setHoles(List<TwoManBestBallHoleResponse> holes) {
        this.holes = holes;
    }
}