package com.myrtletrip.scorehistory.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.service.RoundTeeResolver;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class RoundScoreHistorySyncService {

    public static final String SOURCE_TRIP_ROUND = "TRIP_ROUND";

    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundTeeResolver roundTeeResolver;

    public RoundScoreHistorySyncService(ScoreHistoryEntryRepository scoreHistoryEntryRepository,
                                        ScorecardRepository scorecardRepository,
                                        RoundTeeResolver roundTeeResolver) {
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundTeeResolver = roundTeeResolver;
    }

    @Transactional
    public void syncFinalizedRound(Long roundId) {
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
        for (Scorecard scorecard : scorecards) {
            syncFinalizedScorecard(scorecard);
        }
    }

    @Transactional
    public void syncFinalizedScorecard(Scorecard scorecard) {
        if (scorecard == null) {
            throw new IllegalArgumentException("scorecard is required");
        }

        Round round = scorecard.getRound();
        if (round == null) {
            throw new IllegalArgumentException("scorecard.round is required");
        }
        if (!Boolean.TRUE.equals(round.getFinalized())) {
            return;
        }
        if (round.getFormat() == RoundFormat.TEAM_SCRAMBLE) {
            return;
        }

        RoundTee effectiveRoundTee = roundTeeResolver.resolve(scorecard);
        if (scorecard.getRoundTee() == null) {
            scorecard.setRoundTee(effectiveRoundTee);
        }

        validateFinalizedScorecard(scorecard);

        Optional<ScoreHistoryEntry> existingOpt = scoreHistoryEntryRepository
                .findByRound_IdAndPlayer_Id(round.getId(), scorecard.getPlayer().getId());

        ScoreHistoryEntry entry = existingOpt.orElseGet(ScoreHistoryEntry::new);
        entry.setPlayer(scorecard.getPlayer());
        entry.setRound(round);
        entry.setCourse(round.getCourse());
        entry.setScoreDate(round.getRoundDate());
        entry.setCourseName(round.getCourse().getName());
        entry.setCourseRating(effectiveRoundTee.getCourseRating());
        entry.setSlope(effectiveRoundTee.getSlope());
        entry.setGrossScore(scorecard.getGrossScore());
        entry.setAdjustedGrossScore(scorecard.getAdjustedGrossScore());
        entry.setDifferential(calculateDifferential(
                scorecard.getAdjustedGrossScore(),
                entry.getCourseRating(),
                entry.getSlope()
        ));
        entry.setSourceType(SOURCE_TRIP_ROUND);
        entry.setIncludedInMyrtleCalc(true);
        // Phase 1 legacy cleanup: used_alternate_tee is retained physically until Phase 2,
        // but the application no longer derives behavior from old alternate/default tee roles.
  //      entry.setUsedAlternateTee(false);
        entry.setHandicapGroupCode(round.getTrip().getTripCode());
        entry.setHolesPlayed(18);
        entry.setManualDifferentialRequired(false);

        scoreHistoryEntryRepository.save(entry);
    }

    private void validateFinalizedScorecard(Scorecard scorecard) {
        if (scorecard.getGrossScore() == null || scorecard.getAdjustedGrossScore() == null || scorecard.getNetScore() == null) {
            throw new IllegalStateException(
                    "Finalized round correction requires complete scorecard totals for playerId="
                            + scorecard.getPlayer().getId()
            );
        }

        RoundTee effectiveRoundTee = roundTeeResolver.resolve(scorecard);
        if (effectiveRoundTee == null || effectiveRoundTee.getId() == null) {
            throw new IllegalStateException(
                    "Finalized round correction requires a round tee for playerId="
                            + scorecard.getPlayer().getId()
            );
        }

        if (scorecard.getCourseHandicap() == null || scorecard.getPlayingHandicap() == null) {
            throw new IllegalStateException(
                    "Finalized round correction requires populated handicaps for playerId="
                            + scorecard.getPlayer().getId()
            );
        }
    }

    private BigDecimal calculateDifferential(Integer adjustedGrossScore,
                                             BigDecimal rating,
                                             Integer slope) {
        if (adjustedGrossScore == null) {
            throw new IllegalArgumentException("adjustedGrossScore is required");
        }
        if (rating == null) {
            throw new IllegalArgumentException("rating is required");
        }
        if (slope == null || slope <= 0) {
            throw new IllegalArgumentException("slope must be > 0");
        }

        BigDecimal numerator = BigDecimal.valueOf(adjustedGrossScore)
                .subtract(rating)
                .multiply(BigDecimal.valueOf(113));

        return numerator.divide(BigDecimal.valueOf(slope), 3, RoundingMode.HALF_UP);
    }
}
