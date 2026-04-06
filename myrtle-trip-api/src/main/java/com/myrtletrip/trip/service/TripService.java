package com.myrtletrip.trip.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.trip.dto.TripDetailResponse;
import com.myrtletrip.trip.dto.TripListResponse;
import com.myrtletrip.trip.dto.TripPlayerResponse;
import com.myrtletrip.trip.dto.TripRoundListResponse;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final PlayerRepository playerRepository;
    private final RoundRepository roundRepository;

    public TripService(TripRepository tripRepository,
                       TripPlayerRepository tripPlayerRepository,
                       PlayerRepository playerRepository,
                       RoundRepository roundRepository) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.playerRepository = playerRepository;
        this.roundRepository = roundRepository;
    }

    @Transactional
    public Trip createOrUpdateTripRoster(TripSetupRequest request) {
        if (request.getTripCode() == null || request.getTripCode().isBlank()) {
            throw new IllegalArgumentException("tripCode is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.getPlayerIds() == null || request.getPlayerIds().isEmpty()) {
            throw new IllegalArgumentException("playerIds are required");
        }
        if (request.getTripYear() == null) {
            throw new IllegalArgumentException("tripYear is required");
        }

        Trip trip = tripRepository.findByTripCode(request.getTripCode())
                .orElseGet(Trip::new);

        trip.setTripCode(request.getTripCode());
        trip.setName(request.getName());
        trip.setTripYear(request.getTripYear());

        trip = tripRepository.save(trip);

        tripPlayerRepository.deleteByTrip(trip);

        int displayOrder = 1;

        for (Long playerId : request.getPlayerIds()) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

            TripPlayer tripPlayer = new TripPlayer();
            tripPlayer.setTrip(trip);
            tripPlayer.setPlayer(player);
            tripPlayer.setDisplayOrder(displayOrder++);

            tripPlayerRepository.save(tripPlayer);
        }

        return trip;
    }

    @Transactional(readOnly = true)
    public List<TripListResponse> getTrips() {
        return tripRepository.findAll().stream()
                .map(trip -> {
                    TripListResponse response = new TripListResponse();
                    response.setTripId(trip.getId());
                    response.setTripName(trip.getName());
                    response.setTripCode(trip.getTripCode());
                    response.setTripYear(trip.getTripYear());
                    response.setPlayerCount(tripPlayerRepository.countByTrip(trip));
                    response.setRoundCount(roundRepository.countByTrip_Id(trip.getId()));
                    return response;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public TripDetailResponse getTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        TripDetailResponse response = new TripDetailResponse();
        response.setTripId(trip.getId());
        response.setTripName(trip.getName());
        response.setTripCode(trip.getTripCode());
        response.setTripYear(trip.getTripYear());
        return response;
    }

    @Transactional(readOnly = true)
    public List<TripPlayerResponse> getTripPlayers(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        return tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip).stream()
                .map(tripPlayer -> {
                    TripPlayerResponse response = new TripPlayerResponse();
                    response.setPlayerId(tripPlayer.getPlayer().getId());
                    response.setDisplayName(tripPlayer.getPlayer().getDisplayName());
                    response.setHandicapIndex(null);
                    response.setActive(tripPlayer.getPlayer().isActive());
                    return response;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TripRoundListResponse> getTripRounds(Long tripId) {
        return roundRepository.findByTrip_IdOrderByRoundDateAsc(tripId).stream()
                .map(round -> {
                    TripRoundListResponse response = new TripRoundListResponse();
                    response.setRoundId(round.getId());
                    response.setRoundNumber(null);
                    response.setRoundDate(round.getRoundDate());
                    response.setCourseName(round.getCourse() != null ? round.getCourse().getName() : null);
                    response.setTeeName(round.getCourseTee() != null ? round.getCourseTee().getTeeName() : null);
                    response.setGameFormat(round.getFormat() != null ? round.getFormat().name() : null);
                    response.setFinalized(Boolean.TRUE.equals(round.getFinalized()));
                    return response;
                })
                .toList();
    }
}