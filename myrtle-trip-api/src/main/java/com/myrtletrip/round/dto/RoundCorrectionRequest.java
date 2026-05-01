package com.myrtletrip.round.dto;

import java.util.List;

public class RoundCorrectionRequest {

    private List<PlayerCorrectionDto> playerCorrections;
    private List<RoundTeeCorrectionRequest> teeCorrections;
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

    public Boolean getRefreshHandicaps() {
        return refreshHandicaps;
    }

    public void setRefreshHandicaps(Boolean refreshHandicaps) {
        this.refreshHandicaps = refreshHandicaps;
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
