package com.myrtletrip.trip.service;

import com.myrtletrip.event.dto.RoundEventResponse;
import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.event.repository.RoundEventRepository;
import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.service.RoundTeamAutoAssignmentService;
import com.myrtletrip.round.service.RoundEventCapabilityService;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.dto.CurrentRoundResponse;
import com.myrtletrip.trip.dto.SaveTripPlannedRoundsRequest;
import com.myrtletrip.trip.dto.TripDetailResponse;
import com.myrtletrip.trip.dto.TripListResponse;
import com.myrtletrip.trip.dto.TripPlannedRoundResponse;
import com.myrtletrip.trip.dto.TripPlayerResponse;
import com.myrtletrip.trip.dto.TripReadinessResponse;
import com.myrtletrip.trip.dto.TripRoundListResponse;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.model.TripHandicapMethod;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TripService {

    private static final String GHIN_FROZEN = "GHIN_FROZEN";
    private static final String DB_HISTORY_FROZEN = "DB_HISTORY_FROZEN";
    private static final String TRIP_ROUND = "TRIP_ROUND";
    private static final int DEFAULT_PLANNED_ROUND_COUNT = 5;
    private static final int MIN_PLANNED_ROUND_COUNT = 1;
    private static final int MAX_PLANNED_ROUND_COUNT = 12;
    private static final TripHandicapMethod DEFAULT_HANDICAP_METHOD = TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY;

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamAutoAssignmentService roundTeamAutoAssignmentService;
    private final RoundEventRepository roundEventRepository;
    private final TripHandicapService tripHandicapService;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    private final RoundEventCapabilityService roundEventCapabilityService;
    private final TripParticipationService tripParticipationService;
    private final TripLifecycleService tripLifecycleService;
    private final TripGhinInitializationService tripGhinInitializationService;
    private final TripStatusService tripStatusService;
    private final TripPlannedRoundService tripPlannedRoundService;
    private final TripSetupService tripSetupService;

    public TripService(TripRepository tripRepository,
                       TripPlayerRepository tripPlayerRepository,
                       RoundRepository roundRepository,
                       ScorecardRepository scorecardRepository,
                       RoundGroupRepository roundGroupRepository,
                       RoundTeamRepository roundTeamRepository,
                       RoundTeamAutoAssignmentService roundTeamAutoAssignmentService,
                       RoundEventRepository roundEventRepository,
                       TripHandicapService tripHandicapService,
                       ScoreHistoryEntryRepository scoreHistoryEntryRepository,
                       RoundEventCapabilityService roundEventCapabilityService,
                       TripParticipationService tripParticipationService,
                       TripLifecycleService tripLifecycleService,
                       TripGhinInitializationService tripGhinInitializationService,
                       TripStatusService tripStatusService,
                       TripPlannedRoundService tripPlannedRoundService,
                       TripSetupService tripSetupService) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamAutoAssignmentService = roundTeamAutoAssignmentService;
        this.roundEventRepository = roundEventRepository;
        this.tripHandicapService = tripHandicapService;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
        this.roundEventCapabilityService = roundEventCapabilityService;
        this.tripParticipationService = tripParticipationService;
        this.tripLifecycleService = tripLifecycleService;
        this.tripGhinInitializationService = tripGhinInitializationService;
        this.tripStatusService = tripStatusService;
        this.tripPlannedRoundService = tripPlannedRoundService;
        this.tripSetupService = tripSetupService;
    }

    @Transactional
    public Trip createOrUpdateTripRoster(TripSetupRequest request) {
        return tripSetupService.createOrUpdateTripRoster(request);
    }

    @Transactional(readOnly = true)
    public List<TripListResponse> getTrips(boolean includeArchived) {
        List<Trip> trips = includeArchived ? tripRepository.findAll() : tripRepository.findByArchivedFalseOrArchivedIsNull();
        List<TripListResponse> responses = new ArrayList<TripListResponse>();

        for (Trip trip : trips) {
            TripListResponse response = new TripListResponse();
            response.setTripId(trip.getId());
            response.setTripName(trip.getName());
            response.setTripCode(trip.getTripCode());
            response.setTripYear(trip.getTripYear());
            response.setStatus(trip.getStatus() != null ? trip.getStatus().name() : null);
            response.setCorrectionMode(Boolean.TRUE.equals(trip.getCorrectionMode()));
            response.setArchived(Boolean.TRUE.equals(trip.getArchived()));

            long playerCount = tripPlayerRepository.countByTrip(trip);
            long roundCount = roundRepository.countByTrip_Id(trip.getId());
            response.setPlayerCount(playerCount);
            response.setRoundCount(roundCount);
            response.setPlannedRoundCount(tripPlannedRoundService.resolvePlannedRoundCount(trip.getPlannedRoundCount()));
            response.setCanDelete(roundCount == 0L);
            response.setCanArchive(!Boolean.TRUE.equals(trip.getArchived()));
            response.setCanRestore(Boolean.TRUE.equals(trip.getArchived()));

            TripDateRange tripDateRange = resolveTripDateRange(trip);
            response.setStartDate(tripDateRange.getStartDate());
            response.setEndDate(tripDateRange.getEndDate());

            responses.add(response);
        }

        return responses;
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
        response.setEntryFee(trip.getEntryFee());
        response.setTripStartDate(trip.getTripStartDate());
        response.setTripEndDate(trip.getTripEndDate());
        response.setPlannedRoundCount(tripPlannedRoundService.resolvePlannedRoundCount(trip.getPlannedRoundCount()));
        response.setHandicapsEnabled(trip.getHandicapsEnabled() == null ? Boolean.TRUE : trip.getHandicapsEnabled());
        response.setHandicapMethod(trip.getHandicapMethod() != null ? trip.getHandicapMethod().name() : DEFAULT_HANDICAP_METHOD.name());
        response.setInitialized(trip.getInitialized());
        response.setStatus(trip.getStatus() != null ? trip.getStatus().name() : null);
        response.setCorrectionMode(Boolean.TRUE.equals(trip.getCorrectionMode()));
        response.setArchived(Boolean.TRUE.equals(trip.getArchived()));
        response.setHasFemalePlayers(hasFemalePlayers(trip));

        TripReadinessResponse readiness = buildTripReadiness(trip);
        response.setUnresolvedGhinFixCount(readiness.getUnresolvedGhinFixCount());
        response.setReadiness(readiness);

        Round currentRound = findCurrentRoundEntity(tripId);
        response.setCurrentRound(toCurrentRoundResponse(currentRound));
        return response;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<TripPlayerResponse> getTripPlayers(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);
        List<TripPlayerResponse> responses = new ArrayList<TripPlayerResponse>();

        for (TripPlayer tripPlayer : tripPlayers) {
            Player player = tripPlayer.getPlayer();

            TripPlayerResponse response = new TripPlayerResponse();
            response.setPlayerId(player.getId());
            response.setDisplayName(player.getDisplayName());
            response.setGhinNumber(player.getGhinNumber());
            response.setActive(player.isActive());
            response.setFrozenHandicapIndex(tripPlayer.getFrozenHandicapIndex());
            ScorecardParticipationStatus participationStatus = tripPlayer.getParticipationStatus();
            response.setParticipationStatus(participationStatus == null ? ScorecardParticipationStatus.ACTIVE.name() : participationStatus.name());
            response.setUnavailableRoundCount(tripParticipationService.countUnavailableRounds(tripId, player.getId()));
            response.setGhinHistoryCount(countUsableGhinHistoryRows(player, trip.getTripCode()));
            response.setDbScoreHistoryCount(countUsableDbScoreHistoryRows(player));
            response.setTripScoreCount(countUsableTripScoreRows(player));

            BigDecimal handicapIndex = null;

            try {
                if (TripHandicapMethod.FROZEN_GHIN_INDEX.equals(trip.getHandicapMethod())) {
                    handicapIndex = tripPlayer.getFrozenHandicapIndex();
                } else if (trip.getTripCode() != null && !trip.getTripCode().isBlank()) {
                    handicapIndex = tripHandicapService.calculateTripIndex(player, trip.getTripCode(), resolveEffectiveHandicapMethod(trip));
                }
            } catch (Exception ex) {
                handicapIndex = null;
            }

            response.setHandicapIndex(handicapIndex);
            response.setUsableHandicapIndex(handicapIndex != null);
            responses.add(response);
        }

        return responses;
    }

    @Transactional
    public List<TripPlayerResponse> updateTripPlayerParticipation(Long tripId, Long playerId, String participationStatusText) {
        tripParticipationService.updateParticipation(tripId, playerId, participationStatusText);
        return getTripPlayers(tripId);
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
        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundDateAsc(tripId);
        List<TripRoundListResponse> responses = new ArrayList<TripRoundListResponse>();

        for (Round round : rounds) {
            roundTeamAutoAssignmentService.syncTeamsFromGroupsIfNeeded(round.getId());

            List<Scorecard> scorecards = scorecardRepository.findByRound_Id(round.getId());
            List<RoundGroup> groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(round.getId());
            List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(round.getId());

            boolean finalized = Boolean.TRUE.equals(round.getFinalized());
            boolean needsGrouping = false;
            boolean needsTeams = false;
            boolean readyForScoring = false;

            if (!finalized) {
                needsGrouping = calculateNeedsGrouping(scorecards, groups);

                if (!needsGrouping) {
                    needsTeams = calculateNeedsTeams(round, scorecards, teams);
                }

                readyForScoring = !needsGrouping && !needsTeams;
            }

            TripRoundListResponse response = new TripRoundListResponse();
            response.setRoundId(round.getId());
            response.setRoundNumber(round.getRoundNumber());
            response.setRoundDate(round.getRoundDate());
            response.setCourseName(round.getCourse() != null ? round.getCourse().getName() : null);
            response.setTeeName(round.getStandardRoundTee() != null ? round.getStandardRoundTee().getTeeName() : null);
            response.setGameFormat(round.getFormat() != null ? round.getFormat().name() : null);
            response.setFinalized(finalized);
            response.setNeedsGrouping(needsGrouping);
            response.setNeedsTeams(needsTeams);
            response.setReadyForScoring(readyForScoring);
            response.setEvents(buildRoundEventResponses(round.getId()));

            responses.add(response);
        }

        return responses;
    }

    private List<RoundEventResponse> buildRoundEventResponses(Long roundId) {
        List<RoundEventResponse> responses = new ArrayList<RoundEventResponse>();
        if (roundId == null) {
            return responses;
        }

        List<RoundEvent> events = roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(roundId);
        for (RoundEvent event : events) {
            RoundEventResponse response = new RoundEventResponse();
            response.setId(event.getId());
            response.setRoundId(roundId);
            response.setEventType(event.getEventType());
            response.setEventName(event.getEventName());
            response.setEventOrder(event.getEventOrder());
            response.setActive(event.getActive());
            response.setUsesGross(event.getUsesGross());
            response.setUsesNet(event.getUsesNet());
            response.setUsesTeams(event.getUsesTeams());
            response.setTeamSize(event.getTeamSize());
            response.setHandicapPercent(event.getHandicapPercent());
            responses.add(response);
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public TripReadinessResponse getTripReadiness(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        return buildTripReadiness(trip);
    }

    @Transactional(readOnly = true)
    public void validateTripCanStart(Long tripId) {
        TripReadinessResponse readiness = getTripReadiness(tripId);

        if (Boolean.TRUE.equals(readiness.getCanStartTrip())) {
            return;
        }

        List<String> blockingItems = readiness.getBlockingItems();
        if (blockingItems == null || blockingItems.isEmpty()) {
            throw new IllegalStateException("Trip is not ready to start.");
        }

        throw new IllegalStateException("Trip is not ready to start: " + String.join(" ", blockingItems));
    }
 
    
    private CurrentRoundResponse toCurrentRoundResponse(Round round) {
        if (round == null) {
            return null;
        }

        CurrentRoundResponse response = new CurrentRoundResponse();
        response.setRoundId(round.getId());
        response.setRoundNumber(round.getRoundNumber());
        response.setRoundDate(
        	    round.getRoundDate() != null ? round.getRoundDate().toString() : null
        	);
        response.setFormat(round.getFormat() != null ? round.getFormat().name() : null);
        response.setScrambleTeamSize(resolveCurrentRoundScrambleTeamSize(round));
//        response.setIncludeInFourDayStandings(Boolean.TRUE.equals(round.getIncludeInFourDayStandings()));
        response.setCourseName(round.getCourse() != null ? round.getCourse().getName() : null);
        response.setTeeName(
                round.getStandardRoundTee() != null ? round.getStandardRoundTee().getTeeName() : null
        );
        response.setFinalized(Boolean.TRUE.equals(round.getFinalized()));
        return response;
    }
    
    private TripDateRange resolveTripDateRange(Trip trip) {
        LocalDate startDate = trip.getTripStartDate();
        LocalDate endDate = trip.getTripEndDate();

        if (startDate != null || endDate != null) {
            return new TripDateRange(startDate, endDate);
        }

        List<TripPlannedRound> plannedRounds = tripPlannedRoundService.findAllPlannedRounds(trip);

        for (TripPlannedRound plannedRound : plannedRounds) {
            if (plannedRound == null || plannedRound.getRoundDate() == null) {
                continue;
            }

            LocalDate roundDate = plannedRound.getRoundDate();
            if (startDate == null || roundDate.isBefore(startDate)) {
                startDate = roundDate;
            }
            if (endDate == null || roundDate.isAfter(endDate)) {
                endDate = roundDate;
            }
        }

        if (startDate == null || endDate == null) {
            List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundDateAsc(trip.getId());
            for (Round round : rounds) {
                if (round == null || round.getRoundDate() == null) {
                    continue;
                }

                LocalDate roundDate = round.getRoundDate();
                if (startDate == null || roundDate.isBefore(startDate)) {
                    startDate = roundDate;
                }
                if (endDate == null || roundDate.isAfter(endDate)) {
                    endDate = roundDate;
                }
            }
        }

        return new TripDateRange(startDate, endDate);
    }

    private static class TripDateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        private TripDateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }
    }


    private long countUsableGhinHistoryRows(Player player, String tripCode) {
        if (player == null || tripCode == null || tripCode.isBlank()) {
            return 0L;
        }
        return scoreHistoryEntryRepository
                .countByPlayerAndHandicapGroupCodeAndSourceTypeAndDifferentialIsNotNullAndManualDifferentialRequiredFalse(
                        player,
                        tripCode,
                        GHIN_FROZEN
                );
    }

    private long countUsableDbScoreHistoryRows(Player player) {
        if (player == null) {
            return 0L;
        }
        return scoreHistoryEntryRepository
                .countByPlayerAndSourceTypeAndDifferentialIsNotNullAndManualDifferentialRequiredFalse(
                        player,
                        DB_HISTORY_FROZEN
                );
    }

    private long countUsableTripScoreRows(Player player) {
        if (player == null) {
            return 0L;
        }
        return scoreHistoryEntryRepository
                .countByPlayerAndSourceTypeAndDifferentialIsNotNullAndManualDifferentialRequiredFalse(
                        player,
                        TRIP_ROUND
                );
    }

    private TripReadinessResponse buildTripReadiness(Trip trip) {
        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);
        List<TripPlannedRound> plannedRounds = tripPlannedRoundService.loadActivePlannedRounds(trip);

        int activePlayerCount = 0;
        for (TripPlayer tripPlayer : tripPlayers) {
            if (tripPlayer != null
                    && tripPlayer.getPlayer() != null
                    && tripPlayer.getPlayer().isActive()) {
                activePlayerCount++;
            }
        }

        int completedPlannedRoundCount = 0;
        for (TripPlannedRound plannedRound : plannedRounds) {
            if (isPlannedRoundComplete(plannedRound)) {
                completedPlannedRoundCount++;
            }
        }

        boolean handicapsEnabled = trip.getHandicapsEnabled() == null || Boolean.TRUE.equals(trip.getHandicapsEnabled());

        long unresolvedGhinFixCount = 0L;
        if (handicapsEnabled
                && !TripHandicapMethod.FROZEN_GHIN_INDEX.equals(trip.getHandicapMethod())
                && trip.getTripCode() != null && !trip.getTripCode().isBlank()) {
            unresolvedGhinFixCount =
                    scoreHistoryEntryRepository
                            .countByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrue(
                                    trip.getTripCode(),
                                    GHIN_FROZEN
                            );
        }

        boolean handicapIndexesReady = true;
        if (handicapsEnabled) {
            for (TripPlayer tripPlayer : tripPlayers) {
                if (tripPlayer == null
                        || tripPlayer.getPlayer() == null
                        || !tripPlayer.getPlayer().isActive()) {
                    continue;
                }

                if (TripHandicapMethod.FROZEN_GHIN_INDEX.equals(trip.getHandicapMethod())) {
                    if (tripPlayer.getFrozenHandicapIndex() == null) {
                        handicapIndexesReady = false;
                    }
                } else {
                    try {
                        BigDecimal calculatedIndex = tripHandicapService.calculateTripIndex(
                                tripPlayer.getPlayer(),
                                trip.getTripCode(),
                                resolveEffectiveHandicapMethod(trip)
                        );
                        if (calculatedIndex == null) {
                            handicapIndexesReady = false;
                        }
                    } catch (Exception ex) {
                        handicapIndexesReady = false;
                    }
                }
            }
        }

        boolean rosterReady = activePlayerCount > 0;
        boolean plannedRoundsReady =
                plannedRounds.size() >= MIN_PLANNED_ROUND_COUNT
                        && completedPlannedRoundCount == plannedRounds.size();
        boolean ghinFixesReady = unresolvedGhinFixCount == 0L;

        boolean alreadyStarted =
                Boolean.TRUE.equals(trip.getInitialized())
                        || TripStatus.IN_PROGRESS.equals(trip.getStatus())
                        || TripStatus.COMPLETE.equals(trip.getStatus());

        boolean canStartTrip =
                !alreadyStarted
                        && rosterReady
                        && plannedRoundsReady
                        && ghinFixesReady
                        && handicapIndexesReady;

        List<String> blockingItems = new ArrayList<String>();

        if (alreadyStarted) {
            blockingItems.add("Trip has already been started.");
        }

        if (!rosterReady) {
            blockingItems.add("Add at least one active player to the trip roster.");
        }

        if (plannedRounds.size() < MIN_PLANNED_ROUND_COUNT) {
            blockingItems.add("Trip must have at least one planned round.");
        } else if (completedPlannedRoundCount != plannedRounds.size()) {
            blockingItems.add("All planned rounds must have a date, format, course, and standard tee. Alternate tee is optional but must differ from the standard tee.");
        }

        if (!handicapIndexesReady) {
            if (TripHandicapMethod.FROZEN_GHIN_INDEX.equals(trip.getHandicapMethod())) {
                blockingItems.add("Enter a frozen GHIN handicap index for every active player before starting the trip, or mark the trip as Scratch / no handicaps.");
            } else {
                blockingItems.add("Every active player must have a calculable handicap index before starting the trip, or mark the trip as Scratch / no handicaps.");
            }
        }

        if (!ghinFixesReady) {
            blockingItems.add("Resolve all GHIN manual differential fixes before starting the trip.");
        }

        TripReadinessResponse response = new TripReadinessResponse();
        response.setActivePlayerCount(activePlayerCount);
        response.setPlannedRoundCount(plannedRounds.size());
        response.setCompletedPlannedRoundCount(completedPlannedRoundCount);
        response.setUnresolvedGhinFixCount(unresolvedGhinFixCount);
        response.setRosterReady(rosterReady);
        response.setPlannedRoundsReady(plannedRoundsReady);
        response.setGhinFixesReady(ghinFixesReady);
        response.setHandicapIndexesReady(handicapIndexesReady);
        response.setCanStartTrip(canStartTrip);
        response.setBlockingItems(blockingItems);

        return response;
    }
    private boolean isPlannedRoundComplete(TripPlannedRound plannedRound) {
        if (plannedRound == null) {
            return false;
        }
        if (plannedRound.getRoundDate() == null) {
            return false;
        }
        if (!tripPlannedRoundService.hasPlannedRoundEventConfiguration(plannedRound)) {
            return false;
        }
        if (plannedRound.getCourseId() == null) {
            return false;
        }
        if (plannedRound.getStandardTeeId() == null) {
            return false;
        }
        return true;
    }

    private boolean hasFemalePlayers(Trip trip) {
        if (trip == null) {
            return false;
        }
        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTrip(trip);
        for (TripPlayer tripPlayer : tripPlayers) {
            if (tripPlayer != null
                    && tripPlayer.getPlayer() != null
                    && "F".equalsIgnoreCase(tripPlayer.getPlayer().getGender())) {
                return true;
            }
        }
        return false;
    }


















    private TripHandicapMethod resolveEffectiveHandicapMethod(Trip trip) {
        if (trip == null || trip.getHandicapMethod() == null) {
            return DEFAULT_HANDICAP_METHOD;
        }
        return trip.getHandicapMethod();
    }























    private Integer resolveCurrentRoundScrambleTeamSize(Round round) {
        if (round == null || !roundEventCapabilityService.isScrambleRound(round)) {
            return 4;
        }
        int size = round.getScrambleTeamSize() == null ? 4 : round.getScrambleTeamSize();
        if (size < 2 || size > 4) {
            return 4;
        }
        return size;
    }
































    private boolean calculateNeedsGrouping(List<Scorecard> scorecards, List<RoundGroup> groups) {
        if (scorecards == null || scorecards.isEmpty()) {
            return true;
        }

        if (groups == null || groups.isEmpty()) {
            return true;
        }

        Set<Long> roundPlayerIds = new HashSet<Long>();

        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getPlayer() == null || scorecard.getPlayer().getId() == null) {
                continue;
            }

            roundPlayerIds.add(scorecard.getPlayer().getId());
        }

        if (roundPlayerIds.isEmpty()) {
            return true;
        }

        Set<Long> groupedPlayerIds = new HashSet<Long>();

        for (RoundGroup group : groups) {
            List<RoundGroupPlayer> players = group.getPlayers();

            if (players == null || players.isEmpty()) {
                return true;
            }

            if (players.size() > 4) {
                return true;
            }

            for (RoundGroupPlayer groupPlayer : players) {
                if (groupPlayer == null || groupPlayer.getPlayer() == null || groupPlayer.getPlayer().getId() == null) {
                    return true;
                }

                Long playerId = groupPlayer.getPlayer().getId();

                if (!roundPlayerIds.contains(playerId)) {
                    return true;
                }

                if (!groupedPlayerIds.add(playerId)) {
                    return true;
                }
            }
        }

        return groupedPlayerIds.size() != roundPlayerIds.size();
    }

    private boolean calculateNeedsTeams(Round round, List<Scorecard> scorecards, List<RoundTeam> teams) {
        RoundEventCapabilityService.RoundEventCapabilities capabilities = roundEventCapabilityService.getCapabilities(round);

        if (!capabilities.requiresTeams()) {
            return false;
        }

        if (scorecards == null || scorecards.isEmpty()) {
            return true;
        }

        if (teams == null || teams.isEmpty()) {
            return true;
        }

        int expectedTeamSize = roundEventCapabilityService.expectedTeamSize(round);
        Map<Long, Integer> teamCounts = new HashMap<Long, Integer>();
        Set<Long> knownTeamIds = new HashSet<Long>();

        for (RoundTeam team : teams) {
            if (team.getId() != null) {
                knownTeamIds.add(team.getId());
                teamCounts.put(team.getId(), 0);
            }
        }

        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getPlayer() == null || scorecard.getPlayer().getId() == null) {
                return true;
            }

            if (scorecard.getTeam() == null || scorecard.getTeam().getId() == null) {
                return true;
            }

            Long teamId = scorecard.getTeam().getId();

            if (!knownTeamIds.contains(teamId)) {
                return true;
            }

            Integer currentCount = teamCounts.get(teamId);
            if (currentCount == null) {
                currentCount = 0;
            }

            teamCounts.put(teamId, currentCount + 1);
        }

        for (Map.Entry<Long, Integer> entry : teamCounts.entrySet()) {
            Integer teamSize = entry.getValue();

            if (teamSize == null || teamSize.intValue() != expectedTeamSize) {
                return true;
            }
        }

        return false;
    }
}
