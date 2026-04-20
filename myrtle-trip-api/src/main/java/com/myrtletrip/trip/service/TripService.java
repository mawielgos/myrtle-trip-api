package com.myrtletrip.trip.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.handicap.source.frozen.FrozenGhinImportService;
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
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.dto.CurrentRoundResponse;
import com.myrtletrip.trip.dto.SaveTripPlannedRoundsRequest;
import com.myrtletrip.trip.dto.TripDetailResponse;
import com.myrtletrip.trip.dto.TripListResponse;
import com.myrtletrip.trip.dto.TripPlannedRoundRequest;
import com.myrtletrip.trip.dto.TripPlannedRoundResponse;
import com.myrtletrip.trip.dto.TripPlayerResponse;
import com.myrtletrip.trip.dto.TripReadinessResponse;
import com.myrtletrip.trip.dto.TripRoundListResponse;
import com.myrtletrip.trip.dto.TripSetupRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
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
    private static final int DEFAULT_PLANNED_ROUND_COUNT = 5;
    private static final int MIN_PLANNED_ROUND_COUNT = 1;

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final PlayerRepository playerRepository;
    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamAutoAssignmentService roundTeamAutoAssignmentService;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final TripHandicapService tripHandicapService;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    private final FrozenGhinImportService frozenGhinImportService;

    public TripService(TripRepository tripRepository,
                       TripPlayerRepository tripPlayerRepository,
                       PlayerRepository playerRepository,
                       RoundRepository roundRepository,
                       ScorecardRepository scorecardRepository,
                       RoundGroupRepository roundGroupRepository,
                       RoundTeamRepository roundTeamRepository,
                       RoundTeamAutoAssignmentService roundTeamAutoAssignmentService,
                       TripPlannedRoundRepository tripPlannedRoundRepository,
                       CourseRepository courseRepository,
                       CourseTeeRepository courseTeeRepository,
                       TripHandicapService tripHandicapService,
                       ScoreHistoryEntryRepository scoreHistoryEntryRepository,
                       FrozenGhinImportService frozenGhinImportService) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.playerRepository = playerRepository;
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamAutoAssignmentService = roundTeamAutoAssignmentService;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.tripHandicapService = tripHandicapService;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
        this.frozenGhinImportService = frozenGhinImportService;
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

        validateUniquePlayerIds(request.getPlayerIds());

        Trip trip;

        if (request.getTripId() != null) {
            trip = tripRepository.findById(request.getTripId())
                    .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + request.getTripId()));
        } else {
            trip = tripRepository.findByTripCode(request.getTripCode()).orElse(null);
            if (trip == null) {
                trip = new Trip();
            }
        }

        boolean isNewTrip = trip.getId() == null;

        if (!isNewTrip) {
            assertTripSetupEditable(trip);
        }
        
        if (!isNewTrip && !trip.getTripCode().equals(request.getTripCode())) {
            long existingHandicapRows = scoreHistoryEntryRepository.countByHandicapGroupCode(trip.getTripCode());

            if (existingHandicapRows > 0) {
                throw new IllegalStateException(
                        "Cannot change trip_code after handicap data has been initialized."
                );
            }
        }

        trip.setTripCode(request.getTripCode());
        trip.setName(request.getName());
        trip.setTripYear(request.getTripYear());
        trip.setEntryFee(request.getEntryFee());

        if (isNewTrip && trip.getInitialized() == null) {
            trip.setInitialized(false);
        }

        if (trip.getStatus() == null) {
            trip.setStatus(TripStatus.PLANNING);
        }

        if (!TripStatus.COMPLETE.equals(trip.getStatus())) {
            if (!Boolean.TRUE.equals(trip.getInitialized())) {
                trip.setStatus(TripStatus.PLANNING);
            }
        }

        trip = tripRepository.saveAndFlush(trip);

        tripPlayerRepository.deleteByTrip(trip);
        tripPlayerRepository.flush();

        int displayOrder = 1;

        for (Long playerId : request.getPlayerIds()) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

            TripPlayer tripPlayer = new TripPlayer();
            tripPlayer.setTrip(trip);
            tripPlayer.setPlayer(player);
            tripPlayer.setDisplayOrder(displayOrder);
            tripPlayerRepository.save(tripPlayer);

            displayOrder++;
        }

        tripPlayerRepository.flush();

        if (isNewTrip) {
            createDefaultPlannedRounds(trip);
        }

        return trip;
    }

    @Transactional(readOnly = true)
    public List<TripListResponse> getTrips() {
        List<Trip> trips = tripRepository.findAll();
        List<TripListResponse> responses = new ArrayList<TripListResponse>();

        for (Trip trip : trips) {
            TripListResponse response = new TripListResponse();
            response.setTripId(trip.getId());
            response.setTripName(trip.getName());
            response.setTripCode(trip.getTripCode());
            response.setTripYear(trip.getTripYear());
            response.setStatus(trip.getStatus() != null ? trip.getStatus().name() : null);

            long playerCount = tripPlayerRepository.countByTrip(trip);
            long roundCount = roundRepository.countByTrip_Id(trip.getId());
            response.setPlayerCount(playerCount);
            response.setRoundCount(roundCount);
            response.setCanDelete(roundCount == 0L);

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
        response.setInitialized(trip.getInitialized());
        response.setStatus(trip.getStatus() != null ? trip.getStatus().name() : null);

        TripReadinessResponse readiness = buildTripReadiness(trip);
        response.setUnresolvedGhinFixCount(readiness.getUnresolvedGhinFixCount());
        response.setReadiness(readiness);

        Round currentRound = findCurrentRoundEntity(tripId);
        response.setCurrentRound(toCurrentRoundResponse(currentRound));
        return response;
    }

    @Transactional(readOnly = true)
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
            response.setActive(player.isActive());

            BigDecimal handicapIndex = null;

            try {
                if (trip.getTripCode() != null && !trip.getTripCode().isBlank()) {
                    handicapIndex = tripHandicapService.calculateTripIndex(player, trip.getTripCode());
                }
            } catch (Exception ex) {
                handicapIndex = null;
            }

            response.setHandicapIndex(handicapIndex);
            responses.add(response);
        }

        return responses;
    }

    @Transactional
    public void deleteTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        long startedRoundCount = roundRepository.countByTrip_Id(tripId);
        if (startedRoundCount > 0L) {
            throw new IllegalStateException("Only trips with no started rounds can be deleted.");
        }

        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);
        String handicapGroupCode = trip.getTripCode();

        if (handicapGroupCode != null && !handicapGroupCode.isBlank()) {
            for (TripPlayer tripPlayer : tripPlayers) {
                if (tripPlayer == null || tripPlayer.getPlayer() == null) {
                    continue;
                }

                scoreHistoryEntryRepository.deleteByPlayerAndHandicapGroupCodeAndSourceType(
                        tripPlayer.getPlayer(),
                        handicapGroupCode,
                        GHIN_FROZEN
                );
            }
        }

        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);

        if (!plannedRounds.isEmpty()) {
            tripPlannedRoundRepository.deleteAll(plannedRounds);
            tripPlannedRoundRepository.flush();
        }

        if (!tripPlayers.isEmpty()) {
            tripPlayerRepository.deleteAll(tripPlayers);
            tripPlayerRepository.flush();
        }

        tripRepository.delete(trip);
        tripRepository.flush();
    }

    @Transactional
    public void initializeTripGhin(Long tripId) throws Exception {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (TripStatus.IN_PROGRESS.equals(trip.getStatus())
                || TripStatus.COMPLETE.equals(trip.getStatus())
                || Boolean.TRUE.equals(trip.getInitialized())) {
            throw new IllegalStateException("GHIN baseline cannot be loaded after the trip has started.");
        }
        
        if (trip.getTripCode() == null || trip.getTripCode().isBlank()) {
            throw new IllegalArgumentException("Trip code is required before loading GHIN baseline.");
        }

        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);
        if (tripPlayers.isEmpty()) {
            throw new IllegalArgumentException("Trip must have players before loading GHIN baseline.");
        }

        List<Player> ghinPlayers = new ArrayList<Player>();

        for (TripPlayer tripPlayer : tripPlayers) {
            if (tripPlayer == null || tripPlayer.getPlayer() == null) {
                continue;
            }

            Player player = tripPlayer.getPlayer();
            if (!player.isActive()) {
                continue;
            }

            if (!"GHIN".equalsIgnoreCase(player.getHandicapMethod())) {
                continue;
            }

            ghinPlayers.add(player);
        }

        if (ghinPlayers.isEmpty()) {
            throw new IllegalArgumentException("No active GHIN players were found on this trip.");
        }

        frozenGhinImportService.initializeFrozenGhinForPlayers(ghinPlayers, trip.getTripCode());
    }
    @Transactional(readOnly = true)
    public List<TripPlannedRoundResponse> getPlannedRounds(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        List<TripPlannedRound> rounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);
        List<TripPlannedRoundResponse> responses = new ArrayList<TripPlannedRoundResponse>();

        for (TripPlannedRound round : rounds) {
            responses.add(toPlannedRoundResponse(round));
        }

        return responses;
    }

    @Transactional
    public List<TripPlannedRoundResponse> savePlannedRounds(Long tripId, SaveTripPlannedRoundsRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        assertTripSetupEditable(trip);
        
        if (request == null || request.getRounds() == null || request.getRounds().isEmpty()) {
            throw new IllegalArgumentException("Planned rounds are required.");
        }

        validatePlannedRounds(request.getRounds());

        tripPlannedRoundRepository.deleteByTrip(trip);
        tripPlannedRoundRepository.flush();

        List<TripPlannedRound> roundsToSave = new ArrayList<TripPlannedRound>();

        for (TripPlannedRoundRequest roundRequest : request.getRounds()) {
            TripPlannedRound plannedRound = new TripPlannedRound();
            plannedRound.setTrip(trip);
            plannedRound.setRoundNumber(roundRequest.getRoundNumber());
            plannedRound.setRoundDate(roundRequest.getRoundDate());
            plannedRound.setCourseId(roundRequest.getCourseId());
            plannedRound.setStandardTeeId(roundRequest.getStandardTeeId());
            plannedRound.setAlternateTeeId(roundRequest.getAlternateTeeId());
            plannedRound.setFormat(parseRoundFormat(roundRequest.getFormat()));
            plannedRound.setIncludeInFourDayStandings(Boolean.TRUE.equals(roundRequest.getIncludeInFourDayStandings()));
            plannedRound.setIncludeInScrambleSeeding(Boolean.TRUE.equals(roundRequest.getIncludeInScrambleSeeding()));

            roundsToSave.add(plannedRound);
        }

        tripPlannedRoundRepository.saveAll(roundsToSave);
        tripPlannedRoundRepository.flush();

        return getPlannedRounds(tripId);
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

        Round currentRound = findCurrentRoundEntity(tripId);

        if (currentRound == null) {
            trip.setStatus(TripStatus.COMPLETE);
        } else if (Boolean.TRUE.equals(trip.getInitialized())) {
            trip.setStatus(TripStatus.IN_PROGRESS);
        }

        tripRepository.save(trip);
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
 
    private void assertTripSetupEditable(Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("Trip is required.");
        }

        if (TripStatus.IN_PROGRESS.equals(trip.getStatus())
                || TripStatus.COMPLETE.equals(trip.getStatus())
                || Boolean.TRUE.equals(trip.getInitialized())) {
            throw new IllegalStateException("Trip setup cannot be changed after the trip has started.");
        }
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
//        response.setIncludeInFourDayStandings(Boolean.TRUE.equals(round.getIncludeInFourDayStandings()));
//        response.setIncludeInScrambleSeeding(Boolean.TRUE.equals(round.getIncludeInScrambleSeeding()));
        response.setCourseName(round.getCourse() != null ? round.getCourse().getName() : null);
        response.setTeeName(
                round.getStandardRoundTee() != null ? round.getStandardRoundTee().getTeeName() : null
        );
        response.setFinalized(Boolean.TRUE.equals(round.getFinalized()));
        return response;
    }
    
    private TripDateRange resolveTripDateRange(Trip trip) {
        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);

        LocalDate startDate = null;
        LocalDate endDate = null;

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


    private TripReadinessResponse buildTripReadiness(Trip trip) {
        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);
        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);

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

        long unresolvedGhinFixCount = 0L;
        if (trip.getTripCode() != null && !trip.getTripCode().isBlank()) {
            unresolvedGhinFixCount =
                    scoreHistoryEntryRepository
                            .countByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrue(
                                    trip.getTripCode(),
                                    GHIN_FROZEN
                            );
        }

        boolean hasFourDayIncludedRound = false;
        boolean hasScrambleSeedingIncludedRound = false;
        for (TripPlannedRound plannedRound : plannedRounds) {
            if (Boolean.TRUE.equals(plannedRound.getIncludeInFourDayStandings())) {
                hasFourDayIncludedRound = true;
            }
            if (Boolean.TRUE.equals(plannedRound.getIncludeInScrambleSeeding())) {
                hasScrambleSeedingIncludedRound = true;
            }
        }

        boolean rosterReady = activePlayerCount > 0;
        boolean plannedRoundsReady =
                plannedRounds.size() >= MIN_PLANNED_ROUND_COUNT
                        && completedPlannedRoundCount == plannedRounds.size()
                        && hasFourDayIncludedRound
                        && hasScrambleSeedingIncludedRound;
        boolean ghinFixesReady = unresolvedGhinFixCount == 0L;

        boolean alreadyStarted =
                Boolean.TRUE.equals(trip.getInitialized())
                        || TripStatus.IN_PROGRESS.equals(trip.getStatus())
                        || TripStatus.COMPLETE.equals(trip.getStatus());

        boolean canStartTrip =
                !alreadyStarted
                        && rosterReady
                        && plannedRoundsReady
                        && ghinFixesReady;

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

        if (!hasFourDayIncludedRound) {
            blockingItems.add("Select at least one planned round to include in the 4-day tournament standings.");
        }

        if (!hasScrambleSeedingIncludedRound) {
            blockingItems.add("Select at least one planned round to include in scramble seeding.");
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
        if (plannedRound.getFormat() == null) {
            return false;
        }
        if (plannedRound.getCourseId() == null) {
            return false;
        }
        if (plannedRound.getStandardTeeId() == null) {
            return false;
        }
        if (plannedRound.getAlternateTeeId() != null
                && plannedRound.getAlternateTeeId().equals(plannedRound.getStandardTeeId())) {
            return false;
        }

        return true;
    }

    private void createDefaultPlannedRounds(Trip trip) {
        if (tripPlannedRoundRepository.countByTrip(trip) > 0) {
            return;
        }

        for (int i = 1; i <= DEFAULT_PLANNED_ROUND_COUNT; i++) {
            TripPlannedRound plannedRound = new TripPlannedRound();
            plannedRound.setTrip(trip);
            plannedRound.setRoundNumber(i);
            plannedRound.setFormat(defaultFormatForRound(i));
            plannedRound.setIncludeInFourDayStandings(defaultIncludeInFourDayStandings(i));
            plannedRound.setIncludeInScrambleSeeding(defaultIncludeInScrambleSeeding(i));
            tripPlannedRoundRepository.save(plannedRound);
        }
    }

    private void validateUniquePlayerIds(List<Long> playerIds) {
        Set<Long> seen = new HashSet<Long>();

        for (Long playerId : playerIds) {
            if (playerId == null) {
                throw new IllegalArgumentException("playerId is required");
            }
            if (!seen.add(playerId)) {
                throw new IllegalArgumentException("Duplicate playerId: " + playerId);
            }
        }
    }

    private void validatePlannedRounds(List<TripPlannedRoundRequest> rounds) {
        Set<Integer> usedRoundNumbers = new HashSet<Integer>();
        boolean anyIncludedInFourDayStandings = false;
        boolean anyIncludedInScrambleSeeding = false;

        for (TripPlannedRoundRequest round : rounds) {
            if (round == null) {
                throw new IllegalArgumentException("Planned round is required.");
            }
            if (round.getRoundNumber() == null) {
                throw new IllegalArgumentException("roundNumber is required.");
            }
            if (round.getRoundNumber() < 1) {
                throw new IllegalArgumentException("roundNumber must be >= 1.");
            }
            if (!usedRoundNumbers.add(round.getRoundNumber())) {
                throw new IllegalArgumentException("Duplicate planned round number: " + round.getRoundNumber());
            }
            if ((round.getStandardTeeId() != null || round.getAlternateTeeId() != null) && round.getCourseId() == null) {
                throw new IllegalArgumentException("courseId is required when tee selections are provided for round " + round.getRoundNumber());
            }
            if (round.getFormat() != null && !round.getFormat().isBlank()) {
                parseRoundFormat(round.getFormat());
            }
            if (round.getAlternateTeeId() != null
                    && round.getStandardTeeId() != null
                    && round.getAlternateTeeId().equals(round.getStandardTeeId())) {
                throw new IllegalArgumentException(
                        "alternateTeeId must be different from standardTeeId for round " + round.getRoundNumber()
                );
            }
            if (Boolean.TRUE.equals(round.getIncludeInFourDayStandings())) {
                anyIncludedInFourDayStandings = true;
            }
            if (Boolean.TRUE.equals(round.getIncludeInScrambleSeeding())) {
                anyIncludedInScrambleSeeding = true;
            }
        }

        if (!anyIncludedInFourDayStandings) {
            throw new IllegalArgumentException("At least one planned round must be included in the 4-day tournament standings.");
        }
        if (!anyIncludedInScrambleSeeding) {
            throw new IllegalArgumentException("At least one planned round must be included in scramble seeding.");
        }
    }

    private TripPlannedRoundResponse toPlannedRoundResponse(TripPlannedRound round) {
        TripPlannedRoundResponse response = new TripPlannedRoundResponse();
        response.setPlannedRoundId(round.getId());
        response.setRoundNumber(round.getRoundNumber());
        response.setRoundDate(round.getRoundDate());
        response.setCourseId(round.getCourseId());
        response.setStandardTeeId(round.getStandardTeeId());
        response.setAlternateTeeId(round.getAlternateTeeId());
        response.setFormat(round.getFormat() != null ? round.getFormat().name() : null);
        response.setIncludeInFourDayStandings(Boolean.TRUE.equals(round.getIncludeInFourDayStandings()));
        response.setIncludeInScrambleSeeding(Boolean.TRUE.equals(round.getIncludeInScrambleSeeding()));

        Course course = null;
        if (round.getCourseId() != null) {
            course = courseRepository.findById(round.getCourseId()).orElse(null);
        }

        CourseTee standardTee = null;
        if (round.getStandardTeeId() != null) {
            standardTee = courseTeeRepository.findById(round.getStandardTeeId()).orElse(null);
        }

        CourseTee alternateTee = null;
        if (round.getAlternateTeeId() != null) {
            alternateTee = courseTeeRepository.findById(round.getAlternateTeeId()).orElse(null);
        }

        response.setCourseName(course != null ? course.getName() : null);
        response.setStandardTeeDisplay(formatCourseTeeDisplay(standardTee));
        response.setAlternateTeeDisplay(formatCourseTeeDisplay(alternateTee));

        return response;
    }

    private String formatCourseTeeDisplay(CourseTee tee) {
        if (tee == null) {
            return null;
        }

        String teeName = tee.getTeeName();
        if (tee.getCourseRating() == null || tee.getSlope() == null) {
            return teeName;
        }

        return teeName + " (" + tee.getCourseRating() + "/" + tee.getSlope() + ")";
    }

    private RoundFormat parseRoundFormat(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return RoundFormat.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid round format: " + value);
        }
    }

    private boolean defaultIncludeInFourDayStandings(int roundNumber) {
        return roundNumber <= 4;
    }

    private boolean defaultIncludeInScrambleSeeding(int roundNumber) {
        return roundNumber <= 3;
    }

    private RoundFormat defaultFormatForRound(int roundNumber) {
        if (roundNumber == 1) {
            return RoundFormat.MIDDLE_MAN;
        }
        if (roundNumber == 2) {
            return RoundFormat.ONE_TWO_THREE;
        }
        if (roundNumber == 3) {
            return RoundFormat.TWO_MAN_LOW_NET;
        }
        if (roundNumber == 4) {
            return RoundFormat.THREE_LOW_NET;
        }
        return RoundFormat.TEAM_SCRAMBLE;
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
        RoundFormat format = round.getFormat();

        if (format == null || !format.requiresTeams()) {
            return false;
        }

        if (scorecards == null || scorecards.isEmpty()) {
            return true;
        }

        if (teams == null || teams.isEmpty()) {
            return true;
        }

        int expectedTeamSize = format.expectedTeamSize();
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
