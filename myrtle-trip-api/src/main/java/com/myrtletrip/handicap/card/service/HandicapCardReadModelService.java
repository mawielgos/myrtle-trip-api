package com.myrtletrip.handicap.card.service;

import com.myrtletrip.handicap.card.dto.HandicapCardListResponse;
import com.myrtletrip.handicap.card.dto.HandicapCardPlayerResponse;
import com.myrtletrip.handicap.card.dto.HandicapCardPlayerSummaryResponse;
import com.myrtletrip.handicap.card.dto.HandicapCardScoreResponse;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.model.TripHandicapMethod;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class HandicapCardReadModelService {

    private static final String HANDICAP_METHOD_DB_SCORE_HISTORY = "DB_SCORE_HISTORY";
    private static final String HANDICAP_METHOD_LEGACY_MYRTLE_BEACH = "MYRTLE_BEACH";

    private static final String SECTION_PENDING = "PENDING";
    private static final String SECTION_CURRENT_WINDOW = "CURRENT_WINDOW";

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final HandicapCardCalculationService calculationService;

    public HandicapCardReadModelService(TripRepository tripRepository,
                                        TripPlayerRepository tripPlayerRepository,
                                        HandicapCardCalculationService calculationService) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.calculationService = calculationService;
    }

    public HandicapCardListResponse getTripHandicapCards(Long tripId, LocalDate asOfDate) {
        Trip trip = loadTrip(tripId);
        LocalDate effectiveAsOfDate = effectiveAsOfDate(asOfDate);
        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(tripId);

        HandicapCardListResponse response = new HandicapCardListResponse();
        response.setTripId(trip.getId());
        response.setTripName(trip.getName());
        response.setTripCode(trip.getTripCode());
        response.setTripYear(trip.getTripYear());
        response.setAsOfDate(effectiveAsOfDate);

        List<HandicapCardPlayerSummaryResponse> playerResponses = new ArrayList<>();
        for (TripPlayer tripPlayer : tripPlayers) {
            if (tripPlayer == null || tripPlayer.getPlayer() == null) {
                continue;
            }
            HandicapCardCalculationService.HandicapCalculation calculation =
                    calculationService.calculateForTripPlayer(trip, tripPlayer, effectiveAsOfDate);
            playerResponses.add(toSummaryResponse(trip, tripPlayer, calculation));
        }
        response.setPlayers(playerResponses);

        return response;
    }

    public HandicapCardPlayerResponse getPlayerHandicapCard(Long tripId, Long playerId, LocalDate asOfDate) {
        if (playerId == null) {
            throw new IllegalArgumentException("playerId is required");
        }

        Trip trip = loadTrip(tripId);
        LocalDate effectiveAsOfDate = effectiveAsOfDate(asOfDate);
        TripPlayer tripPlayer = null;
        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(tripId);
        for (TripPlayer candidate : tripPlayers) {
            if (candidate == null || candidate.getPlayer() == null || candidate.getPlayer().getId() == null) {
                continue;
            }
            if (candidate.getPlayer().getId().equals(playerId)) {
                tripPlayer = candidate;
                break;
            }
        }

        if (tripPlayer == null) {
            throw new IllegalArgumentException("Player " + playerId + " is not on trip " + tripId);
        }

        HandicapCardCalculationService.HandicapCalculation calculation =
                calculationService.calculateForTripPlayer(trip, tripPlayer, effectiveAsOfDate);

        HandicapCardPlayerResponse response = new HandicapCardPlayerResponse();
        response.setTripId(trip.getId());
        response.setTripName(trip.getName());
        response.setTripCode(trip.getTripCode());
        response.setTripYear(trip.getTripYear());
        response.setAsOfDate(effectiveAsOfDate);
        response.setPlayerId(tripPlayer.getPlayer().getId());
        response.setPlayerName(tripPlayer.getPlayer().getDisplayName());
        response.setHandicapMethod(resolveDisplayHandicapMethod(trip, tripPlayer));
        response.setTripIndex(calculation.tripIndex);
        response.setPendingTripIndex(calculation.pendingTripIndex);
        response.setPendingScoreCount(calculation.pendingEntryIds.size());
        response.setEligibleScoreCount(calculation.eligibleScoreCount);
        response.setWindowScoreCount(calculation.windowEntries.size());
        response.setUsedScoreCount(calculation.usedEntryIds.size());
        response.setCalculationLabel(calculation.calculationLabel);
        response.setPendingCalculationLabel(calculation.pendingCalculationLabel);

        List<HandicapCardScoreResponse> scoreResponses = new ArrayList<>();
        int displaySortOrder = 1;
        for (ScoreHistoryEntry entry : calculation.displayEntries) {
            HandicapCardScoreResponse scoreResponse = toScoreResponse(entry, calculation);
            scoreResponse.setDisplaySortOrder(displaySortOrder);
            scoreResponses.add(scoreResponse);
            displaySortOrder++;
        }
        response.setScores(scoreResponses);

        return response;
    }

    private HandicapCardPlayerSummaryResponse toSummaryResponse(
            Trip trip,
            TripPlayer tripPlayer,
            HandicapCardCalculationService.HandicapCalculation calculation) {
        HandicapCardPlayerSummaryResponse response = new HandicapCardPlayerSummaryResponse();
        Player player = tripPlayer.getPlayer();
        response.setPlayerId(player.getId());
        response.setPlayerName(player.getDisplayName());
        response.setDisplayOrder(tripPlayer.getDisplayOrder());
        response.setHandicapMethod(resolveDisplayHandicapMethod(trip, tripPlayer));
        response.setTripIndex(calculation.tripIndex);
        response.setEligibleScoreCount(calculation.eligibleScoreCount);
        response.setWindowScoreCount(calculation.windowEntries.size());
        response.setUsedScoreCount(calculation.usedEntryIds.size());

        if (calculation.tripIndex == null) {
            response.setStatusCode("INCOMPLETE");
            response.setStatusLabel("Not enough valid differentials");
        } else if (calculation.manualReviewCount > 0) {
            response.setStatusCode("REVIEW");
            response.setStatusLabel(calculation.manualReviewCount + " score(s) need review");
        } else {
            response.setStatusCode("READY");
            response.setStatusLabel("Ready");
        }

        return response;
    }

    private HandicapCardScoreResponse toScoreResponse(
            ScoreHistoryEntry entry,
            HandicapCardCalculationService.HandicapCalculation calculation) {
        HandicapCardScoreResponse response = new HandicapCardScoreResponse();
        response.setScoreHistoryEntryId(entry.getId());
        response.setScoreDate(entry.getScoreDate());
        response.setCourseName(entry.getCourseName());
        response.setCourseRating(entry.getCourseRating());
        response.setSlope(entry.getSlope());
        response.setGrossScore(entry.getGrossScore());
        response.setAdjustedGrossScore(entry.getAdjustedGrossScore());
        response.setDifferential(entry.getDifferential());
        response.setSourceType(entry.getSourceType());
        response.setScoreType(entry.getScoreType());
        response.setHolesPlayed(entry.getHolesPlayed());
        response.setPostingOrder(entry.getPostingOrder());
        response.setManualDifferentialRequired(Boolean.TRUE.equals(entry.getManualDifferentialRequired()));
        response.setPendingForCalculationDate(calculation.pendingEntryIds.contains(entry.getId()));
        response.setUsedInPendingIndex(calculation.pendingUsedEntryIds.contains(entry.getId()));
        response.setEligibleForWindow(calculation.windowEntryIds.contains(entry.getId()));
        response.setUsedInIndex(calculation.usedEntryIds.contains(entry.getId()));
        response.setScoreSection(calculation.pendingEntryIds.contains(entry.getId()) ? SECTION_PENDING : SECTION_CURRENT_WINDOW);
        response.setExclusionReason(resolveExclusionReason(entry, calculation));
        return response;
    }

    private String resolveExclusionReason(
            ScoreHistoryEntry entry,
            HandicapCardCalculationService.HandicapCalculation calculation) {
        if (entry == null) {
            return null;
        }
        if (calculation.pendingEntryIds.contains(entry.getId())) {
            if (entry.getDifferential() == null) {
                return "Pending score has no differential";
            }
            if (Boolean.TRUE.equals(entry.getManualDifferentialRequired())) {
                return "Pending score needs manual differential review";
            }
            return "Posted on calculation date; pending next index";
        }
        if (entry.getDifferential() == null) {
            return "No differential";
        }
        if (Boolean.TRUE.equals(entry.getManualDifferentialRequired())) {
            return "Manual differential review required";
        }
        if (!calculation.windowEntryIds.contains(entry.getId())) {
            return "Outside calculation window";
        }
        if (!calculation.usedEntryIds.contains(entry.getId())) {
            return "Not one of the lowest used differentials";
        }
        return null;
    }

    private String resolveDisplayHandicapMethod(Trip trip, TripPlayer tripPlayer) {
        if (TripHandicapMethod.FROZEN_GHIN_INDEX.equals(trip.getHandicapMethod())) {
            return "FROZEN_GHIN_INDEX";
        }
        if (tripPlayer == null || tripPlayer.getPlayer() == null) {
            return null;
        }
        return normalize(tripPlayer.getPlayer().getHandicapMethod());
    }

    private LocalDate effectiveAsOfDate(LocalDate asOfDate) {
        return asOfDate == null ? LocalDate.now() : asOfDate;
    }

    private Trip loadTrip(Long tripId) {
        if (tripId == null) {
            throw new IllegalArgumentException("tripId is required");
        }
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.US);
        if (HANDICAP_METHOD_LEGACY_MYRTLE_BEACH.equals(normalized)) {
            return HANDICAP_METHOD_DB_SCORE_HISTORY;
        }
        return normalized;
    }
}
