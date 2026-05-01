package com.myrtletrip.scorehistory.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.scorehistory.dto.ManualScoreHistoryEntryResponse;
import com.myrtletrip.scorehistory.dto.SaveManualScoreHistoryEntryRequest;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.entity.Trip;
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

    private final TripRepository tripRepository;
    private final PlayerRepository playerRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;

    public ManualScoreHistoryService(TripRepository tripRepository,
                                     PlayerRepository playerRepository,
                                     ScoreHistoryEntryRepository scoreHistoryEntryRepository) {
        this.tripRepository = tripRepository;
        this.playerRepository = playerRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
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

    @Transactional
    public ManualScoreHistoryEntryResponse createManualEntry(Long tripId, SaveManualScoreHistoryEntryRequest request) {
        Trip trip = findTrip(tripId);
        ScoreHistoryEntry saved = createEntryForTrip(trip, request, request == null ? null : request.getPostingOrder());
        return toResponse(saved);
    }

    @Transactional
    public List<ManualScoreHistoryEntryResponse> createManualEntries(Long tripId, List<SaveManualScoreHistoryEntryRequest> requests) {
        Trip trip = findTrip(tripId);

        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one score history entry is required.");
        }

        List<ManualScoreHistoryEntryResponse> responses = new ArrayList<ManualScoreHistoryEntryResponse>();
        for (int i = 0; i < requests.size(); i++) {
            SaveManualScoreHistoryEntryRequest request = requests.get(i);
            Integer postingOrder = request.getPostingOrder();
            if (postingOrder == null) {
                postingOrder = Integer.valueOf(i + 1);
            }
            ScoreHistoryEntry saved = createEntryForTrip(trip, request, postingOrder);
            responses.add(toResponse(saved));
        }
        return responses;
    }

    private ScoreHistoryEntry createEntryForTrip(Trip trip, SaveManualScoreHistoryEntryRequest request, Integer postingOrder) {
        validateRequest(request);

        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + request.getPlayerId()));

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
        entry.setUsedAlternateTee(Boolean.FALSE);
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
        validateRequest(request);

        ScoreHistoryEntry entry = scoreHistoryEntryRepository
                .findByIdAndHandicapGroupCodeAndSourceType(scoreHistoryEntryId, trip.getTripCode(), SOURCE_TYPE_GHIN_FROZEN)
                .orElseThrow(() -> new IllegalArgumentException("Score history entry not found: " + scoreHistoryEntryId));

        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + request.getPlayerId()));

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
        entry.setPostingOrder(request.getPostingOrder());
        entry.setManualDifferentialRequired(Boolean.FALSE);

        ScoreHistoryEntry saved = scoreHistoryEntryRepository.save(entry);
        return toResponse(saved);
    }

    @Transactional
    public void deleteManualEntry(Long tripId, Long scoreHistoryEntryId) {
        Trip trip = findTrip(tripId);

        ScoreHistoryEntry entry = scoreHistoryEntryRepository
                .findByIdAndHandicapGroupCodeAndSourceType(scoreHistoryEntryId, trip.getTripCode(), SOURCE_TYPE_GHIN_FROZEN)
                .orElseThrow(() -> new IllegalArgumentException("Score history entry not found: " + scoreHistoryEntryId));

        scoreHistoryEntryRepository.delete(entry);
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
            return null;
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
