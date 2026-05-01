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

    public RoundRecalculationOrchestrationService(RoundRepository roundRepository,
                                                  ScorecardRepository scorecardRepository,
                                                  ScoringService scoringService,
                                                  RoundGameScoringService roundGameScoringService,
                                                  RoundScoreHistorySyncService roundScoreHistorySyncService,
                                                  ScorecardHandicapService scorecardHandicapService,
                                                  TripPrizeRecalculationService tripPrizeRecalculationService,
                                                  EntityManager entityManager) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.scoringService = scoringService;
        this.roundGameScoringService = roundGameScoringService;
        this.roundScoreHistorySyncService = roundScoreHistorySyncService;
        this.scorecardHandicapService = scorecardHandicapService;
        this.tripPrizeRecalculationService = tripPrizeRecalculationService;
        this.entityManager = entityManager;
    }

    @Transactional
    public void handlePostScorecardChange(Long scorecardId) {
        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));

        handlePostRoundChange(scorecard.getRound().getId());
    }

    @Transactional
    public void handlePostRoundChange(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        recalculateCurrentRound(round);

        if (Boolean.TRUE.equals(round.getFinalized())) {
            roundScoreHistorySyncService.syncFinalizedRound(roundId);
        }

        recalculateDownstreamRounds(round);

        Long tripId = round.getTrip().getId();

        // Corrections can change Myrtles Cup standings and all persisted payout rows.
        // Flush and clear so the prize recalculation reads the just-saved Scorecard/HoleScore data.
        entityManager.flush();
        entityManager.clear();
        tripPrizeRecalculationService.recalculate(tripId);
    }

    private void recalculateCurrentRound(Round round) {
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(round.getId());
        for (Scorecard scorecard : scorecards) {
            scoringService.recalculate(scorecard.getId());
        }

        if (round.getFormat() != null && round.getFormat().requiresTeams()) {
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

            if (round.getFormat() != null && round.getFormat().requiresTeams()) {
                roundGameScoringService.recalculateRound(round.getId());
            }

            roundScoreHistorySyncService.syncFinalizedRound(round.getId());
        }
    }
}
