package com.myrtletrip.readiness.service;

import com.myrtletrip.prize.entity.PrizeSchedule;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.readiness.dto.ReadinessIssueResponse;
import com.myrtletrip.readiness.dto.TripWorkflowReadinessResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.tournament.entity.TripTournament;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.trip.dto.TripReadinessResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripRepository;
import com.myrtletrip.trip.service.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripWorkflowReadinessService {

    private static final String ERROR = "ERROR";
    private static final String WARNING = "WARNING";

    private final TripRepository tripRepository;
    private final RoundRepository roundRepository;
    private final PrizeScheduleRepository prizeScheduleRepository;
    private final TripTournamentRepository tripTournamentRepository;
    private final TripService tripService;

    public TripWorkflowReadinessService(
            TripRepository tripRepository,
            RoundRepository roundRepository,
            PrizeScheduleRepository prizeScheduleRepository,
            TripTournamentRepository tripTournamentRepository,
            TripService tripService
    ) {
        this.tripRepository = tripRepository;
        this.roundRepository = roundRepository;
        this.prizeScheduleRepository = prizeScheduleRepository;
        this.tripTournamentRepository = tripTournamentRepository;
        this.tripService = tripService;
    }

    @Transactional(readOnly = true)
    public TripWorkflowReadinessResponse getReadiness(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        TripReadinessResponse startReadiness = tripService.getTripReadiness(tripId);
        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        List<PrizeSchedule> prizeSchedules = prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId);
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);

        int finalizedRoundCount = 0;
        for (Round round : rounds) {
            if (round != null && Boolean.TRUE.equals(round.getFinalized())) {
                finalizedRoundCount++;
            }
        }

        int incompletePrizeScheduleCount = 0;
        for (PrizeSchedule schedule : prizeSchedules) {
            if (schedule == null || schedule.getPayouts() == null || schedule.getPayouts().isEmpty()) {
                incompletePrizeScheduleCount++;
            }
        }

        boolean started = Boolean.TRUE.equals(trip.getInitialized())
                || TripStatus.IN_PROGRESS.equals(trip.getStatus())
                || TripStatus.COMPLETE.equals(trip.getStatus());
        boolean complete = TripStatus.COMPLETE.equals(trip.getStatus());
        boolean inProgress = TripStatus.IN_PROGRESS.equals(trip.getStatus());
        boolean roundsCreatedReady = !rounds.isEmpty();
        boolean allRoundsFinalizedReady = roundsCreatedReady && finalizedRoundCount == rounds.size();
        boolean tournamentReady = calculateTournamentReady(tournament);
        boolean prizeSetupReady = !prizeSchedules.isEmpty() && incompletePrizeScheduleCount == 0;

        TripWorkflowReadinessResponse response = new TripWorkflowReadinessResponse();
        response.setTripId(tripId);
        response.setTripStatus(trip.getStatus() == null ? null : trip.getStatus().name());
        response.setCanStartTrip(Boolean.TRUE.equals(startReadiness.getCanStartTrip()));
        response.setCanCompleteTrip(inProgress && allRoundsFinalizedReady);
        response.setCanEditTournament(!started);
        response.setCanEditPrizeSetup(!complete);
        response.setRosterReady(startReadiness.getRosterReady());
        response.setPlannedRoundsReady(startReadiness.getPlannedRoundsReady());
        response.setHandicapIndexesReady(startReadiness.getHandicapIndexesReady());
        response.setGhinFixesReady(startReadiness.getGhinFixesReady());
        response.setRoundsCreatedReady(roundsCreatedReady);
        response.setAllRoundsFinalizedReady(allRoundsFinalizedReady);
        response.setTournamentReady(tournamentReady);
        response.setPrizeSetupReady(prizeSetupReady);
        response.setActivePlayerCount(startReadiness.getActivePlayerCount());
        response.setPlannedRoundCount(startReadiness.getPlannedRoundCount());
        response.setCompletedPlannedRoundCount(startReadiness.getCompletedPlannedRoundCount());
        response.setRoundCount(rounds.size());
        response.setFinalizedRoundCount(finalizedRoundCount);
        response.setPrizeScheduleCount(prizeSchedules.size());
        response.setIncompletePrizeScheduleCount(incompletePrizeScheduleCount);

        addStartTripIssues(response, startReadiness, tripId);
        addCompletionIssues(response, inProgress, complete, rounds, finalizedRoundCount, tripId);
        addTournamentIssues(response, tournament, tripId);
        addPrizeIssues(response, prizeSchedules, incompletePrizeScheduleCount, tripId);

        return response;
    }

    private void addStartTripIssues(TripWorkflowReadinessResponse response, TripReadinessResponse startReadiness, Long tripId) {
        if (Boolean.TRUE.equals(startReadiness.getCanStartTrip())) {
            return;
        }
        if (!Boolean.TRUE.equals(startReadiness.getRosterReady())) {
            response.getIssues().add(issue("TRIP_ROSTER_INCOMPLETE", ERROR, "START_TRIP", "Add at least one active player before starting the trip.", "Edit Trip", "/trips/" + tripId + "/edit"));
        }
        if (!Boolean.TRUE.equals(startReadiness.getPlannedRoundsReady())) {
            response.getIssues().add(issue("TRIP_ROUNDS_INCOMPLETE", ERROR, "START_TRIP", "Complete planned round setup before starting the trip.", "Round Planning", "/trips/" + tripId + "/round-planning"));
        }
        if (!Boolean.TRUE.equals(startReadiness.getHandicapIndexesReady())) {
            response.getIssues().add(issue("TRIP_HANDICAPS_INCOMPLETE", ERROR, "START_TRIP", "Every active player must have a usable handicap/index or the trip must be marked as scratch.", "Trip Detail", "/trips/" + tripId));
        }
        if (!Boolean.TRUE.equals(startReadiness.getGhinFixesReady())) {
            response.getIssues().add(issue("TRIP_GHIN_FIXES_OPEN", ERROR, "START_TRIP", "Resolve GHIN manual differential fixes before starting the trip.", "GHIN Fixes", "/trips/" + tripId + "/ghin-fixes"));
        }
    }

    private void addCompletionIssues(TripWorkflowReadinessResponse response, boolean inProgress, boolean complete, List<Round> rounds, int finalizedRoundCount, Long tripId) {
        if (complete) {
            response.getIssues().add(issue("TRIP_ALREADY_COMPLETE", WARNING, "LOCK_TRIP", "This trip is already complete/locked."));
            return;
        }
        if (!inProgress) {
            response.getIssues().add(issue("TRIP_NOT_STARTED", WARNING, "LOCK_TRIP", "Trip completion is only available after the trip has started."));
            return;
        }
        if (rounds == null || rounds.isEmpty()) {
            response.getIssues().add(issue("TRIP_NO_ROUNDS_CREATED", ERROR, "LOCK_TRIP", "Trip cannot be completed until started rounds exist."));
            return;
        }
        if (finalizedRoundCount != rounds.size()) {
            response.getIssues().add(issue("TRIP_ROUNDS_NOT_FINALIZED", ERROR, "LOCK_TRIP", "Trip cannot be completed until every round is finalized. Finalized rounds: " + finalizedRoundCount + " of " + rounds.size() + ".", "Trip Detail", "/trips/" + tripId));
        }
    }

    private void addTournamentIssues(TripWorkflowReadinessResponse response, TripTournament tournament, Long tripId) {
        if (tournament == null || !Boolean.TRUE.equals(tournament.getEnabled())) {
            response.getIssues().add(issue("TOURNAMENT_DISABLED", WARNING, "TOURNAMENT", "Multi-round tournament is disabled for this trip."));
            return;
        }
        if (tournament.getRounds() == null || tournament.getRounds().size() < 2) {
            response.getIssues().add(issue("TOURNAMENT_ROUNDS_INCOMPLETE", ERROR, "TOURNAMENT", "Enabled tournament needs at least two included rounds.", "Tournament Setup", "/trips/" + tripId + "/tournament"));
        }
    }

    private void addPrizeIssues(TripWorkflowReadinessResponse response, List<PrizeSchedule> schedules, int incompletePrizeScheduleCount, Long tripId) {
        if (schedules == null || schedules.isEmpty()) {
            response.getIssues().add(issue("PRIZE_SETUP_EMPTY", WARNING, "PRIZES", "No prize schedules have been configured yet.", "Prize Setup", "/trips/" + tripId + "/prizes"));
            return;
        }
        if (incompletePrizeScheduleCount > 0) {
            response.getIssues().add(issue("PRIZE_SCHEDULES_INCOMPLETE", WARNING, "PRIZES", incompletePrizeScheduleCount + " prize schedule(s) have no payouts configured.", "Prize Setup", "/trips/" + tripId + "/prizes"));
        }
    }

    private boolean calculateTournamentReady(TripTournament tournament) {
        if (tournament == null || !Boolean.TRUE.equals(tournament.getEnabled())) {
            return true;
        }
        return tournament.getRounds() != null && tournament.getRounds().size() >= 2;
    }

    private ReadinessIssueResponse issue(String code, String severity, String area, String message) {
        return new ReadinessIssueResponse(code, severity, area, message);
    }

    private ReadinessIssueResponse issue(String code, String severity, String area, String message, String actionLabel, String actionPath) {
        return new ReadinessIssueResponse(code, severity, area, message, actionLabel, actionPath);
    }
}
