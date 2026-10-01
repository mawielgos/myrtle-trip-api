package com.myrtletrip.trip.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myrtletrip.handicap.source.frozen.FrozenGhinImportService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@Service
public class TripGhinInitializationService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final FrozenGhinImportService frozenGhinImportService;

    public TripGhinInitializationService(TripRepository tripRepository,
                                         TripPlayerRepository tripPlayerRepository,
                                         FrozenGhinImportService frozenGhinImportService) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.frozenGhinImportService = frozenGhinImportService;
    }

    @Transactional
    public void initializeTripGhin(Long tripId) throws Exception {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (TripStatus.IN_PROGRESS.equals(trip.getStatus())
                || TripStatus.COMPLETE.equals(trip.getStatus())
                || Boolean.TRUE.equals(trip.getInitialized())) {
            throw new IllegalStateException("GHIN baseline cannot be loaded after the trip has started.");
        }

        if (trip.getTripCode() == null || trip.getTripCode().isBlank()) {
            throw new IllegalArgumentException("Trip code is required before loading GHIN baseline.");
        }

        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);
        if (tripPlayers.isEmpty()) {
            throw new IllegalArgumentException("Trip must have players before loading GHIN baseline.");
        }

        List<Player> ghinPlayers = new ArrayList<>();

        for (TripPlayer tripPlayer : tripPlayers) {
            if (tripPlayer == null || tripPlayer.getPlayer() == null) {
                continue;
            }

            Player player = tripPlayer.getPlayer();
            if (!player.isActive()) {
                continue;
            }

            boolean hasGhinMethod = "GHIN".equalsIgnoreCase(player.getHandicapMethod());
            boolean hasGhinNumber = player.getGhinNumber() != null && !player.getGhinNumber().isBlank();

            if (!hasGhinMethod && !hasGhinNumber) {
                continue;
            }

            if (hasGhinNumber && !hasGhinMethod) {
                player.setHandicapMethod("GHIN");
            }

            ghinPlayers.add(player);
        }

        if (ghinPlayers.isEmpty()) {
            throw new IllegalArgumentException("No active GHIN players were found on this trip.");
        }

        frozenGhinImportService.initializeFrozenGhinForPlayers(ghinPlayers, trip.getTripCode());
    }
}
