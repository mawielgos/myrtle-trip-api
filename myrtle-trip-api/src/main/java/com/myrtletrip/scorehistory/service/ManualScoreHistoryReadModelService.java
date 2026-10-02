package com.myrtletrip.scorehistory.service;

import com.myrtletrip.scorehistory.dto.DbScoreHistoryImportCandidateResponse;
import com.myrtletrip.scorehistory.dto.ManualScoreHistoryEntryResponse;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
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
public class ManualScoreHistoryReadModelService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;

    public ManualScoreHistoryReadModelService(TripRepository tripRepository,
                                              TripPlayerRepository tripPlayerRepository,
                                              ScoreHistoryEntryRepository scoreHistoryEntryRepository) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<ManualScoreHistoryEntryResponse> getManualEntries(Long tripId) {
        Trip trip = findTrip(tripId);

        List<ScoreHistoryEntry> entries =
                scoreHistoryEntryRepository.findByHandicapGroupCodeAndSourceTypeOrderByPlayer_DisplayNameAscPostingOrderAscIdAsc(
                        trip.getTripCode(),
                        ManualScoreHistoryService.SOURCE_TYPE_GHIN_FROZEN
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

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
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

    private BigDecimal roundOneDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(1, RoundingMode.HALF_UP);
    }
}
