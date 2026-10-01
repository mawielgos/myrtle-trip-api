package com.myrtletrip.trip.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.model.TripHandicapMethod;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TripSetupService {

    private static final int MIN_PLANNED_ROUND_COUNT = 1;
    private static final int MAX_PLANNED_ROUND_COUNT = 12;
    private static final TripHandicapMethod DEFAULT_HANDICAP_METHOD =
            TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY;

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final PlayerRepository playerRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    private final TripPlannedRoundService tripPlannedRoundService;

    public TripSetupService(TripRepository tripRepository,
                            TripPlayerRepository tripPlayerRepository,
                            PlayerRepository playerRepository,
                            ScoreHistoryEntryRepository scoreHistoryEntryRepository,
                            TripPlannedRoundService tripPlannedRoundService) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.playerRepository = playerRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
        this.tripPlannedRoundService = tripPlannedRoundService;
    }

    @Transactional
    public Trip createOrUpdateTripRoster(TripSetupRequest request) {
        if (request.getTripCode() == null || request.getTripCode().isBlank()) {
            throw new IllegalArgumentException("tripCode is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        List<Long> requestedPlayerIds = request.getPlayerIds() == null
                ? new ArrayList<Long>()
                : new ArrayList<Long>(request.getPlayerIds());

        if (request.getTripYear() == null) {
            throw new IllegalArgumentException("tripYear is required");
        }

        validateUniquePlayerIds(requestedPlayerIds);

        Trip trip;
        if (request.getTripId() != null) {
            trip = tripRepository.findById(request.getTripId())
                    .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + request.getTripId()));
        } else {
            trip = tripRepository.findByTripCode(request.getTripCode()).orElse(null);
            if (trip == null) {
                trip = new Trip();
            }
        }

        boolean isNewTrip = trip.getId() == null;

        if (!isNewTrip) {
            assertTripSetupEditable(trip);
        }

        if (!isNewTrip && !trip.getTripCode().equals(request.getTripCode())) {
            long existingHandicapRows = scoreHistoryEntryRepository.countByHandicapGroupCode(trip.getTripCode());
            if (existingHandicapRows > 0) {
                throw new IllegalStateException(
                        "Cannot change trip_code after handicap data has been initialized."
                );
            }
        }

        TripHandicapMethod resolvedHandicapMethod = resolveTripHandicapMethod(request.getHandicapMethod());
        validateTripHandicapPolicy(request, resolvedHandicapMethod);
        validateTripDatesAndRoundCount(request);

        trip.setTripCode(request.getTripCode());
        trip.setName(request.getName());
        trip.setTripYear(request.getTripYear());
        trip.setEntryFee(request.getEntryFee());
        trip.setTripStartDate(request.getTripStartDate());
        trip.setTripEndDate(request.getTripEndDate());
        trip.setPlannedRoundCount(tripPlannedRoundService.resolvePlannedRoundCount(request.getPlannedRoundCount()));
        trip.setHandicapsEnabled(request.getHandicapsEnabled() == null ? Boolean.TRUE : request.getHandicapsEnabled());
        trip.setHandicapMethod(resolvedHandicapMethod);

        if (isNewTrip && trip.getInitialized() == null) {
            trip.setInitialized(false);
        }

        if (trip.getStatus() == null) {
            trip.setStatus(TripStatus.PLANNING);
        }

        if (!TripStatus.COMPLETE.equals(trip.getStatus()) && !Boolean.TRUE.equals(trip.getInitialized())) {
            trip.setStatus(TripStatus.PLANNING);
        }

        trip = tripRepository.saveAndFlush(trip);

        Map<Long, BigDecimal> existingFrozenIndexes = new HashMap<Long, BigDecimal>();
        List<TripPlayer> existingTripPlayers = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);
        for (TripPlayer existingTripPlayer : existingTripPlayers) {
            if (existingTripPlayer != null && existingTripPlayer.getPlayer() != null) {
                existingFrozenIndexes.put(
                        existingTripPlayer.getPlayer().getId(),
                        existingTripPlayer.getFrozenHandicapIndex()
                );
            }
        }

        tripPlayerRepository.deleteByTrip(trip);
        tripPlayerRepository.flush();

        int displayOrder = 1;
        for (Long playerId : requestedPlayerIds) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

            if (!player.isActive()) {
                throw new IllegalArgumentException(
                        "Only active players can be added to a trip roster: " + player.getDisplayName()
                );
            }

            TripPlayer tripPlayer = new TripPlayer();
            tripPlayer.setTrip(trip);
            tripPlayer.setPlayer(player);
            tripPlayer.setDisplayOrder(displayOrder);
            tripPlayer.setFrozenHandicapIndex(
                    resolveFrozenHandicapIndex(
                            request,
                            resolvedHandicapMethod,
                            playerId,
                            existingFrozenIndexes
                    )
            );
            tripPlayerRepository.save(tripPlayer);
            displayOrder++;
        }

        tripPlayerRepository.flush();

        if (isNewTrip) {
            tripPlannedRoundService.createDefaultPlannedRounds(trip);
        } else {
            tripPlannedRoundService.syncPlannedRoundsToTripRoundCount(trip);
            tripPlannedRoundService.validateExistingPlannedRoundsWithinTripDates(trip);
        }

        return trip;
    }

    private void assertTripSetupEditable(Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("Trip is required.");
        }

        if (TripStatus.IN_PROGRESS.equals(trip.getStatus())
                || TripStatus.COMPLETE.equals(trip.getStatus())
                || Boolean.TRUE.equals(trip.getInitialized())) {
            throw new IllegalStateException("Trip setup cannot be changed after the trip has started.");
        }
    }

    private void validateTripDatesAndRoundCount(TripSetupRequest request) {
        int plannedRoundCount = tripPlannedRoundService.resolvePlannedRoundCount(request.getPlannedRoundCount());
        if (plannedRoundCount < MIN_PLANNED_ROUND_COUNT || plannedRoundCount > MAX_PLANNED_ROUND_COUNT) {
            throw new IllegalArgumentException(
                    "Planned round count must be between "
                            + MIN_PLANNED_ROUND_COUNT
                            + " and "
                            + MAX_PLANNED_ROUND_COUNT
                            + "."
            );
        }

        if (request.getTripStartDate() == null) {
            throw new IllegalArgumentException("Trip start date is required.");
        }
        if (request.getTripEndDate() == null) {
            throw new IllegalArgumentException("Trip end date is required.");
        }
        if (request.getTripEndDate().isBefore(request.getTripStartDate())) {
            throw new IllegalArgumentException("Trip end date cannot be before trip start date.");
        }
    }

    private TripHandicapMethod resolveTripHandicapMethod(String rawMethod) {
        if (rawMethod == null || rawMethod.isBlank()) {
            return DEFAULT_HANDICAP_METHOD;
        }

        String normalized = rawMethod.trim().toUpperCase();

        if ("GHIN".equals(normalized)) {
            return TripHandicapMethod.GHIN_HISTORY;
        }

        if ("DB_SCORE_HISTORY".equals(normalized) || "MYRTLE_BEACH".equals(normalized)) {
            return TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY;
        }

        try {
            return TripHandicapMethod.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported trip handicap method: " + rawMethod);
        }
    }

    private void validateTripHandicapPolicy(TripSetupRequest request, TripHandicapMethod handicapMethod) {
        if (request.getHandicapsEnabled() != null && !Boolean.TRUE.equals(request.getHandicapsEnabled())) {
            return;
        }

        if (!TripHandicapMethod.FROZEN_GHIN_INDEX.equals(handicapMethod)) {
            return;
        }

        if (request.getFrozenHandicapIndexesByPlayerId() == null) {
            throw new IllegalArgumentException("Frozen GHIN Index is required for every selected player.");
        }

        List<Long> requestedPlayerIds = request.getPlayerIds() == null
                ? new ArrayList<Long>()
                : request.getPlayerIds();

        for (Long playerId : requestedPlayerIds) {
            BigDecimal frozenIndex = request.getFrozenHandicapIndexesByPlayerId().get(playerId);
            if (frozenIndex == null) {
                throw new IllegalArgumentException("Frozen GHIN Index is required for every selected player.");
            }
            if (frozenIndex.compareTo(BigDecimal.valueOf(-10.0)) < 0
                    || frozenIndex.compareTo(BigDecimal.valueOf(54.0)) > 0) {
                throw new IllegalArgumentException("Frozen GHIN Index must be between -10.0 and 54.0.");
            }
        }
    }

    private BigDecimal resolveFrozenHandicapIndex(TripSetupRequest request,
                                                   TripHandicapMethod handicapMethod,
                                                   Long playerId,
                                                   Map<Long, BigDecimal> existingFrozenIndexes) {
        if (request.getHandicapsEnabled() != null && !Boolean.TRUE.equals(request.getHandicapsEnabled())) {
            return null;
        }

        if (!TripHandicapMethod.FROZEN_GHIN_INDEX.equals(handicapMethod)) {
            return null;
        }

        if (request.getFrozenHandicapIndexesByPlayerId() != null
                && request.getFrozenHandicapIndexesByPlayerId().containsKey(playerId)) {
            return request.getFrozenHandicapIndexesByPlayerId().get(playerId);
        }

        return existingFrozenIndexes.get(playerId);
    }

    private void validateUniquePlayerIds(List<Long> playerIds) {
        Set<Long> seen = new HashSet<Long>();

        for (Long playerId : playerIds) {
            if (playerId == null) {
                throw new IllegalArgumentException("playerId is required");
            }
            if (!seen.add(playerId)) {
                throw new IllegalArgumentException("Duplicate playerId: " + playerId);
            }
        }
    }
}
