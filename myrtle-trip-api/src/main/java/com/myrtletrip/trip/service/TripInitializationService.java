package com.myrtletrip.trip.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripInitializationService {

    private final TripStartInitializationService tripStartInitializationService;
    private final TripStartResetService tripStartResetService;

    public TripInitializationService(
            TripStartInitializationService tripStartInitializationService,
            TripStartResetService tripStartResetService
    ) {
        this.tripStartInitializationService = tripStartInitializationService;
        this.tripStartResetService = tripStartResetService;
    }

    @Transactional
    public void initializeTrip(Long tripId) throws Exception {
        tripStartInitializationService.initializeTrip(tripId);
    }

    @Transactional
    public void resetTripStart(Long tripId) {
        tripStartResetService.resetTripStart(tripId);
    }
}
