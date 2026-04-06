package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RoundCompletionService {

    private static final String SOURCE_TRIP_ROUND = "TRIP_ROUND";

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    private final TripHandicapService tripHandicapService;

    public RoundCompletionService(RoundRepository roundRepository,
                                  ScorecardRepository scorecardRepository,
                                  ScoreHistoryEntryRepository scoreHistoryEntryRepository,
                                  TripHandicapService tripHandicapService) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
        this.tripHandicapService = tripHandicapService;
    }

    @Transactional
    public void finalizeRound(Long roundId) {

        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        if (Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Round already finalized");
        }

        if (round.getFormat() == com.myrtletrip.round.model.RoundFormat.TEAM_SCRAMBLE) {
            throw new IllegalStateException("TEAM_SCRAMBLE rounds should not be finalized through player scorecard posting");
        }

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);

        if (scorecards.isEmpty()) {
            throw new IllegalStateException("Round has no scorecards");
        }

        for (Scorecard sc : scorecards) {
            if (sc.getGrossScore() == null || sc.getAdjustedGrossScore() == null) {
                throw new IllegalStateException(
                        "Round cannot be finalized until all scorecards are complete. Incomplete scorecard for playerId="
                                + sc.getPlayer().getId()
                );
            }
        }

        for (Scorecard sc : scorecards) {
            Integer grossScore = sc.getGrossScore();
            Integer adjustedGrossScore = sc.getAdjustedGrossScore();

            BigDecimal rating = resolveCourseRating(sc);
            Integer slope = resolveSlope(sc);

            BigDecimal differential = calculateDifferential(
                    adjustedGrossScore,
                    rating,
                    slope
            );

            if (scoreHistoryEntryRepository.existsByRound_IdAndPlayer_Id(round.getId(), sc.getPlayer().getId())) {
                throw new IllegalStateException("Score history already exists for round " + round.getId()
                        + " and player " + sc.getPlayer().getId());
            }

            ScoreHistoryEntry entry = new ScoreHistoryEntry();
            entry.setPlayer(sc.getPlayer());
            entry.setRound(round);
            entry.setCourse(round.getCourse());
            entry.setScoreDate(round.getRoundDate());
            entry.setCourseName(round.getCourse().getName());
            entry.setCourseRating(rating);
            entry.setSlope(slope);
            entry.setGrossScore(grossScore);
            entry.setAdjustedGrossScore(adjustedGrossScore);
            entry.setUsedAlternateTee(Boolean.TRUE.equals(sc.getUseAlternateTee()));
            entry.setDifferential(differential);
            entry.setSourceType(SOURCE_TRIP_ROUND);
            entry.setIncludedInMyrtleCalc(true);
            entry.setHandicapGroupCode(round.getTrip().getTripCode());
            entry.setHolesPlayed(18);
            entry.setManualDifferentialRequired(false);

            scoreHistoryEntryRepository.save(entry);

            tripHandicapService.calculateTripIndex(
                    sc.getPlayer(),
                    round.getTrip().getTripCode()
            );
        }

        round.setFinalized(true);
        roundRepository.save(round);
    }

    private BigDecimal resolveCourseRating(Scorecard sc) {
        if (Boolean.TRUE.equals(sc.getUseAlternateTee())
                && sc.getRound().getCourseTee().getAlternateCourseRating() != null) {
            return sc.getRound().getCourseTee().getAlternateCourseRating();
        }
        return sc.getRound().getCourseTee().getCourseRating();
    }

    private Integer resolveSlope(Scorecard sc) {
        if (Boolean.TRUE.equals(sc.getUseAlternateTee())
                && sc.getRound().getCourseTee().getAlternateSlope() != null) {
            return sc.getRound().getCourseTee().getAlternateSlope();
        }
        return sc.getRound().getCourseTee().getSlope();
    }

    private BigDecimal calculateDifferential(Integer adjustedGrossScore,
                                             BigDecimal rating,
                                             Integer slope) {

        BigDecimal numerator = BigDecimal.valueOf(adjustedGrossScore)
                .subtract(rating)
                .multiply(BigDecimal.valueOf(113));

        return numerator.divide(BigDecimal.valueOf(slope), 1, RoundingMode.HALF_UP);
    }
}