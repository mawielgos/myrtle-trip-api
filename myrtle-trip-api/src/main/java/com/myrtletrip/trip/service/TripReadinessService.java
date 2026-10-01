package com.myrtletrip.trip.service;

import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.trip.dto.TripReadinessResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.model.TripHandicapMethod;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class TripReadinessService {

    private static final String GHIN_FROZEN = "GHIN_FROZEN";
    private static final int MIN_PLANNED_ROUND_COUNT = 1;
    private static final TripHandicapMethod DEFAULT_HANDICAP_METHOD =
            TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY;

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final TripPlannedRoundService tripPlannedRoundService;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    private final TripHandicapService tripHandicapService;

    public TripReadinessService(TripRepository tripRepository,
                                TripPlayerRepository tripPlayerRepository,
                                TripPlannedRoundService tripPlannedRoundService,
                                ScoreHistoryEntryRepository scoreHistoryEntryRepository,
                                TripHandicapService tripHandicapService) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.tripPlannedRoundService = tripPlannedRoundService;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
        this.tripHandicapService = tripHandicapService;
    }

    @Transactional(readOnly = true)
    public TripReadinessResponse getTripReadiness(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        return getTripReadiness(trip);
    }

    TripReadinessResponse getTripReadiness(Trip trip) {
        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);
        List<TripPlannedRound> plannedRounds = tripPlannedRoundService.loadActivePlannedRounds(trip);

        int activePlayerCount = 0;
        for (TripPlayer tripPlayer : tripPlayers) {
            if (tripPlayer != null
                    && tripPlayer.getPlayer() != null
                    && tripPlayer.getPlayer().isActive()) {
                activePlayerCount++;
            }
        }

        int completedPlannedRoundCount = 0;
        for (TripPlannedRound plannedRound : plannedRounds) {
            if (isPlannedRoundComplete(plannedRound)) {
                completedPlannedRoundCount++;
            }
        }

        boolean handicapsEnabled =
                trip.getHandicapsEnabled() == null || Boolean.TRUE.equals(trip.getHandicapsEnabled());

        long unresolvedGhinFixCount = 0L;
        if (handicapsEnabled
                && !TripHandicapMethod.FROZEN_GHIN_INDEX.equals(trip.getHandicapMethod())
                && trip.getTripCode() != null
                && !trip.getTripCode().isBlank()) {
            unresolvedGhinFixCount =
                    scoreHistoryEntryRepository
                            .countByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrue(
                                    trip.getTripCode(),
                                    GHIN_FROZEN
                            );
        }

        boolean handicapIndexesReady = true;
        if (handicapsEnabled) {
            for (TripPlayer tripPlayer : tripPlayers) {
                if (tripPlayer == null
                        || tripPlayer.getPlayer() == null
                        || !tripPlayer.getPlayer().isActive()) {
                    continue;
                }

                if (TripHandicapMethod.FROZEN_GHIN_INDEX.equals(trip.getHandicapMethod())) {
                    if (tripPlayer.getFrozenHandicapIndex() == null) {
                        handicapIndexesReady = false;
                    }
                } else {
                    try {
                        BigDecimal calculatedIndex = tripHandicapService.calculateTripIndex(
                                tripPlayer.getPlayer(),
                                trip.getTripCode(),
                                resolveEffectiveHandicapMethod(trip)
                        );
                        if (calculatedIndex == null) {
                            handicapIndexesReady = false;
                        }
                    } catch (Exception ex) {
                        handicapIndexesReady = false;
                    }
                }
            }
        }

        boolean rosterReady = activePlayerCount > 0;
        boolean plannedRoundsReady =
                plannedRounds.size() >= MIN_PLANNED_ROUND_COUNT
                        && completedPlannedRoundCount == plannedRounds.size();
        boolean ghinFixesReady = unresolvedGhinFixCount == 0L;

        boolean alreadyStarted =
                Boolean.TRUE.equals(trip.getInitialized())
                        || TripStatus.IN_PROGRESS.equals(trip.getStatus())
                        || TripStatus.COMPLETE.equals(trip.getStatus());

        boolean canStartTrip =
                !alreadyStarted
                        && rosterReady
                        && plannedRoundsReady
                        && ghinFixesReady
                        && handicapIndexesReady;

        List<String> blockingItems = new ArrayList<String>();

        if (alreadyStarted) {
            blockingItems.add("Trip has already been started.");
        }

        if (!rosterReady) {
            blockingItems.add("Add at least one active player to the trip roster.");
        }

        if (plannedRounds.size() < MIN_PLANNED_ROUND_COUNT) {
            blockingItems.add("Trip must have at least one planned round.");
        } else if (completedPlannedRoundCount != plannedRounds.size()) {
            blockingItems.add(
                    "All planned rounds must have a date, format, course, and standard tee. "
                            + "Alternate tee is optional but must differ from the standard tee."
            );
        }

        if (!handicapIndexesReady) {
            if (TripHandicapMethod.FROZEN_GHIN_INDEX.equals(trip.getHandicapMethod())) {
                blockingItems.add(
                        "Enter a frozen GHIN handicap index for every active player before starting the trip, "
                                + "or mark the trip as Scratch / no handicaps."
                );
            } else {
                blockingItems.add(
                        "Every active player must have a calculable handicap index before starting the trip, "
                                + "or mark the trip as Scratch / no handicaps."
                );
            }
        }

        if (!ghinFixesReady) {
            blockingItems.add("Resolve all GHIN manual differential fixes before starting the trip.");
        }

        TripReadinessResponse response = new TripReadinessResponse();
        response.setActivePlayerCount(activePlayerCount);
        response.setPlannedRoundCount(plannedRounds.size());
        response.setCompletedPlannedRoundCount(completedPlannedRoundCount);
        response.setUnresolvedGhinFixCount(unresolvedGhinFixCount);
        response.setRosterReady(rosterReady);
        response.setPlannedRoundsReady(plannedRoundsReady);
        response.setGhinFixesReady(ghinFixesReady);
        response.setHandicapIndexesReady(handicapIndexesReady);
        response.setCanStartTrip(canStartTrip);
        response.setBlockingItems(blockingItems);

        return response;
    }

    @Transactional(readOnly = true)
    public void validateTripCanStart(Long tripId) {
        TripReadinessResponse readiness = getTripReadiness(tripId);

        if (Boolean.TRUE.equals(readiness.getCanStartTrip())) {
            return;
        }

        List<String> blockingItems = readiness.getBlockingItems();
        if (blockingItems == null || blockingItems.isEmpty()) {
            throw new IllegalStateException("Trip is not ready to start.");
        }

        throw new IllegalStateException(
                "Trip is not ready to start: " + String.join(" ", blockingItems)
        );
    }

    private boolean isPlannedRoundComplete(TripPlannedRound plannedRound) {
        if (plannedRound == null) {
            return false;
        }
        if (plannedRound.getRoundDate() == null) {
            return false;
        }
        if (!tripPlannedRoundService.hasPlannedRoundEventConfiguration(plannedRound)) {
            return false;
        }
        if (plannedRound.getCourseId() == null) {
            return false;
        }
        return plannedRound.getStandardTeeId() != null;
    }

    private TripHandicapMethod resolveEffectiveHandicapMethod(Trip trip) {
        if (trip == null || trip.getHandicapMethod() == null) {
            return DEFAULT_HANDICAP_METHOD;
        }
        return trip.getHandicapMethod();
    }
}
