package com.myrtletrip.round.service;

import com.myrtletrip.games.service.RoundGameScoringService;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.service.ScoringService;
import com.myrtletrip.scorehistory.service.RoundScoreHistorySyncService;
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

    public RoundRecalculationOrchestrationService(RoundRepository roundRepository,
                                                  ScorecardRepository scorecardRepository,
                                                  ScoringService scoringService,
                                                  RoundGameScoringService roundGameScoringService,
                                                  RoundScoreHistorySyncService roundScoreHistorySyncService,
                                                  ScorecardHandicapService scorecardHandicapService) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.scoringService = scoringService;
        this.roundGameScoringService = roundGameScoringService;
        this.roundScoreHistorySyncService = roundScoreHistorySyncService;
        this.scorecardHandicapService = scorecardHandicapService;
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
