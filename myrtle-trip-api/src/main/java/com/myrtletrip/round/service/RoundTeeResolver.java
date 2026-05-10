package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.model.RoundTeeRole;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoundTeeResolver {

    private final RoundTeeRepository roundTeeRepository;

    public RoundTeeResolver(RoundTeeRepository roundTeeRepository) {
        this.roundTeeRepository = roundTeeRepository;
    }

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

        RoundTee playerGenderDefault = resolvePlayerGenderDefaultRoundTee(scorecard, round);
        if (playerGenderDefault != null) {
            return playerGenderDefault;
        }

        RoundTee defaultRoundTee = round.getDefaultRoundTee();
        if (defaultRoundTee != null) {
            return defaultRoundTee;
        }

        RoundTee fallbackRoundTee = resolveFallbackRoundTee(round);
        if (fallbackRoundTee != null) {
            return fallbackRoundTee;
        }

        throw new IllegalStateException("Round default tee is not set for round " + round.getId());
    }


    private RoundTee resolvePlayerGenderDefaultRoundTee(Scorecard scorecard, Round round) {
        if (scorecard == null || scorecard.getPlayer() == null || round == null || round.getId() == null) {
            return null;
        }

        String gender = scorecard.getPlayer().getGender();
        if (!"F".equalsIgnoreCase(gender)) {
            return null;
        }

        List<RoundTee> roundTees = roundTeeRepository.findByRound_IdOrderByTeeNameAsc(round.getId());
        if (roundTees == null || roundTees.isEmpty()) {
            return null;
        }

        for (RoundTee roundTee : roundTees) {
            if (roundTee == null || roundTee.getSourceCourseTee() == null) {
                continue;
            }
            if (roundTee.getSourceCourseTee().isEligibleForGender("F")) {
                return roundTee;
            }
        }

        return null;
    }

    private boolean sameBigDecimal(java.math.BigDecimal a, java.math.BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    private boolean sameInteger(Integer a, Integer b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private RoundTee resolveFallbackRoundTee(Round round) {
        if (round == null || round.getId() == null) {
            return null;
        }

        List<RoundTee> roundTees = roundTeeRepository.findByRound_IdOrderByTeeNameAsc(round.getId());
        if (roundTees == null || roundTees.isEmpty()) {
            return null;
        }

        for (RoundTee roundTee : roundTees) {
            if (roundTee != null && RoundTeeRole.DEFAULT.equals(roundTee.getTeeRole())) {
                return roundTee;
            }
        }


        for (RoundTee roundTee : roundTees) {
            if (roundTee != null) {
                return roundTee;
            }
        }

        return null;
    }
}
