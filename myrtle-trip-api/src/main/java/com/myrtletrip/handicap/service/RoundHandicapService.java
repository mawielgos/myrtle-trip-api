package com.myrtletrip.handicap.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.scoreentry.entity.Scorecard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class RoundHandicapService {

    private final TripHandicapService tripHandicapService;
    private final CourseHandicapService courseHandicapService;

    public RoundHandicapService(TripHandicapService tripHandicapService,
                                CourseHandicapService courseHandicapService) {
        this.tripHandicapService = tripHandicapService;
        this.courseHandicapService = courseHandicapService;
    }

    public BigDecimal calculateTripIndex(Scorecard scorecard, String handicapGroupCode) {
        if (scorecard == null) {
            throw new IllegalArgumentException("scorecard is required");
        }
        if (handicapGroupCode == null || handicapGroupCode.isBlank()) {
            throw new IllegalArgumentException("handicapGroupCode is required");
        }

        Round round = scorecard.getRound();
        Player player = scorecard.getPlayer();

        if (round == null) {
            throw new IllegalArgumentException("scorecard.round is required");
        }
        if (player == null) {
            throw new IllegalArgumentException("scorecard.player is required");
        }
        if (round.getRoundDate() == null) {
            throw new IllegalArgumentException("round.roundDate is required");
        }

        return tripHandicapService.calculateTripIndexAsOf(
                player,
                handicapGroupCode,
                round.getRoundDate()
        );
    }

    public Integer calculateCourseHandicap(Scorecard scorecard, String handicapGroupCode) {
        if (scorecard == null) {
            throw new IllegalArgumentException("scorecard is required");
        }

        Round round = scorecard.getRound();
        if (round == null) {
            throw new IllegalArgumentException("scorecard.round is required");
        }
        if (round.getCourseTee() == null) {
            throw new IllegalArgumentException("round.courseTee is required");
        }

        BigDecimal tripIndex = calculateTripIndex(scorecard, handicapGroupCode);
        if (tripIndex == null) {
            return null;
        }

        if (Boolean.TRUE.equals(scorecard.getUseAlternateTee())) {
            return courseHandicapService.calculateAlternateCourseHandicap(
                    tripIndex,
                    round.getCourseTee()
            );
        }

        return courseHandicapService.calculateCourseHandicap(
                tripIndex,
                round.getCourseTee()
        );
    }

    public Integer calculatePlayingHandicap(Scorecard scorecard, String handicapGroupCode) {
        Integer courseHandicap = calculateCourseHandicap(scorecard, handicapGroupCode);
        if (courseHandicap == null) {
            return null;
        }

        // Temporary rule: playing handicap = course handicap.
        // Later this can apply round-format-specific percentages or other adjustments.
        return courseHandicap;
    }

    public void populateCurrentHandicaps(Scorecard scorecard, String handicapGroupCode) {
        if (scorecard == null) {
            throw new IllegalArgumentException("scorecard is required");
        }

        scorecard.setCourseHandicap(calculateCourseHandicap(scorecard, handicapGroupCode));
        scorecard.setPlayingHandicap(calculatePlayingHandicap(scorecard, handicapGroupCode));
    }
}
