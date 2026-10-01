package com.myrtletrip.trip.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripRepository;

@Service
public class TripStatusService {

    private final TripRepository tripRepository;
    private final RoundRepository roundRepository;

    public TripStatusService(TripRepository tripRepository,
                             RoundRepository roundRepository) {
        this.tripRepository = tripRepository;
        this.roundRepository = roundRepository;
    }

    @Transactional(readOnly = true)
    public Round findCurrentRoundEntity(Long tripId) {
        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);

        for (Round round : rounds) {
            if (!Boolean.TRUE.equals(round.getFinalized())) {
                return round;
            }
        }

        return null;
    }

    @Transactional
    public void refreshTripStatusFromRounds(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (TripStatus.COMPLETE.equals(trip.getStatus())) {
            // Explicit trip completion is sticky. Corrections should not reopen the trip automatically.
        } else if (Boolean.TRUE.equals(trip.getInitialized())) {
            trip.setStatus(TripStatus.IN_PROGRESS);
        }

        tripRepository.save(trip);
    }
}
