package com.myrtletrip.round.dto;

import java.util.List;

public class RoundCorrectionRequest {

    private List<PlayerCorrectionDto> playerCorrections;
    private List<RoundTeeCorrectionRequest> teeCorrections;
    private List<ParticipationCorrectionDto> participationCorrections;
    private Boolean refreshHandicaps;

    public List<PlayerCorrectionDto> getPlayerCorrections() {
        return playerCorrections;
    }

    public void setPlayerCorrections(List<PlayerCorrectionDto> playerCorrections) {
        this.playerCorrections = playerCorrections;
    }

    public List<RoundTeeCorrectionRequest> getTeeCorrections() {
        return teeCorrections;
    }

    public void setTeeCorrections(List<RoundTeeCorrectionRequest> teeCorrections) {
        this.teeCorrections = teeCorrections;
    }

    public List<ParticipationCorrectionDto> getParticipationCorrections() {
        return participationCorrections;
    }

    public void setParticipationCorrections(List<ParticipationCorrectionDto> participationCorrections) {
        this.participationCorrections = participationCorrections;
    }

    public Boolean getRefreshHandicaps() {
        return refreshHandicaps;
    }

    public void setRefreshHandicaps(Boolean refreshHandicaps) {
        this.refreshHandicaps = refreshHandicaps;
    }

    public static class ParticipationCorrectionDto {
        private Long scorecardId;
        private String participationStatus;
        private Integer withdrawalHoleNumber;

        public Long getScorecardId() {
            return scorecardId;
        }

        public void setScorecardId(Long scorecardId) {
            this.scorecardId = scorecardId;
        }

        public String getParticipationStatus() {
            return participationStatus;
        }

        public void setParticipationStatus(String participationStatus) {
            this.participationStatus = participationStatus;
        }

        public Integer getWithdrawalHoleNumber() {
            return withdrawalHoleNumber;
        }

        public void setWithdrawalHoleNumber(Integer withdrawalHoleNumber) {
            this.withdrawalHoleNumber = withdrawalHoleNumber;
        }
    }

    public static class PlayerCorrectionDto {
        private Long playerId;
        private List<Integer> holes; // nullable entries allowed

        public Long getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        public List<Integer> getHoles() {
            return holes;
        }

        public void setHoles(List<Integer> holes) {
            this.holes = holes;
        }
    }
}
