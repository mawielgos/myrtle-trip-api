package com.myrtletrip.trip.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.trip.dto.SaveTripPlannedRoundsRequest;
import com.myrtletrip.trip.dto.TripDetailResponse;
import com.myrtletrip.trip.dto.TripListResponse;
import com.myrtletrip.trip.dto.TripPlannedRoundResponse;
import com.myrtletrip.trip.dto.TripPlayerResponse;
import com.myrtletrip.trip.dto.TripReadinessResponse;
import com.myrtletrip.trip.dto.TripRoundListResponse;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.trip.entity.Trip;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripService {

    private final TripParticipationService tripParticipationService;
    private final TripLifecycleService tripLifecycleService;
    private final TripGhinInitializationService tripGhinInitializationService;
    private final TripStatusService tripStatusService;
    private final TripPlannedRoundService tripPlannedRoundService;
    private final TripSetupService tripSetupService;
    private final TripReadinessService tripReadinessService;
    private final TripRoundListService tripRoundListService;
    private final TripReadModelService tripReadModelService;

    public TripService(TripParticipationService tripParticipationService,
                       TripLifecycleService tripLifecycleService,
                       TripGhinInitializationService tripGhinInitializationService,
                       TripStatusService tripStatusService,
                       TripPlannedRoundService tripPlannedRoundService,
                       TripSetupService tripSetupService,
                       TripReadinessService tripReadinessService,
                       TripRoundListService tripRoundListService,
                       TripReadModelService tripReadModelService) {
        this.tripParticipationService = tripParticipationService;
        this.tripLifecycleService = tripLifecycleService;
        this.tripGhinInitializationService = tripGhinInitializationService;
        this.tripStatusService = tripStatusService;
        this.tripPlannedRoundService = tripPlannedRoundService;
        this.tripSetupService = tripSetupService;
        this.tripReadinessService = tripReadinessService;
        this.tripRoundListService = tripRoundListService;
        this.tripReadModelService = tripReadModelService;
    }

    @Transactional
    public Trip createOrUpdateTripRoster(TripSetupRequest request) {
        return tripSetupService.createOrUpdateTripRoster(request);
    }

    @Transactional(readOnly = true)
    public List<TripListResponse> getTrips(boolean includeArchived) {
        return tripReadModelService.getTrips(includeArchived);
    }

    @Transactional(readOnly = true)
    public TripDetailResponse getTrip(Long tripId) {
        return tripReadModelService.getTrip(tripId);
    }

    public List<TripPlayerResponse> getTripPlayers(Long tripId) {
        return tripReadModelService.getTripPlayers(tripId);
    }

    @Transactional
    public List<TripPlayerResponse> updateTripPlayerParticipation(Long tripId, Long playerId, String participationStatusText) {
        tripParticipationService.updateParticipation(tripId, playerId, participationStatusText);
        return tripReadModelService.getTripPlayers(tripId);
    }

    @Transactional
    public void archiveTrip(Long tripId) {
        tripLifecycleService.archiveTrip(tripId);
    }

    @Transactional
    public void restoreTrip(Long tripId) {
        tripLifecycleService.restoreTrip(tripId);
    }

    @Transactional
    public void deleteTrip(Long tripId) {
        tripLifecycleService.deleteTrip(tripId);
    }

    @Transactional
    public void initializeTripGhin(Long tripId) throws Exception {
        tripGhinInitializationService.initializeTripGhin(tripId);
    }

    @Transactional(readOnly = true)
    public List<TripPlannedRoundResponse> getPlannedRounds(Long tripId) {
        return tripPlannedRoundService.getPlannedRounds(tripId);
    }

    @Transactional
    public List<TripPlannedRoundResponse> savePlannedRounds(Long tripId, SaveTripPlannedRoundsRequest request) {
        return tripPlannedRoundService.savePlannedRounds(tripId, request);
    }

    @Transactional(readOnly = true)
    public Round findCurrentRoundEntity(Long tripId) {
        return tripStatusService.findCurrentRoundEntity(tripId);
    }

    @Transactional
    public void refreshTripStatusFromRounds(Long tripId) {
        tripStatusService.refreshTripStatusFromRounds(tripId);
    }

    @Transactional
    public List<TripRoundListResponse> getTripRounds(Long tripId) {
        return tripRoundListService.getTripRounds(tripId);
    }

    @Transactional(readOnly = true)
    public TripReadinessResponse getTripReadiness(Long tripId) {
        return tripReadinessService.getTripReadiness(tripId);
    }

    @Transactional(readOnly = true)
    public void validateTripCanStart(Long tripId) {
        tripReadinessService.validateTripCanStart(tripId);
    }
}
