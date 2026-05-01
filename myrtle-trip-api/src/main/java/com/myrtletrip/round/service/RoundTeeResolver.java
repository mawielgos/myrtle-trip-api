package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.scoreentry.entity.Scorecard;
import org.springframework.stereotype.Component;

@Component
public class RoundTeeResolver {

    public RoundTee resolve(Scorecard scorecard) {
        if (scorecard == null) {
            throw new IllegalArgumentException("scorecard is required");
        }

        if (scorecard.getRoundTee() != null) {
            return scorecard.getRoundTee();
        }

        Round round = scorecard.getRound();
        if (round == null) {
            throw new IllegalArgumentException("scorecard.round is required");
        }

        RoundTee defaultRoundTee = round.getDefaultRoundTee();
        if (defaultRoundTee == null) {
            throw new IllegalStateException("Round default tee is not set for round " + round.getId());
        }

        return defaultRoundTee;
    }
}
