package com.myrtletrip.round.dto;

import com.myrtletrip.round.model.RoundFormat;
import java.util.List;

public class TripRoundPlanRequest {

    private List<RoundPlanItem> rounds;

    public List<RoundPlanItem> getRounds() {
        return rounds;
    }

    public void setRounds(List<RoundPlanItem> rounds) {
        this.rounds = rounds;
    }

    public static class RoundPlanItem {
        private Integer roundNumber;
        private RoundFormat format;

        public Integer getRoundNumber() {
            return roundNumber;
        }

        public void setRoundNumber(Integer roundNumber) {
            this.roundNumber = roundNumber;
        }

        public RoundFormat getFormat() {
            return format;
        }

        public void setFormat(RoundFormat format) {
            this.format = format;
        }
    }
}
