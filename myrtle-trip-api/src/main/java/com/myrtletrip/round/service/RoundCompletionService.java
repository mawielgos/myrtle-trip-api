package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.entity.TeamHoleScore;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.scorehistory.service.RoundScoreHistorySyncService;
import com.myrtletrip.trip.service.TripService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoundCompletionService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final ScorecardRepository scorecardRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final RoundScoreHistorySyncService roundScoreHistorySyncService;
    private final ScorecardHandicapService scorecardHandicapService;
    private final TripService tripService;

    public RoundCompletionService(RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            ScorecardRepository scorecardRepository,
            TeamHoleScoreRepository teamHoleScoreRepository,
            RoundScoreHistorySyncService roundScoreHistorySyncService,
            ScorecardHandicapService scorecardHandicapService,
            TripService tripService) {
		this.roundRepository = roundRepository;
		this.roundTeamRepository = roundTeamRepository;
		this.scorecardRepository = scorecardRepository;
		this.teamHoleScoreRepository = teamHoleScoreRepository;
		this.roundScoreHistorySyncService = roundScoreHistorySyncService;
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

        round.setFinalized(true);
        roundRepository.save(round);

        for (Scorecard sc : scorecards) {
            roundScoreHistorySyncService.syncFinalizedScorecard(sc);
        }
        tripService.refreshTripStatusFromRounds(round.getTrip().getId());
    }

    private void finalizeTeamScrambleRound(Round round) {
        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(round.getId());
        if (teams.isEmpty()) {
            throw new IllegalStateException("TEAM_SCRAMBLE round has no assigned teams");
        }

        for (RoundTeam team : teams) {
            if (team.getScrambleTotalScore() != null) {
                continue;
            }

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

}
