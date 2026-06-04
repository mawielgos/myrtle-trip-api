package com.myrtletrip.round.service;

import com.myrtletrip.games.service.RoundGameScoringService;
import com.myrtletrip.prize.service.TripPrizeRecalculationService;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.service.ScoringService;
import com.myrtletrip.scorehistory.service.RoundScoreHistorySyncService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoundRecalculationOrchestrationService {

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final ScoringService scoringService;
    private final RoundGameScoringService roundGameScoringService;
    private final RoundScoreHistorySyncService roundScoreHistorySyncService;
    private final ScorecardHandicapService scorecardHandicapService;
    private final TripPrizeRecalculationService tripPrizeRecalculationService;
    private final EntityManager entityManager;
    private final RoundEventCapabilityService roundEventCapabilityService;

    public RoundRecalculationOrchestrationService(RoundRepository roundRepository,
                                                  ScorecardRepository scorecardRepository,
                                                  ScoringService scoringService,
                                                  RoundGameScoringService roundGameScoringService,
                                                  RoundScoreHistorySyncService roundScoreHistorySyncService,
                                                  ScorecardHandicapService scorecardHandicapService,
                                                  TripPrizeRecalculationService tripPrizeRecalculationService,
                                                  EntityManager entityManager,
                                                  RoundEventCapabilityService roundEventCapabilityService) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.scoringService = scoringService;
        this.roundGameScoringService = roundGameScoringService;
        this.roundScoreHistorySyncService = roundScoreHistorySyncService;
        this.scorecardHandicapService = scorecardHandicapService;
        this.tripPrizeRecalculationService = tripPrizeRecalculationService;
        this.entityManager = entityManager;
        this.roundEventCapabilityService = roundEventCapabilityService;
    }

    @Transactional
    public void handlePostScorecardChange(Long scorecardId) {
        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));

        handlePostRoundChange(scorecard.getRound().getId());
    }

    @Transactional
    public void handlePostRoundChange(Long roundId) {
        // Serialize full-round recalculation for a round. Tee changes from the score-entry
        // page can be fired close together; without a deterministic round-level lock, two
        // concurrent recalculations can update the same hole_score rows in different
        // orders and PostgreSQL can correctly report a deadlock.
        Round round = roundRepository.findByIdForUpdate(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        recalculateCurrentRound(round);

        if (Boolean.TRUE.equals(round.getFinalized())) {
            roundScoreHistorySyncService.syncFinalizedRound(roundId);
        }

        recalculateDownstreamRounds(round);

        Long tripId = round.getTrip().getId();

        // Corrections can change multi-round tournament standings and all persisted payout rows.
        // Flush and clear so the prize recalculation reads the just-saved Scorecard/HoleScore data.
        entityManager.flush();
        entityManager.clear();
        tripPrizeRecalculationService.recalculate(tripId);
    }

    private void recalculateCurrentRound(Round round) {
        List<Scorecard> scorecards = scorecardRepository.findByRound_IdOrderByIdAsc(round.getId());
        for (Scorecard scorecard : scorecards) {
            scoringService.recalculate(scorecard.getId());
        }

        if (roundEventCapabilityService.requiresTeams(round)) {
            roundGameScoringService.recalculateRound(round.getId());
        }
    }

    private void recalculateDownstreamRounds(Round sourceRound) {
        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(sourceRound.getTrip().getId());

        boolean afterSource = false;
        for (Round round : rounds) {
            if (round.getId().equals(sourceRound.getId())) {
                afterSource = true;
                continue;
            }

            if (!afterSource) {
                continue;
            }

            if (!Boolean.TRUE.equals(round.getFinalized())) {
                continue;
            }

            scorecardHandicapService.refreshRoundHandicapsForCorrection(round.getId());

            if (roundEventCapabilityService.requiresTeams(round)) {
                roundGameScoringService.recalculateRound(round.getId());
            }

            roundScoreHistorySyncService.syncFinalizedRound(round.getId());
        }
    }
}
