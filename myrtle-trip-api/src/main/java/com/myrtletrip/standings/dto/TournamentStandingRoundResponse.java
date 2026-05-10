package com.myrtletrip.standings.dto;

public class TournamentStandingRoundResponse {

    private Long roundId;
    private Integer roundNumber;
    private String label;
    private Integer score;
    private Integer toPar;

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getToPar() {
        return toPar;
    }

    public void setToPar(Integer toPar) {
        this.toPar = toPar;
    }
}
