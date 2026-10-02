package com.myrtletrip.strokes.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.round.service.ScorecardHandicapService;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.strokes.dto.StrokesPerDayTeePlanItemRequest;
import com.myrtletrip.strokes.dto.StrokesPerDayTeePlanSaveRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StrokesPerDayTeePlanCommandService {

    private final TripRepository tripRepository;
    private final RoundRepository roundRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final ScorecardRepository scorecardRepository;
    private final ScorecardHandicapService scorecardHandicapService;

    public StrokesPerDayTeePlanCommandService(TripRepository tripRepository,
                                              RoundRepository roundRepository,
                                              RoundTeeRepository roundTeeRepository,
                                              ScorecardRepository scorecardRepository,
                                              ScorecardHandicapService scorecardHandicapService) {
        this.tripRepository = tripRepository;
        this.roundRepository = roundRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.scorecardRepository = scorecardRepository;
        this.scorecardHandicapService = scorecardHandicapService;
    }

    @Transactional
    public void saveTeePlan(Long tripId, StrokesPerDayTeePlanSaveRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        if (request == null || request.getChanges() == null || request.getChanges().isEmpty()) {
            return;
        }

        for (StrokesPerDayTeePlanItemRequest change : request.getChanges()) {
            applyTeePlanChange(trip.getId(), change);
        }
    }

    private void applyTeePlanChange(Long tripId, StrokesPerDayTeePlanItemRequest change) {
        if (change == null) throw new IllegalArgumentException("Tee plan change is required");
        if (change.getPlayerId() == null) throw new IllegalArgumentException("playerId is required");
        if (change.getRoundId() == null) throw new IllegalArgumentException("roundId is required");
        if (change.getRoundTeeId() == null) throw new IllegalArgumentException("roundTeeId is required");

        Round round = roundRepository.findById(change.getRoundId())
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + change.getRoundId()));

        if (round.getTrip() == null || round.getTrip().getId() == null || !round.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("Round " + change.getRoundId() + " does not belong to trip " + tripId);
        }

        String statusCode = resolveRoundStatusCode(round);
        if (!"PLANNING".equals(statusCode)) {
            throw new IllegalStateException("Tee planning is locked for round " + round.getId()
                    + " because it is " + resolveRoundStatusLabel(statusCode));
        }

        RoundTee roundTee = roundTeeRepository.findById(change.getRoundTeeId())
                .orElseThrow(() -> new IllegalArgumentException("Round tee not found: " + change.getRoundTeeId()));
        if (roundTee.getRound() == null || roundTee.getRound().getId() == null
                || !roundTee.getRound().getId().equals(round.getId())) {
            throw new IllegalArgumentException("Round tee " + change.getRoundTeeId() + " does not belong to round " + round.getId());
        }

        Scorecard scorecard = scorecardRepository.findByRound_IdAndPlayer_Id(round.getId(), change.getPlayerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Scorecard not found for round " + round.getId() + " and player " + change.getPlayerId()
                ));

        scorecardHandicapService.setScorecardTee(scorecard.getId(), roundTee.getId());
    }

    private String resolveRoundStatusCode(Round round) {
        if (round != null && Boolean.TRUE.equals(round.getFinalized())) return "FINALIZED";
        if (round != null && round.getId() != null) {
            List<Scorecard> scorecards = scorecardRepository.findByRound_Id(round.getId());
            for (Scorecard scorecard : scorecards) {
                if (scorecard.getThruHole() != null && scorecard.getThruHole() > 0) return "IN_PROGRESS";
                if (scorecard.getGrossScore() != null || scorecard.getNetScore() != null) return "IN_PROGRESS";
            }
        }
        return "PLANNING";
    }

    private String resolveRoundStatusLabel(String statusCode) {
        if ("FINALIZED".equals(statusCode)) return "Finalized";
        if ("IN_PROGRESS".equals(statusCode)) return "In Progress";
        return "Planning";
    }
}
