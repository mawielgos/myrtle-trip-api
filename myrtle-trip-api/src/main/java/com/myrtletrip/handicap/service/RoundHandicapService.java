package com.myrtletrip.handicap.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.service.RoundTeeResolver;
import com.myrtletrip.scoreentry.entity.Scorecard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class RoundHandicapService {

    private final TripHandicapService tripHandicapService;
    private final CourseHandicapService courseHandicapService;
    private final RoundTeeResolver roundTeeResolver;

    public RoundHandicapService(TripHandicapService tripHandicapService,
                                CourseHandicapService courseHandicapService,
                                RoundTeeResolver roundTeeResolver) {
        this.tripHandicapService = tripHandicapService;
        this.courseHandicapService = courseHandicapService;
        this.roundTeeResolver = roundTeeResolver;
    }

    public BigDecimal calculateTripIndex(Scorecard scorecard, String handicapGroupCode) {
        if (scorecard == null) throw new IllegalArgumentException("scorecard is required");
        if (handicapGroupCode == null || handicapGroupCode.isBlank()) throw new IllegalArgumentException("handicapGroupCode is required");

        Round round = scorecard.getRound();
        Player player = scorecard.getPlayer();

        if (round == null) throw new IllegalArgumentException("scorecard.round is required");
        if (player == null) throw new IllegalArgumentException("scorecard.player is required");
        if (round.getRoundDate() == null) throw new IllegalArgumentException("round.roundDate is required");

        return tripHandicapService.calculateTripIndexAsOf(player, handicapGroupCode, round.getRoundDate());
    }

    public Integer calculateCourseHandicap(Scorecard scorecard, String handicapGroupCode) {
        if (scorecard == null) throw new IllegalArgumentException("scorecard is required");
        RoundTee roundTee = roundTeeResolver.resolve(scorecard);

        BigDecimal tripIndex = calculateTripIndex(scorecard, handicapGroupCode);
        if (tripIndex == null) return null;

        return courseHandicapService.calculateCourseHandicap(tripIndex, roundTee);
    }

    public Integer calculatePlayingHandicap(Scorecard scorecard, String handicapGroupCode) {
        Integer courseHandicap = calculateCourseHandicap(scorecard, handicapGroupCode);
        if (courseHandicap == null) return null;

        Round round = scorecard.getRound();
        if (round == null) throw new IllegalArgumentException("scorecard.round is required");

        int handicapPercent = normalizeHandicapPercent(round.getHandicapPercent());
        return (int) Math.round(courseHandicap * (handicapPercent / 100.0));
    }

    public void populateCurrentHandicaps(Scorecard scorecard, String handicapGroupCode) {
        if (scorecard == null) throw new IllegalArgumentException("scorecard is required");

        Integer courseHandicap = calculateCourseHandicap(scorecard, handicapGroupCode);
        scorecard.setCourseHandicap(courseHandicap);

        if (courseHandicap == null) {
            scorecard.setPlayingHandicap(null);
            return;
        }

        scorecard.setPlayingHandicap(calculatePlayingHandicap(scorecard, handicapGroupCode));
    }

    private int normalizeHandicapPercent(Integer handicapPercent) {
        if (handicapPercent == null) return 100;
        if (handicapPercent < 0 || handicapPercent > 100) throw new IllegalArgumentException("handicapPercent must be between 0 and 100");
        return handicapPercent;
    }
}
