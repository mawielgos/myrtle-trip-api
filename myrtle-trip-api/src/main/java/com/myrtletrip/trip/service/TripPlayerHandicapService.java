package com.myrtletrip.trip.service;

import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.service.RoundRecalculationOrchestrationService;
import com.myrtletrip.round.service.ScorecardHandicapService;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TripPlayerHandicapService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final RoundRepository roundRepository;
    private final ScorecardHandicapService scorecardHandicapService;
    private final RoundRecalculationOrchestrationService roundRecalculationOrchestrationService;

    public TripPlayerHandicapService(
            TripRepository tripRepository,
            TripPlayerRepository tripPlayerRepository,
            RoundRepository roundRepository,
            ScorecardHandicapService scorecardHandicapService,
            RoundRecalculationOrchestrationService roundRecalculationOrchestrationService
    ) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.roundRepository = roundRepository;
        this.scorecardHandicapService = scorecardHandicapService;
        this.roundRecalculationOrchestrationService = roundRecalculationOrchestrationService;
    }

    @Transactional
    public void saveFrozenHandicapIndex(Long tripId, Long playerId, BigDecimal frozenHandicapIndex) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        validateHandicapIndexCorrectionAllowed(trip);
        validateFrozenHandicapIndex(frozenHandicapIndex);

        TripPlayer tripPlayer = tripPlayerRepository.findByTrip_IdAndPlayer_Id(tripId, playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player is not on this trip/event roster."));

        tripPlayer.setFrozenHandicapIndex(frozenHandicapIndex);
        tripPlayerRepository.save(tripPlayer);

        refreshOpenRoundHandicaps(tripId);
    }

    private void validateHandicapIndexCorrectionAllowed(Trip trip) {
        if (trip == null || trip.getId() == null) {
            throw new IllegalArgumentException("Trip is required");
        }

        long finalizedRounds = roundRepository.countByTrip_IdAndFinalizedTrue(trip.getId());
        if (finalizedRounds > 0) {
            throw new IllegalStateException("Frozen handicap indexes cannot be changed after a round has been finalized. Use correction mode for finalized-round changes.");
        }
    }

    private void refreshOpenRoundHandicaps(Long tripId) {
        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        for (Round round : rounds) {
            if (round == null || round.getId() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(round.getFinalized())) {
                continue;
            }

            scorecardHandicapService.refreshRoundHandicaps(round.getId());
            roundRecalculationOrchestrationService.handlePostRoundChange(round.getId());
        }
    }

    private void validateFrozenHandicapIndex(BigDecimal frozenHandicapIndex) {
        if (frozenHandicapIndex == null) {
            return;
        }

        if (frozenHandicapIndex.compareTo(new BigDecimal("-10.0")) < 0 ||
                frozenHandicapIndex.compareTo(new BigDecimal("54.0")) > 0) {
            throw new IllegalArgumentException("Frozen handicap index must be between -10.0 and 54.0.");
        }
    }
}
