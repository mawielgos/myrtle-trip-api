package com.myrtletrip.scorehistory.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.scorehistory.dto.DbScoreHistoryImportCandidateResponse;
import com.myrtletrip.scorehistory.dto.ManualScoreHistoryEntryResponse;
import com.myrtletrip.scorehistory.dto.SaveManualScoreHistoryEntryRequest;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class ManualScoreHistoryService {

    // Manual uploads on this page intentionally create GHIN_FROZEN-style rows so the
    // existing GHIN trip-index calculation can use them exactly like imported GHIN rows.
    public static final String SOURCE_TYPE_GHIN_FROZEN = "GHIN_FROZEN";
    private static final int MAX_MANUAL_SCORES_PER_PLAYER = 20;

    private final TripRepository tripRepository;
    private final PlayerRepository playerRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final RoundTeamRepository roundTeamRepository;

    public ManualScoreHistoryService(TripRepository tripRepository,
                                     PlayerRepository playerRepository,
                                     TripPlayerRepository tripPlayerRepository,
                                     ScoreHistoryEntryRepository scoreHistoryEntryRepository,
                                     ScorecardRepository scorecardRepository,
                                     HoleScoreRepository holeScoreRepository,
                                     TeamHoleScoreRepository teamHoleScoreRepository,
                                     RoundTeamRepository roundTeamRepository) {
        this.tripRepository = tripRepository;
        this.playerRepository = playerRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.roundTeamRepository = roundTeamRepository;
    }

    @Transactional(readOnly = true)
    public List<ManualScoreHistoryEntryResponse> getManualEntries(Long tripId) {
        Trip trip = findTrip(tripId);

        List<ScoreHistoryEntry> entries =
                scoreHistoryEntryRepository.findByHandicapGroupCodeAndSourceTypeOrderByPlayer_DisplayNameAscPostingOrderAscIdAsc(
                        trip.getTripCode(),
                        SOURCE_TYPE_GHIN_FROZEN
                );

        List<ManualScoreHistoryEntryResponse> responses = new ArrayList<ManualScoreHistoryEntryResponse>();
        for (ScoreHistoryEntry entry : entries) {
            responses.add(toResponse(entry));
        }
        return responses;
    }


    @Transactional(readOnly = true)
    public List<DbScoreHistoryImportCandidateResponse> getImportableDbScoreHistory(Long tripId) {
        Trip trip = findTrip(tripId);

        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTrip_Id(trip.getId());
        List<Long> playerIds = new ArrayList<Long>();
        for (TripPlayer tripPlayer : tripPlayers) {
            if (tripPlayer.getPlayer() != null && tripPlayer.getPlayer().getId() != null) {
                playerIds.add(tripPlayer.getPlayer().getId());
            }
        }

        List<DbScoreHistoryImportCandidateResponse> responses = new ArrayList<DbScoreHistoryImportCandidateResponse>();
        if (playerIds.isEmpty()) {
            return responses;
        }

        List<ScoreHistoryEntry> entries = scoreHistoryEntryRepository.findImportablePriorTripRoundEntries(
                trip.getId(),
                playerIds,
                RoundScoreHistorySyncService.SOURCE_TRIP_ROUND
        );

        for (ScoreHistoryEntry entry : entries) {
            responses.add(toDbImportCandidateResponse(entry));
        }
        return responses;
    }

    @Transactional
    public ManualScoreHistoryEntryResponse createManualEntry(Long tripId, SaveManualScoreHistoryEntryRequest request) {
        Trip trip = findTrip(tripId);
        assertTripSetupEditable(trip);
        validateRequest(request);

        Player player = resolveTripPlayer(trip, request.getPlayerId());
        validateCreateLimit(trip, player.getId(), 1);

        Integer postingOrder = request.getPostingOrder();
        if (postingOrder == null) {
            postingOrder = getNextPostingOrder(trip, player.getId());
        } else {
            shiftPostingOrdersAtOrAfter(trip, player.getId(), postingOrder);
        }

        ScoreHistoryEntry saved = createEntryForTrip(trip, request, player, postingOrder);
        normalizePostingOrders(trip, player.getId());
        return toResponse(saved);
    }

    @Transactional
    public List<ManualScoreHistoryEntryResponse> createManualEntries(Long tripId, List<SaveManualScoreHistoryEntryRequest> requests) {
        Trip trip = findTrip(tripId);
        assertTripSetupEditable(trip);

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one score history entry is required.");
        }

        validateBulkPlayerCounts(trip, requests);

        List<Long> affectedPlayerIds = new ArrayList<Long>();
        List<ManualScoreHistoryEntryResponse> responses = new ArrayList<ManualScoreHistoryEntryResponse>();

        for (SaveManualScoreHistoryEntryRequest request : requests) {
            validateRequest(request);
            Player player = resolveTripPlayer(trip, request.getPlayerId());
            addAffectedPlayerId(affectedPlayerIds, player.getId());

            Integer postingOrder = request.getPostingOrder();
            if (postingOrder == null) {
                postingOrder = getNextPostingOrder(trip, player.getId());
            } else {
                shiftPostingOrdersAtOrAfter(trip, player.getId(), postingOrder);
            }

            ScoreHistoryEntry saved = createEntryForTrip(trip, request, player, postingOrder);
            responses.add(toResponse(saved));
        }

        for (Long playerId : affectedPlayerIds) {
            normalizePostingOrders(trip, playerId);
        }

        return responses;
    }

    private ScoreHistoryEntry createEntryForTrip(Trip trip,
                                                 SaveManualScoreHistoryEntryRequest request,
                                                 Player player,
                                                 Integer postingOrder) {
        ScoreHistoryEntry entry = new ScoreHistoryEntry();
        entry.setPlayer(player);
        entry.setRound(null);
        entry.setCourse(null);
        entry.setScoreDate(request.getScoreDate());
        entry.setCourseName(normalizeCourseName(request.getCourseName()));
        entry.setCourseRating(roundOneDecimal(request.getCourseRating()));
        entry.setSlope(request.getSlope());
        entry.setGrossScore(request.getGrossScore());

        Integer adjustedGrossScore = request.getAdjustedGrossScore();
        if (adjustedGrossScore == null) {
            adjustedGrossScore = request.getGrossScore();
        }
        entry.setAdjustedGrossScore(adjustedGrossScore);

        BigDecimal differential = request.getDifferential();
        if (differential == null) {
            differential = calculateDifferential(adjustedGrossScore, request.getCourseRating(), request.getSlope());
        }
        entry.setDifferential(roundOneDecimal(differential));

        entry.setSourceType(SOURCE_TYPE_GHIN_FROZEN);
        entry.setIncludedInMyrtleCalc(request.getIncludedInMyrtleCalc() == null ? Boolean.TRUE : request.getIncludedInMyrtleCalc());
        entry.setHandicapGroupCode(trip.getTripCode());
        entry.setPostingOrder(postingOrder);
        entry.setScoreType("A");
        entry.setHolesPlayed(request.getHolesPlayed() == null ? 18 : request.getHolesPlayed());
        entry.setManualDifferentialRequired(Boolean.FALSE);

        return scoreHistoryEntryRepository.save(entry);
    }

    @Transactional
    public ManualScoreHistoryEntryResponse updateManualEntry(Long tripId,
                                                            Long scoreHistoryEntryId,
                                                            SaveManualScoreHistoryEntryRequest request) {
        Trip trip = findTrip(tripId);
        assertTripSetupEditable(trip);
        validateRequest(request);

        ScoreHistoryEntry entry = scoreHistoryEntryRepository
                .findByIdAndHandicapGroupCodeAndSourceType(scoreHistoryEntryId, trip.getTripCode(), SOURCE_TYPE_GHIN_FROZEN)
                .orElseThrow(() -> new IllegalArgumentException("Score history entry not found: " + scoreHistoryEntryId));

        Long originalPlayerId = entry.getPlayer() == null ? null : entry.getPlayer().getId();
        Player player = resolveTripPlayer(trip, request.getPlayerId());
        validateUpdateLimit(trip, player.getId(), scoreHistoryEntryId);

        Integer requestedPostingOrder = request.getPostingOrder();
        if (requestedPostingOrder != null) {
            validateDuplicatePostingOrderForUpdate(trip, player.getId(), scoreHistoryEntryId, requestedPostingOrder);
        }

        entry.setPlayer(player);
        entry.setScoreDate(request.getScoreDate());
        entry.setCourseName(normalizeCourseName(request.getCourseName()));
        entry.setCourseRating(roundOneDecimal(request.getCourseRating()));
        entry.setSlope(request.getSlope());
        entry.setGrossScore(request.getGrossScore());

        Integer adjustedGrossScore = request.getAdjustedGrossScore();
        if (adjustedGrossScore == null) {
            adjustedGrossScore = request.getGrossScore();
        }
        entry.setAdjustedGrossScore(adjustedGrossScore);

        BigDecimal differential = request.getDifferential();
        if (differential == null) {
            differential = calculateDifferential(adjustedGrossScore, request.getCourseRating(), request.getSlope());
        }
        entry.setDifferential(roundOneDecimal(differential));

        entry.setIncludedInMyrtleCalc(request.getIncludedInMyrtleCalc() == null ? Boolean.TRUE : request.getIncludedInMyrtleCalc());
        entry.setHolesPlayed(request.getHolesPlayed() == null ? 18 : request.getHolesPlayed());
        entry.setPostingOrder(requestedPostingOrder);
        entry.setManualDifferentialRequired(Boolean.FALSE);

        ScoreHistoryEntry saved = scoreHistoryEntryRepository.save(entry);

        if (originalPlayerId != null && !originalPlayerId.equals(player.getId())) {
            normalizePostingOrders(trip, originalPlayerId);
        }
        normalizePostingOrders(trip, player.getId());

        return toResponse(saved);
    }

    @Transactional
    public void deleteManualEntry(Long tripId, Long scoreHistoryEntryId) {
        Trip trip = findTrip(tripId);
        assertTripSetupEditable(trip);

        ScoreHistoryEntry entry = scoreHistoryEntryRepository
                .findByIdAndHandicapGroupCodeAndSourceType(scoreHistoryEntryId, trip.getTripCode(), SOURCE_TYPE_GHIN_FROZEN)
                .orElseThrow(() -> new IllegalArgumentException("Score history entry not found: " + scoreHistoryEntryId));

        Long playerId = entry.getPlayer() == null ? null : entry.getPlayer().getId();
        scoreHistoryEntryRepository.delete(entry);
        scoreHistoryEntryRepository.flush();

        if (playerId != null) {
            normalizePostingOrders(trip, playerId);
        }
    }

    private Player resolveTripPlayer(Trip trip, Long playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player is required.");
        }

        tripPlayerRepository.findByTrip_IdAndPlayer_Id(trip.getId(), playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player is not on this trip: " + playerId));

        return playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));
    }

    private void validateCreateLimit(Trip trip, Long playerId, int newRows) {
        long existing = scoreHistoryEntryRepository.countByPlayer_IdAndHandicapGroupCodeAndSourceType(
                playerId,
                trip.getTripCode(),
                SOURCE_TYPE_GHIN_FROZEN
        );

        if (existing + newRows > MAX_MANUAL_SCORES_PER_PLAYER) {
            throw new IllegalArgumentException("Cannot commit more than 20 score history rows for a player in one trip.");
        }
    }

    private void validateUpdateLimit(Trip trip, Long playerId, Long currentEntryId) {
        long existing = scoreHistoryEntryRepository.countByPlayer_IdAndHandicapGroupCodeAndSourceTypeAndIdNot(
                playerId,
                trip.getTripCode(),
                SOURCE_TYPE_GHIN_FROZEN,
                currentEntryId
        );

        if (existing + 1 > MAX_MANUAL_SCORES_PER_PLAYER) {
            throw new IllegalArgumentException("Cannot commit more than 20 score history rows for a player in one trip.");
        }
    }

    private void validateBulkPlayerCounts(Trip trip, List<SaveManualScoreHistoryEntryRequest> requests) {
        List<Long> playerIds = new ArrayList<Long>();
        List<Integer> counts = new ArrayList<Integer>();

        for (SaveManualScoreHistoryEntryRequest request : requests) {
            validateRequest(request);
            Player player = resolveTripPlayer(trip, request.getPlayerId());

            int index = findPlayerIndex(playerIds, player.getId());
            if (index < 0) {
                playerIds.add(player.getId());
                counts.add(Integer.valueOf(1));
            } else {
                counts.set(index, Integer.valueOf(counts.get(index).intValue() + 1));
            }
        }

        for (int i = 0; i < playerIds.size(); i++) {
            validateCreateLimit(trip, playerIds.get(i), counts.get(i).intValue());
        }
    }

    private void validateDuplicatePostingOrderForUpdate(Trip trip, Long playerId, Long currentEntryId, Integer postingOrder) {
        List<ScoreHistoryEntry> entries = scoreHistoryEntryRepository
                .findByPlayer_IdAndHandicapGroupCodeAndSourceTypeAndIdNotOrderByPostingOrderAscIdAsc(
                        playerId,
                        trip.getTripCode(),
                        SOURCE_TYPE_GHIN_FROZEN,
                        currentEntryId
                );

        for (ScoreHistoryEntry entry : entries) {
            if (entry.getPostingOrder() != null && entry.getPostingOrder().equals(postingOrder)) {
                throw new IllegalArgumentException("Another score history row already uses sequence " + postingOrder + " for this player.");
            }
        }
    }

    private Integer getNextPostingOrder(Trip trip, Long playerId) {
        List<ScoreHistoryEntry> entries = scoreHistoryEntryRepository
                .findByPlayer_IdAndHandicapGroupCodeAndSourceTypeOrderByPostingOrderAscIdAsc(
                        playerId,
                        trip.getTripCode(),
                        SOURCE_TYPE_GHIN_FROZEN
                );

        int maxPostingOrder = 0;
        for (ScoreHistoryEntry entry : entries) {
            if (entry.getPostingOrder() != null && entry.getPostingOrder().intValue() > maxPostingOrder) {
                maxPostingOrder = entry.getPostingOrder().intValue();
            }
        }
        return Integer.valueOf(maxPostingOrder + 1);
    }

    private void shiftPostingOrdersAtOrAfter(Trip trip, Long playerId, Integer postingOrder) {
        List<ScoreHistoryEntry> entries = scoreHistoryEntryRepository
                .findByPlayer_IdAndHandicapGroupCodeAndSourceTypeOrderByPostingOrderAscIdAsc(
                        playerId,
                        trip.getTripCode(),
                        SOURCE_TYPE_GHIN_FROZEN
                );

        for (int i = entries.size() - 1; i >= 0; i--) {
            ScoreHistoryEntry entry = entries.get(i);
            Integer existingPostingOrder = entry.getPostingOrder();
            if (existingPostingOrder != null && existingPostingOrder.intValue() >= postingOrder.intValue()) {
                entry.setPostingOrder(Integer.valueOf(existingPostingOrder.intValue() + 1));
                scoreHistoryEntryRepository.save(entry);
            }
        }
    }

    private void normalizePostingOrders(Trip trip, Long playerId) {
        List<ScoreHistoryEntry> entries = scoreHistoryEntryRepository
                .findByPlayer_IdAndHandicapGroupCodeAndSourceTypeOrderByPostingOrderAscIdAsc(
                        playerId,
                        trip.getTripCode(),
                        SOURCE_TYPE_GHIN_FROZEN
                );

        int nextPostingOrder = 1;
        for (ScoreHistoryEntry entry : entries) {
            entry.setPostingOrder(Integer.valueOf(nextPostingOrder));
            scoreHistoryEntryRepository.save(entry);
            nextPostingOrder++;
        }
    }

    private void addAffectedPlayerId(List<Long> affectedPlayerIds, Long playerId) {
        if (findPlayerIndex(affectedPlayerIds, playerId) < 0) {
            affectedPlayerIds.add(playerId);
        }
    }

    private int findPlayerIndex(List<Long> playerIds, Long playerId) {
        for (int i = 0; i < playerIds.size(); i++) {
            if (playerIds.get(i).equals(playerId)) {
                return i;
            }
        }
        return -1;
    }

    private void assertTripSetupEditable(Trip trip) {
        if (hasScoringStarted(trip)) {
            throw new IllegalStateException("Manual score history cannot be changed after scoring has started for this trip.");
        }
    }

    private boolean hasScoringStarted(Trip trip) {
        Long tripId = trip.getId();

        if (scorecardRepository.countByRound_Trip_IdAndGrossScoreIsNotNull(tripId) > 0) {
            return true;
        }
        if (scorecardRepository.countByRound_Trip_IdAndAdjustedGrossScoreIsNotNull(tripId) > 0) {
            return true;
        }
        if (scorecardRepository.countByRound_Trip_IdAndNetScoreIsNotNull(tripId) > 0) {
            return true;
        }
        if (holeScoreRepository.countByScorecard_Round_Trip_IdAndStrokesIsNotNull(tripId) > 0) {
            return true;
        }
        if (teamHoleScoreRepository.countByRoundTeam_Round_Trip_Id(tripId) > 0) {
            return true;
        }
        if (roundTeamRepository.countByRound_Trip_IdAndScrambleTotalScoreIsNotNull(tripId) > 0) {
            return true;
        }

        return false;
    }

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
    }

    private void validateRequest(SaveManualScoreHistoryEntryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Score history request is required.");
        }
        if (request.getPlayerId() == null) {
            throw new IllegalArgumentException("Player is required.");
        }
        if (request.getScoreDate() == null) {
            throw new IllegalArgumentException("Score month/year is required.");
        }
        if (request.getCourseRating() == null) {
            throw new IllegalArgumentException("Course rating is required.");
        }
        if (request.getSlope() == null || request.getSlope() <= 0) {
            throw new IllegalArgumentException("Slope must be greater than zero.");
        }
        if (request.getGrossScore() == null || request.getGrossScore() <= 0) {
            throw new IllegalArgumentException("Gross score must be greater than zero.");
        }
        if (request.getAdjustedGrossScore() != null && request.getAdjustedGrossScore() <= 0) {
            throw new IllegalArgumentException("Adjusted gross score must be greater than zero.");
        }
        if (request.getHolesPlayed() != null && request.getHolesPlayed() != 9 && request.getHolesPlayed() != 18) {
            throw new IllegalArgumentException("Holes played must be 9 or 18.");
        }
        if (request.getPostingOrder() != null && request.getPostingOrder() <= 0) {
            throw new IllegalArgumentException("Posting order must be greater than zero.");
        }
    }

    private String normalizeCourseName(String courseName) {
        if (courseName == null || courseName.trim().isEmpty()) {
            return "Unknown Course";
        }
        return courseName.trim();
    }

    private BigDecimal calculateDifferential(Integer adjustedGrossScore, BigDecimal courseRating, Integer slope) {
        BigDecimal adjusted = BigDecimal.valueOf(adjustedGrossScore.longValue());
        BigDecimal slopeValue = BigDecimal.valueOf(slope.longValue());

        return adjusted
                .subtract(courseRating)
                .multiply(BigDecimal.valueOf(113L))
                .divide(slopeValue, 3, RoundingMode.HALF_UP);
    }

    private BigDecimal roundOneDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(1, RoundingMode.HALF_UP);
    }


    private DbScoreHistoryImportCandidateResponse toDbImportCandidateResponse(ScoreHistoryEntry entry) {
        DbScoreHistoryImportCandidateResponse response = new DbScoreHistoryImportCandidateResponse();
        response.setSourceScoreHistoryEntryId(entry.getId());

        if (entry.getPlayer() != null) {
            response.setPlayerId(entry.getPlayer().getId());
            response.setPlayerName(entry.getPlayer().getDisplayName());
        }

        if (entry.getRound() != null) {
            response.setSourceRoundNumber(entry.getRound().getRoundNumber());
            if (entry.getRound().getTrip() != null) {
                response.setSourceTripId(entry.getRound().getTrip().getId());
                response.setSourceTripName(entry.getRound().getTrip().getName());
                response.setSourceTripCode(entry.getRound().getTrip().getTripCode());
            }
        }

        response.setScoreDate(entry.getScoreDate());
        response.setCourseName(entry.getCourseName());
        response.setCourseRating(roundOneDecimal(entry.getCourseRating()));
        response.setSlope(entry.getSlope());
        response.setGrossScore(entry.getGrossScore());
        response.setAdjustedGrossScore(entry.getAdjustedGrossScore());
        response.setDifferential(roundOneDecimal(entry.getDifferential()));
        response.setIncludedInMyrtleCalc(entry.getIncludedInMyrtleCalc());
        response.setHolesPlayed(entry.getHolesPlayed());

        return response;
    }

    private ManualScoreHistoryEntryResponse toResponse(ScoreHistoryEntry entry) {
        ManualScoreHistoryEntryResponse response = new ManualScoreHistoryEntryResponse();
        response.setScoreHistoryEntryId(entry.getId());

        if (entry.getPlayer() != null) {
            response.setPlayerId(entry.getPlayer().getId());
            response.setPlayerName(entry.getPlayer().getDisplayName());
        }

        response.setScoreDate(entry.getScoreDate());
        response.setCourseName(entry.getCourseName());
        response.setCourseRating(entry.getCourseRating());
        response.setSlope(entry.getSlope());
        response.setGrossScore(entry.getGrossScore());
        response.setAdjustedGrossScore(entry.getAdjustedGrossScore());
        response.setDifferential(entry.getDifferential());
        response.setIncludedInMyrtleCalc(entry.getIncludedInMyrtleCalc());
        response.setHolesPlayed(entry.getHolesPlayed());
        response.setPostingOrder(entry.getPostingOrder());

        return response;
    }
}
