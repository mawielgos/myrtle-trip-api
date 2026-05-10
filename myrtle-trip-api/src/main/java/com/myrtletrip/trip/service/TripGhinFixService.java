package com.myrtletrip.trip.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.dto.GhinFixRowResponse;
import com.myrtletrip.trip.dto.SaveGhinFixRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class TripGhinFixService {

    private static final String SOURCE_GHIN_FROZEN = "GHIN_FROZEN";

    private final TripRepository tripRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;

    public TripGhinFixService(TripRepository tripRepository,
                              ScoreHistoryEntryRepository scoreHistoryEntryRepository) {
        this.tripRepository = tripRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<GhinFixRowResponse> getOutstandingFixes(Long tripId) {
        Trip trip = getTripWithCode(tripId);

        List<ScoreHistoryEntry> entries =
                scoreHistoryEntryRepository
                        .findByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrueOrderByPlayer_DisplayNameAscPostingOrderAscIdAsc(
                                trip.getTripCode(),
                                SOURCE_GHIN_FROZEN
                        );

        List<GhinFixRowResponse> responses = new ArrayList<>();

        for (ScoreHistoryEntry entry : entries) {
            responses.add(mapRow(entry));
        }

        return responses;
    }

    @Transactional
    public GhinFixRowResponse saveFix(Long tripId,
                                      Long scoreHistoryEntryId,
                                      SaveGhinFixRequest request) {
        Trip trip = getTripWithCode(tripId);

        assertTripSetupEditable(trip);

        if (request == null || request.getDifferential() == null) {
            throw new IllegalArgumentException("Differential is required.");
        }

        BigDecimal normalizedDifferential =
                request.getDifferential().setScale(3, RoundingMode.HALF_UP);

        ScoreHistoryEntry entry =
                scoreHistoryEntryRepository
                        .findByIdAndHandicapGroupCodeAndSourceType(
                                scoreHistoryEntryId,
                                trip.getTripCode(),
                                SOURCE_GHIN_FROZEN
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException("GHIN fix row not found: " + scoreHistoryEntryId)
                        );

        entry.setDifferential(normalizedDifferential);
        entry.setManualDifferentialRequired(false);

        ScoreHistoryEntry saved = scoreHistoryEntryRepository.save(entry);
        return mapRow(saved);
    }

    private void assertTripSetupEditable(Trip trip) {
        if (TripStatus.IN_PROGRESS.equals(trip.getStatus())
                || TripStatus.COMPLETE.equals(trip.getStatus())
                || Boolean.TRUE.equals(trip.getInitialized())) {
            throw new IllegalStateException("GHIN fixes cannot be changed after the trip has started.");
        }
    }

    private Trip getTripWithCode(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (trip.getTripCode() == null || trip.getTripCode().isBlank()) {
            throw new IllegalArgumentException("Trip code is required for GHIN fixes.");
        }

        return trip;
    }

    private GhinFixRowResponse mapRow(ScoreHistoryEntry entry) {
        Player player = entry.getPlayer();

        GhinFixRowResponse response = new GhinFixRowResponse();
        response.setScoreHistoryEntryId(entry.getId());
        response.setPlayerId(player != null ? player.getId() : null);
        response.setPlayerName(buildPlayerName(player));
        response.setGhinNumber(player != null ? player.getGhinNumber() : null);
        response.setPostingOrder(entry.getPostingOrder());
        response.setScoreType(entry.getScoreType());
        response.setHolesPlayed(entry.getHolesPlayed());
        response.setGrossScore(entry.getGrossScore());
        response.setCourseRating(entry.getCourseRating());
        response.setSlope(entry.getSlope());
        response.setDifferential(entry.getDifferential());
        response.setManualDifferentialRequired(entry.getManualDifferentialRequired());
        return response;
    }

    private String buildPlayerName(Player player) {
        if (player == null) {
            return "Unknown Player";
        }

        if (player.getDisplayName() != null && !player.getDisplayName().isBlank()) {
            return player.getDisplayName().trim();
        }

        String firstName = player.getFirstName() == null ? "" : player.getFirstName().trim();
        String lastName = player.getLastName() == null ? "" : player.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? "Unknown Player" : fullName;
    }
}
