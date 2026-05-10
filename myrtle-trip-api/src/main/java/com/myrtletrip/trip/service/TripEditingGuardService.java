package com.myrtletrip.trip.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripEditingGuardService {

    private final TripRepository tripRepository;

    public TripEditingGuardService(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public void assertCorrectionAllowedForRound(Round round) {
        if (round == null || round.getTrip() == null) {
            throw new IllegalArgumentException("Round and trip are required.");
        }

        Trip trip = resolveCurrentTrip(round.getTrip());
        assertCorrectionAllowed(trip);
    }

    @Transactional(readOnly = true)
    public void assertCorrectionAllowed(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        assertCorrectionAllowed(trip);
    }

    public void assertCorrectionAllowed(Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("Trip is required.");
        }

        Trip currentTrip = resolveCurrentTrip(trip);
        if (TripStatus.COMPLETE.equals(currentTrip.getStatus()) && !Boolean.TRUE.equals(currentTrip.getCorrectionMode())) {
            throw new IllegalStateException("Trip is complete. Enable correction mode before changing scores or tees.");
        }
    }

    public void assertStructureEditable(Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("Trip is required.");
        }

        Trip currentTrip = resolveCurrentTrip(trip);
        if (TripStatus.COMPLETE.equals(currentTrip.getStatus())) {
            throw new IllegalStateException("Trip is complete. Teams, groups, round setup, and prize setup are locked.");
        }
    }

    private Trip resolveCurrentTrip(Trip trip) {
        if (trip == null || trip.getId() == null) {
            return trip;
        }

        return tripRepository.findById(trip.getId()).orElse(trip);
    }
}
