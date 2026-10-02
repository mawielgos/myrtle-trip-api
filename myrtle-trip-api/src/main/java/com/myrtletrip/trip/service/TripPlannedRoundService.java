package com.myrtletrip.trip.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.myrtletrip.trip.dto.SaveTripPlannedRoundsRequest;
import com.myrtletrip.trip.dto.TripPlannedRoundResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;

@Service
public class TripPlannedRoundService {

    private final TripPlannedRoundReadModelService plannedRoundReadModelService;
    private final TripPlannedRoundLifecycleService plannedRoundLifecycleService;
    private final TripPlannedRoundCommandService plannedRoundCommandService;

    public TripPlannedRoundService(TripPlannedRoundReadModelService plannedRoundReadModelService,
                                   TripPlannedRoundLifecycleService plannedRoundLifecycleService,
                                   TripPlannedRoundCommandService plannedRoundCommandService) {
        this.plannedRoundReadModelService = plannedRoundReadModelService;
        this.plannedRoundLifecycleService = plannedRoundLifecycleService;
        this.plannedRoundCommandService = plannedRoundCommandService;
    }

    public List<TripPlannedRoundResponse> getPlannedRounds(Long tripId) {
        return plannedRoundReadModelService.getPlannedRounds(tripId);
    }

    public List<TripPlannedRoundResponse> savePlannedRounds(Long tripId, SaveTripPlannedRoundsRequest request) {
        plannedRoundCommandService.savePlannedRounds(tripId, request);
        return plannedRoundReadModelService.getPlannedRounds(tripId);
    }

    public int resolvePlannedRoundCount(Integer plannedRoundCount) {
        return plannedRoundLifecycleService.resolvePlannedRoundCount(plannedRoundCount);
    }

    public void validateExistingPlannedRoundsWithinTripDates(Trip trip) {
        plannedRoundCommandService.validateExistingPlannedRoundsWithinTripDates(trip);
    }

    public List<TripPlannedRound> findAllPlannedRounds(Trip trip) {
        return plannedRoundReadModelService.findAllPlannedRounds(trip);
    }

    public List<TripPlannedRound> loadActivePlannedRounds(Trip trip) {
        return plannedRoundReadModelService.loadActivePlannedRounds(trip);
    }

    public void syncPlannedRoundsToTripRoundCount(Trip trip) {
        plannedRoundLifecycleService.syncPlannedRoundsToTripRoundCount(trip);
    }

    public void createDefaultPlannedRounds(Trip trip) {
        plannedRoundLifecycleService.createDefaultPlannedRounds(trip);
    }

    public boolean hasPlannedRoundEventConfiguration(TripPlannedRound plannedRound) {
        return plannedRoundReadModelService.hasPlannedRoundEventConfiguration(plannedRound);
    }
}
