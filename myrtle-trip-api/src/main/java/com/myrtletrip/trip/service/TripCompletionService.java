package com.myrtletrip.trip.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripCompletionService {

    private final TripRepository tripRepository;
    private final RoundRepository roundRepository;

    public TripCompletionService(TripRepository tripRepository,
                                 RoundRepository roundRepository) {
        this.tripRepository = tripRepository;
        this.roundRepository = roundRepository;
    }

    @Transactional
    public void completeTrip(Long tripId) {
        Trip trip = loadTrip(tripId);

        if (!TripStatus.IN_PROGRESS.equals(trip.getStatus()) && !TripStatus.COMPLETE.equals(trip.getStatus())) {
            throw new IllegalStateException("Only an in-progress trip can be completed.");
        }

        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        if (rounds.isEmpty()) {
            throw new IllegalStateException("Trip cannot be completed until rounds have been created.");
        }

        for (Round round : rounds) {
            if (!Boolean.TRUE.equals(round.getFinalized())) {
                throw new IllegalStateException("Trip cannot be completed until all rounds are finalized.");
            }
        }

        trip.setStatus(TripStatus.COMPLETE);
        trip.setCorrectionMode(false);
        tripRepository.save(trip);
    }

    @Transactional
    public void enableCorrectionMode(Long tripId) {
        Trip trip = loadTrip(tripId);

        if (!TripStatus.COMPLETE.equals(trip.getStatus())) {
            throw new IllegalStateException("Correction mode can only be enabled after the trip is complete.");
        }

        trip.setCorrectionMode(true);
        tripRepository.save(trip);
    }

    @Transactional
    public void disableCorrectionMode(Long tripId) {
        Trip trip = loadTrip(tripId);
        trip.setCorrectionMode(false);
        tripRepository.save(trip);
    }

    private Trip loadTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
    }
}
