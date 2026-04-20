package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.entity.TeamHoleScore;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.service.TripService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RoundCompletionService {

    private static final String SOURCE_TRIP_ROUND = "TRIP_ROUND";

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final ScorecardRepository scorecardRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    private final ScorecardHandicapService scorecardHandicapService;
    private final TripService tripService;

    public RoundCompletionService(RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            ScorecardRepository scorecardRepository,
            TeamHoleScoreRepository teamHoleScoreRepository,
            ScoreHistoryEntryRepository scoreHistoryEntryRepository,
            ScorecardHandicapService scorecardHandicapService,
            TripService tripService) {
		this.roundRepository = roundRepository;
		this.roundTeamRepository = roundTeamRepository;
		this.scorecardRepository = scorecardRepository;
		this.teamHoleScoreRepository = teamHoleScoreRepository;
		this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
		this.scorecardHandicapService = scorecardHandicapService;
		this.tripService = tripService;
	}
    @Transactional
    public void finalizeRound(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        if (Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Round already finalized");
        }

        if (round.getFormat() == RoundFormat.TEAM_SCRAMBLE) {
            finalizeTeamScrambleRound(round);
            return;
        }

        // Critical fix:
        // Refresh all scorecard handicaps using current trip history before final totals are validated/posted.
        scorecardHandicapService.refreshRoundHandicaps(roundId);

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
        if (scorecards.isEmpty()) {
            throw new IllegalStateException("Round has no scorecards");
        }

        for (Scorecard sc : scorecards) {
            if (sc.getGrossScore() == null || sc.getAdjustedGrossScore() == null || sc.getNetScore() == null) {
                throw new IllegalStateException(
                        "Round cannot be finalized until all scorecards are complete. "
                                + "Incomplete scorecard for playerId=" + sc.getPlayer().getId()
                );
            }

            if (sc.getRoundTee() == null) {
                throw new IllegalStateException(
                        "Round cannot be finalized until every scorecard has a round tee. "
                                + "Missing round tee for playerId=" + sc.getPlayer().getId()
                );
            }

            if (sc.getCourseHandicap() == null || sc.getPlayingHandicap() == null) {
                throw new IllegalStateException(
                        "Round cannot be finalized until handicaps are populated. "
                                + "Missing handicap for playerId=" + sc.getPlayer().getId()
                );
            }
        }

        for (Scorecard sc : scorecards) {
            if (scoreHistoryEntryRepository.existsByRound_IdAndPlayer_Id(round.getId(), sc.getPlayer().getId())) {
                throw new IllegalStateException(
                        "Score history already exists for round " + round.getId()
                                + " and player " + sc.getPlayer().getId()
                );
            }

            Integer grossScore = sc.getGrossScore();
            Integer adjustedGrossScore = sc.getAdjustedGrossScore();

            BigDecimal rating = resolveCourseRating(sc);
            Integer slope = resolveSlope(sc);
            BigDecimal differential = calculateDifferential(adjustedGrossScore, rating, slope);

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
            entry.setDifferential(differential);
            entry.setSourceType(SOURCE_TRIP_ROUND);
            entry.setIncludedInMyrtleCalc(true);
            entry.setHandicapGroupCode(round.getTrip().getTripCode());
            entry.setHolesPlayed(18);
            entry.setManualDifferentialRequired(false);

            scoreHistoryEntryRepository.save(entry);
        }

        round.setFinalized(true);
        roundRepository.save(round);
        tripService.refreshTripStatusFromRounds(round.getTrip().getId());
    }

    private void finalizeTeamScrambleRound(Round round) {
        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(round.getId());
        if (teams.isEmpty()) {
            throw new IllegalStateException("TEAM_SCRAMBLE round has no assigned teams");
        }

        for (RoundTeam team : teams) {
            List<TeamHoleScore> holeScores =
                    teamHoleScoreRepository.findByRoundTeam_IdOrderByHoleNumberAsc(team.getId());

            if (holeScores.size() != 18) {
                throw new IllegalStateException(
                        "TEAM_SCRAMBLE round requires 18 team hole scores for teamId=" + team.getId()
                );
            }

            for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
                final int expectedHole = holeNumber;
                boolean found = holeScores.stream()
                        .anyMatch(h -> h.getHoleNumber() != null && h.getHoleNumber() == expectedHole);

                if (!found) {
                    throw new IllegalStateException(
                            "TEAM_SCRAMBLE teamId=" + team.getId()
                                    + " is missing hole score for hole " + holeNumber
                    );
                }
            }
        }

        round.setFinalized(true);
        roundRepository.save(round);
        tripService.refreshTripStatusFromRounds(round.getTrip().getId());
    }

    private BigDecimal resolveCourseRating(Scorecard sc) {
        RoundTee roundTee = requireRoundTee(sc);
        return roundTee.getCourseRating();
    }

    private Integer resolveSlope(Scorecard sc) {
        RoundTee roundTee = requireRoundTee(sc);
        return roundTee.getSlope();
    }

    private RoundTee requireRoundTee(Scorecard sc) {
        if (sc.getRoundTee() == null) {
            throw new IllegalStateException("scorecard.roundTee is required");
        }
        return sc.getRoundTee();
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
