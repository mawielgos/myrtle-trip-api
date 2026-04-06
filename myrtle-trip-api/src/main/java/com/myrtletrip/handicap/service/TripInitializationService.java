package com.myrtletrip.handicap.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripInitializationService {

    private static final String HANDICAP_METHOD_GHIN = "GHIN";
    private static final String HANDICAP_METHOD_MYRTLE_BEACH = "MYRTLE_BEACH";

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final FrozenGhinImportService frozenGhinImportService;
    private final FrozenMyrtleBeachImportService frozenMyrtleBeachImportService;

    public TripInitializationService(TripRepository tripRepository,
                                     TripPlayerRepository tripPlayerRepository,
                                     FrozenGhinImportService frozenGhinImportService,
                                     FrozenMyrtleBeachImportService frozenMyrtleBeachImportService) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.frozenGhinImportService = frozenGhinImportService;
        this.frozenMyrtleBeachImportService = frozenMyrtleBeachImportService;
    }

    @Transactional
    public void initializeTrip(Long tripId) throws Exception {
        if (tripId == null) {
            throw new IllegalArgumentException("tripId is required");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTrip(trip);
        if (tripPlayers == null || tripPlayers.isEmpty()) {
            throw new IllegalStateException("Trip has no players: " + tripId);
        }

        String handicapGroupCode = trip.getTripCode();
        if (handicapGroupCode == null || handicapGroupCode.isBlank()) {
            throw new IllegalStateException("Trip code is required before initialization");
        }

        for (TripPlayer tripPlayer : tripPlayers) {
            Player player = tripPlayer.getPlayer();
            String handicapMethod = normalize(player.getHandicapMethod());

            if (HANDICAP_METHOD_GHIN.equals(handicapMethod)) {
                frozenGhinImportService.initializeFrozenGhinForPlayer(player, handicapGroupCode);
            } else if (HANDICAP_METHOD_MYRTLE_BEACH.equals(handicapMethod)) {
                frozenMyrtleBeachImportService.initializeFrozenMyrtleBeachForPlayer(player, handicapGroupCode);
            } else {
                throw new IllegalStateException("Unsupported handicap method for player "
                        + player.getId() + ": " + player.getHandicapMethod());
            }
        }

        trip.setInitialized(true);
        tripRepository.save(trip);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}