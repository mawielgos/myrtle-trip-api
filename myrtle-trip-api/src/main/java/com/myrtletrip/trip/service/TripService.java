package com.myrtletrip.trip.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
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
import com.myrtletrip.trip.model.TripHandicapMethod;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final TripHandicapMethod DEFAULT_HANDICAP_METHOD = TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY;

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
    private final CourseHoleRepository courseHoleRepository;
    private final CourseTeeComboHoleRepository courseTeeComboHoleRepository;
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
                       CourseHoleRepository courseHoleRepository,
                       CourseTeeComboHoleRepository courseTeeComboHoleRepository,
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
        this.courseHoleRepository = courseHoleRepository;
        this.courseTeeComboHoleRepository = courseTeeComboHoleRepository;
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

        TripHandicapMethod resolvedHandicapMethod = resolveTripHandicapMethod(request.getHandicapMethod());
        validateTripHandicapPolicy(request, resolvedHandicapMethod);
        validateTripDatesAndRoundCount(request);

        trip.setTripCode(request.getTripCode());
        trip.setName(request.getName());
        trip.setTripYear(request.getTripYear());
        trip.setEntryFee(request.getEntryFee());
        trip.setTripStartDate(request.getTripStartDate());
        trip.setTripEndDate(request.getTripEndDate());
        trip.setPlannedRoundCount(resolvePlannedRoundCount(request.getPlannedRoundCount()));
        trip.setHandicapsEnabled(request.getHandicapsEnabled() == null ? Boolean.TRUE : request.getHandicapsEnabled());
        trip.setHandicapMethod(resolvedHandicapMethod);

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

        Map<Long, BigDecimal> existingFrozenIndexes = new HashMap<Long, BigDecimal>();
        List<TripPlayer> existingTripPlayers = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);
        for (TripPlayer existingTripPlayer : existingTripPlayers) {
            if (existingTripPlayer != null && existingTripPlayer.getPlayer() != null) {
                existingFrozenIndexes.put(existingTripPlayer.getPlayer().getId(), existingTripPlayer.getFrozenHandicapIndex());
            }
        }

        tripPlayerRepository.deleteByTrip(trip);
        tripPlayerRepository.flush();

        int displayOrder = 1;

        for (Long playerId : request.getPlayerIds()) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

            if (!player.isActive()) {
                throw new IllegalArgumentException("Only active players can be added to a trip roster: " + player.getDisplayName());
            }

            TripPlayer tripPlayer = new TripPlayer();
            tripPlayer.setTrip(trip);
            tripPlayer.setPlayer(player);
            tripPlayer.setDisplayOrder(displayOrder);
            tripPlayer.setFrozenHandicapIndex(resolveFrozenHandicapIndex(request, resolvedHandicapMethod, playerId, existingFrozenIndexes));
            tripPlayerRepository.save(tripPlayer);

            displayOrder++;
        }

        tripPlayerRepository.flush();

        if (isNewTrip) {
            createDefaultPlannedRounds(trip);
        } else {
            syncPlannedRoundsToTripRoundCount(trip);
            validateExistingPlannedRoundsWithinTripDates(trip);
        }

        return trip;
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
            response.setPlannedRoundCount(resolvePlannedRoundCount(trip.getPlannedRoundCount()));
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
        response.setPlannedRoundCount(resolvePlannedRoundCount(trip.getPlannedRoundCount()));
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
    public void archiveTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (Boolean.TRUE.equals(trip.getArchived())) {
            return;
        }

        trip.setArchived(Boolean.TRUE);
        trip.setArchivedAt(LocalDateTime.now());
        tripRepository.save(trip);
    }

    @Transactional
    public void restoreTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (!Boolean.TRUE.equals(trip.getArchived())) {
            return;
        }

        trip.setArchived(Boolean.FALSE);
        trip.setArchivedAt(null);
        tripRepository.save(trip);
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

        List<TripPlannedRound> rounds = loadActivePlannedRounds(trip);
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

        validatePlannedRounds(request.getRounds(), trip);

        List<TripPlannedRoundRequest> sequencedRequests = sortPlannedRoundRequestsByPlaySequence(request.getRounds());

        tripPlannedRoundRepository.deleteByTrip(trip);
        tripPlannedRoundRepository.flush();

        List<TripPlannedRound> roundsToSave = new ArrayList<TripPlannedRound>();

        for (int index = 0; index < sequencedRequests.size(); index++) {
            TripPlannedRoundRequest roundRequest = sequencedRequests.get(index);
            TripPlannedRound plannedRound = new TripPlannedRound();
            plannedRound.setTrip(trip);
            plannedRound.setRoundNumber(index + 1);
            plannedRound.setRoundDate(roundRequest.getRoundDate());
            plannedRound.setCourseId(roundRequest.getCourseId());
            plannedRound.setStandardTeeId(roundRequest.getDefaultTeeId());
            plannedRound.setWomenDefaultTeeId(roundRequest.getWomenDefaultTeeId());
            RoundFormat parsedFormat = parseRoundFormat(roundRequest.getFormat());
            plannedRound.setFormat(parsedFormat);
            plannedRound.setScrambleTeamSize(resolveScrambleTeamSize(parsedFormat, roundRequest.getScrambleTeamSize()));
            plannedRound.setIncludeInFourDayStandings(Boolean.TRUE.equals(roundRequest.getIncludeInFourDayStandings()));

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

        if (TripStatus.COMPLETE.equals(trip.getStatus())) {
            // Explicit trip completion is sticky. Corrections should not reopen the trip automatically.
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
        response.setScrambleTeamSize(resolveScrambleTeamSize(round.getFormat(), round.getScrambleTeamSize()));
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

        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);

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
        List<TripPlannedRound> plannedRounds = loadActivePlannedRounds(trip);

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
        if (plannedRound.getFormat() == null) {
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

    private int resolvePlannedRoundCount(Integer plannedRoundCount) {
        if (plannedRoundCount == null) {
            return DEFAULT_PLANNED_ROUND_COUNT;
        }
        return plannedRoundCount;
    }

    private void validateTripDatesAndRoundCount(TripSetupRequest request) {
        int plannedRoundCount = resolvePlannedRoundCount(request.getPlannedRoundCount());
        if (plannedRoundCount < 1 || plannedRoundCount > 12) {
            throw new IllegalArgumentException("Planned round count must be between 1 and 12.");
        }

        if (request.getTripStartDate() == null) {
            throw new IllegalArgumentException("Trip start date is required.");
        }
        if (request.getTripEndDate() == null) {
            throw new IllegalArgumentException("Trip end date is required.");
        }
        if (request.getTripEndDate().isBefore(request.getTripStartDate())) {
            throw new IllegalArgumentException("Trip end date cannot be before trip start date.");
        }
    }

    private void validateExistingPlannedRoundsWithinTripDates(Trip trip) {
        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);
        for (TripPlannedRound plannedRound : plannedRounds) {
            if (plannedRound == null) {
                continue;
            }
            validateRoundDateWithinTripDates(trip, plannedRound.getRoundDate(), plannedRound.getRoundNumber());
        }
    }

    private void validateRoundDateWithinTripDates(Trip trip, LocalDate roundDate, Integer roundNumber) {
        if (roundDate == null || trip == null) {
            return;
        }

        LocalDate tripStartDate = trip.getTripStartDate();
        LocalDate tripEndDate = trip.getTripEndDate();

        if (tripStartDate != null && roundDate.isBefore(tripStartDate)) {
            throw new IllegalArgumentException("Round " + roundNumber + " date must be on or after the trip start date.");
        }
        if (tripEndDate != null && roundDate.isAfter(tripEndDate)) {
            throw new IllegalArgumentException("Round " + roundNumber + " date must be on or before the trip end date.");
        }
    }


    private List<TripPlannedRound> loadActivePlannedRounds(Trip trip) {
        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);
        List<TripPlannedRound> active = new ArrayList<TripPlannedRound>();
        int plannedRoundCount = resolvePlannedRoundCount(trip != null ? trip.getPlannedRoundCount() : null);

        for (TripPlannedRound plannedRound : plannedRounds) {
            if (plannedRound == null || plannedRound.getRoundNumber() == null) {
                continue;
            }
            if (plannedRound.getRoundNumber() < 1 || plannedRound.getRoundNumber() > plannedRoundCount) {
                continue;
            }
            active.add(plannedRound);
        }

        return active;
    }

    private void syncPlannedRoundsToTripRoundCount(Trip trip) {
        int targetCount = resolvePlannedRoundCount(trip.getPlannedRoundCount());
        List<TripPlannedRound> existingRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);

        if (existingRounds.size() == targetCount) {
            return;
        }

        if (existingRounds.size() > targetCount) {
            for (int i = targetCount; i < existingRounds.size(); i++) {
                TripPlannedRound plannedRound = existingRounds.get(i);
                if (plannedRound.getRoundDate() != null
                        || plannedRound.getCourseId() != null
                        || plannedRound.getStandardTeeId() != null
                        || plannedRound.getFormat() != null
                        || Boolean.TRUE.equals(plannedRound.getIncludeInFourDayStandings())) {
                    throw new IllegalArgumentException("Cannot reduce planned round count because Round " + plannedRound.getRoundNumber() + " already has setup details. Clear that round first.");
                }
            }

            for (int i = existingRounds.size() - 1; i >= targetCount; i--) {
                tripPlannedRoundRepository.delete(existingRounds.get(i));
            }
            tripPlannedRoundRepository.flush();
            return;
        }

        for (int roundNumber = existingRounds.size() + 1; roundNumber <= targetCount; roundNumber++) {
            TripPlannedRound plannedRound = new TripPlannedRound();
            plannedRound.setTrip(trip);
            plannedRound.setRoundNumber(roundNumber);
            plannedRound.setFormat(defaultFormatForRound(roundNumber));
            plannedRound.setIncludeInFourDayStandings(false);
            tripPlannedRoundRepository.save(plannedRound);
        }
        tripPlannedRoundRepository.flush();
    }

    private void createDefaultPlannedRounds(Trip trip) {
        if (tripPlannedRoundRepository.countByTrip(trip) > 0) {
            return;
        }

        int plannedRoundCount = resolvePlannedRoundCount(trip.getPlannedRoundCount());

        for (int i = 1; i <= plannedRoundCount; i++) {
            TripPlannedRound plannedRound = new TripPlannedRound();
            plannedRound.setTrip(trip);
            plannedRound.setRoundNumber(i);
            plannedRound.setFormat(defaultFormatForRound(i));
            plannedRound.setIncludeInFourDayStandings(defaultIncludeInFourDayStandings(i));
            tripPlannedRoundRepository.save(plannedRound);
        }
    }

    private TripHandicapMethod resolveTripHandicapMethod(String rawMethod) {
        if (rawMethod == null || rawMethod.isBlank()) {
            return DEFAULT_HANDICAP_METHOD;
        }

        String normalized = rawMethod.trim().toUpperCase();

        if ("GHIN".equals(normalized)) {
            return TripHandicapMethod.GHIN_HISTORY;
        }

        if ("DB_SCORE_HISTORY".equals(normalized) || "MYRTLE_BEACH".equals(normalized)) {
            return TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY;
        }

        try {
            return TripHandicapMethod.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported trip handicap method: " + rawMethod);
        }
    }

    private TripHandicapMethod resolveEffectiveHandicapMethod(Trip trip) {
        if (trip == null || trip.getHandicapMethod() == null) {
            return DEFAULT_HANDICAP_METHOD;
        }
        return trip.getHandicapMethod();
    }

    private void validateTripHandicapPolicy(TripSetupRequest request, TripHandicapMethod handicapMethod) {
        if (request.getHandicapsEnabled() != null && !Boolean.TRUE.equals(request.getHandicapsEnabled())) {
            return;
        }

        if (!TripHandicapMethod.FROZEN_GHIN_INDEX.equals(handicapMethod)) {
            return;
        }

        if (request.getFrozenHandicapIndexesByPlayerId() == null) {
            throw new IllegalArgumentException("Frozen GHIN Index is required for every selected player.");
        }

        for (Long playerId : request.getPlayerIds()) {
            BigDecimal frozenIndex = request.getFrozenHandicapIndexesByPlayerId().get(playerId);
            if (frozenIndex == null) {
                throw new IllegalArgumentException("Frozen GHIN Index is required for every selected player.");
            }
            if (frozenIndex.compareTo(BigDecimal.valueOf(-10.0)) < 0
                    || frozenIndex.compareTo(BigDecimal.valueOf(54.0)) > 0) {
                throw new IllegalArgumentException("Frozen GHIN Index must be between -10.0 and 54.0.");
            }
        }
    }

    private BigDecimal resolveFrozenHandicapIndex(TripSetupRequest request,
                                                  TripHandicapMethod handicapMethod,
                                                  Long playerId,
                                                  Map<Long, BigDecimal> existingFrozenIndexes) {
        if (request.getHandicapsEnabled() != null && !Boolean.TRUE.equals(request.getHandicapsEnabled())) {
            return null;
        }

        if (!TripHandicapMethod.FROZEN_GHIN_INDEX.equals(handicapMethod)) {
            return null;
        }

        if (request.getFrozenHandicapIndexesByPlayerId() != null
                && request.getFrozenHandicapIndexesByPlayerId().containsKey(playerId)) {
            return request.getFrozenHandicapIndexesByPlayerId().get(playerId);
        }

        return existingFrozenIndexes.get(playerId);
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

    private List<TripPlannedRoundRequest> sortPlannedRoundRequestsByPlaySequence(List<TripPlannedRoundRequest> rounds) {
        List<TripPlannedRoundRequest> sorted = new ArrayList<TripPlannedRoundRequest>(rounds);
        sorted.sort(new Comparator<TripPlannedRoundRequest>() {
            @Override
            public int compare(TripPlannedRoundRequest left, TripPlannedRoundRequest right) {
                LocalDate leftDate = left != null ? left.getRoundDate() : null;
                LocalDate rightDate = right != null ? right.getRoundDate() : null;

                if (leftDate != null && rightDate != null && !leftDate.equals(rightDate)) {
                    return leftDate.compareTo(rightDate);
                }
                if (leftDate != null && rightDate == null) {
                    return -1;
                }
                if (leftDate == null && rightDate != null) {
                    return 1;
                }

                Integer leftRoundNumber = left != null ? left.getRoundNumber() : null;
                Integer rightRoundNumber = right != null ? right.getRoundNumber() : null;
                if (leftRoundNumber == null && rightRoundNumber == null) {
                    return 0;
                }
                if (leftRoundNumber == null) {
                    return 1;
                }
                if (rightRoundNumber == null) {
                    return -1;
                }
                return leftRoundNumber.compareTo(rightRoundNumber);
            }
        });
        return sorted;
    }

    private void validatePlannedRounds(List<TripPlannedRoundRequest> rounds, Trip trip) {
        Set<Integer> usedRoundNumbers = new HashSet<Integer>();
        int expectedRoundCount = resolvePlannedRoundCount(trip.getPlannedRoundCount());

        if (rounds.size() != expectedRoundCount) {
            throw new IllegalArgumentException("Trip is configured for " + expectedRoundCount + " planned rounds. Update the trip round count before adding or removing round rows.");
        }

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
            if (round.getRoundNumber() > expectedRoundCount) {
                throw new IllegalArgumentException("roundNumber must be <= the trip planned round count.");
            }
            if (!usedRoundNumbers.add(round.getRoundNumber())) {
                throw new IllegalArgumentException("Duplicate planned round number: " + round.getRoundNumber());
            }
            validateRoundDateWithinTripDates(trip, round.getRoundDate(), round.getRoundNumber());

            if (round.getDefaultTeeId() != null && round.getCourseId() == null) {
                throw new IllegalArgumentException("courseId is required when a men's default tee is provided for round " + round.getRoundNumber());
            }
            if (round.getWomenDefaultTeeId() != null && round.getCourseId() == null) {
                throw new IllegalArgumentException("courseId is required when a women's default tee is provided for round " + round.getRoundNumber());
            }
            validatePlannedRoundTee(round.getRoundNumber(), round.getCourseId(), round.getDefaultTeeId(), "men's", "M");
            validatePlannedRoundTee(round.getRoundNumber(), round.getCourseId(), round.getWomenDefaultTeeId(), "women's", "F");
            RoundFormat parsedFormat = null;
            if (round.getFormat() != null && !round.getFormat().isBlank()) {
                parsedFormat = parseRoundFormat(round.getFormat());
            }
            resolveScrambleTeamSize(parsedFormat, round.getScrambleTeamSize());
        }

    }

    private void validatePlannedRoundTee(Integer roundNumber, Long courseId, Long teeId, String label, String gender) {
        if (teeId == null) {
            return;
        }
        CourseTee tee = courseTeeRepository.findById(teeId)
                .orElseThrow(() -> new IllegalArgumentException("Round " + roundNumber + " " + label + " default tee was not found."));
        if (courseId != null && (tee.getCourse() == null || !courseId.equals(tee.getCourse().getId()))) {
            throw new IllegalArgumentException("Round " + roundNumber + " " + label + " default tee does not belong to the selected course.");
        }
        if (!tee.isEligibleForGender(gender)) {
            throw new IllegalArgumentException("Round " + roundNumber + " " + label + " default tee is not eligible for that gender.");
        }

        long holeCount = countConfiguredTeeHoles(tee);
        if (holeCount != 18) {
            throw new IllegalArgumentException(
                    "Round " + roundNumber + " " + label + " default tee has " + holeCount +
                            " holes configured. Open Course Master and enter all 18 holes before starting the trip."
            );
        }
    }

    private long countConfiguredTeeHoles(CourseTee tee) {
        if (tee == null || tee.getId() == null) {
            return 0;
        }

        long regularHoleCount = courseHoleRepository.countByCourseTee_Id(tee.getId());
        if (regularHoleCount == 18) {
            return regularHoleCount;
        }

        long comboHoleCount = courseTeeComboHoleRepository.countByComboTee_Id(tee.getId());
        if (comboHoleCount > 0) {
            return comboHoleCount;
        }

        return regularHoleCount;
    }

    private Integer resolveScrambleTeamSize(RoundFormat format, Integer requestedSize) {
        if (format != RoundFormat.TEAM_SCRAMBLE) {
            return 4;
        }

        int size = requestedSize == null ? 4 : requestedSize;
        if (size < 2 || size > 4) {
            throw new IllegalArgumentException("Scramble team size must be 2, 3, or 4.");
        }
        return size;
    }

    private TripPlannedRoundResponse toPlannedRoundResponse(TripPlannedRound round) {
        TripPlannedRoundResponse response = new TripPlannedRoundResponse();
        response.setPlannedRoundId(round.getId());
        response.setRoundNumber(round.getRoundNumber());
        response.setRoundDate(round.getRoundDate());
        response.setCourseId(round.getCourseId());
        response.setDefaultTeeId(round.getStandardTeeId());
        response.setWomenDefaultTeeId(round.getWomenDefaultTeeId());
        response.setFormat(round.getFormat() != null ? round.getFormat().name() : null);
        response.setScrambleTeamSize(resolveScrambleTeamSize(round.getFormat(), round.getScrambleTeamSize()));
        response.setIncludeInFourDayStandings(Boolean.TRUE.equals(round.getIncludeInFourDayStandings()));

        Course course = null;
        if (round.getCourseId() != null) {
            course = courseRepository.findById(round.getCourseId()).orElse(null);
        }

        CourseTee defaultTee = null;
        if (round.getStandardTeeId() != null) {
            defaultTee = courseTeeRepository.findById(round.getStandardTeeId()).orElse(null);
        }

        CourseTee womenDefaultTee = null;
        if (round.getWomenDefaultTeeId() != null) {
            womenDefaultTee = courseTeeRepository.findById(round.getWomenDefaultTeeId()).orElse(null);
        }

        response.setCourseName(course != null ? course.getName() : null);
        response.setDefaultTeeDisplay(formatCourseTeeDisplay(defaultTee));
        response.setWomenDefaultTeeDisplay(formatWomenCourseTeeDisplay(womenDefaultTee));

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

        return teeName + " (Rating " + tee.getCourseRating() + " / Slope " + tee.getSlope() + ")";
    }

    private String formatWomenCourseTeeDisplay(CourseTee tee) {
        if (tee == null) {
            return null;
        }

        String teeName = tee.getTeeName();
        if (tee.getWomenCourseRating() == null || tee.getWomenSlope() == null) {
            return teeName;
        }

        return teeName + " (Rating " + tee.getWomenCourseRating() + " / Slope " + tee.getWomenSlope() + ")";
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
        return false;
    }

private RoundFormat defaultFormatForRound(int roundNumber) {
        // Path B: new planned rounds should not inherit the old fixed Myrtle sequence.
        // The game is intentionally selected by the user on Trip Round Planning.
        return null;
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
