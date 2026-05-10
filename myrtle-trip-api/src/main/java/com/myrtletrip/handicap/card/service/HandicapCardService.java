package com.myrtletrip.handicap.card.service;

import com.myrtletrip.handicap.card.dto.HandicapCardListResponse;
import com.myrtletrip.handicap.card.dto.HandicapCardPlayerResponse;
import com.myrtletrip.handicap.card.dto.HandicapCardPlayerSummaryResponse;
import com.myrtletrip.handicap.card.dto.HandicapCardScoreResponse;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.model.TripHandicapMethod;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class HandicapCardService {

    private static final String HANDICAP_METHOD_GHIN = "GHIN";
    private static final String HANDICAP_METHOD_DB_SCORE_HISTORY = "DB_SCORE_HISTORY";
    private static final String HANDICAP_METHOD_LEGACY_MYRTLE_BEACH = "MYRTLE_BEACH";

    private static final String SOURCE_GHIN_FROZEN = "GHIN_FROZEN";
    private static final String SOURCE_DB_HISTORY_FROZEN = "DB_HISTORY_FROZEN";
    private static final String SOURCE_TRIP_ROUND = "TRIP_ROUND";

    private static final String SECTION_PENDING = "PENDING";
    private static final String SECTION_CURRENT_WINDOW = "CURRENT_WINDOW";

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;

    public HandicapCardService(TripRepository tripRepository,
                               TripPlayerRepository tripPlayerRepository,
                               ScoreHistoryEntryRepository scoreHistoryEntryRepository) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
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
            HandicapCalculation calculation = calculateForTripPlayer(trip, tripPlayer, effectiveAsOfDate);
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

        HandicapCalculation calculation = calculateForTripPlayer(trip, tripPlayer, effectiveAsOfDate);

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

    private HandicapCardPlayerSummaryResponse toSummaryResponse(Trip trip, TripPlayer tripPlayer, HandicapCalculation calculation) {
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

    private HandicapCardScoreResponse toScoreResponse(ScoreHistoryEntry entry, HandicapCalculation calculation) {
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

    private String resolveExclusionReason(ScoreHistoryEntry entry, HandicapCalculation calculation) {
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

    private HandicapCalculation calculateForTripPlayer(Trip trip, TripPlayer tripPlayer, LocalDate asOfDate) {
        HandicapCalculation calculation = new HandicapCalculation();
        calculation.asOfDate = asOfDate;

        if (TripHandicapMethod.FROZEN_GHIN_INDEX.equals(trip.getHandicapMethod())) {
            calculation.tripIndex = tripPlayer.getFrozenHandicapIndex();
            calculation.pendingTripIndex = tripPlayer.getFrozenHandicapIndex();
            calculation.eligibleScoreCount = 0;
            calculation.calculationLabel = "Frozen GHIN Index: trip-level starting index used for every round.";
            calculation.pendingCalculationLabel = calculation.calculationLabel;
            return calculation;
        }

        return calculateForPlayer(tripPlayer.getPlayer(), trip.getTripCode(), asOfDate);
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

    private HandicapCalculation calculateForPlayer(Player player, String handicapGroupCode, LocalDate asOfDate) {
        HandicapCalculation calculation = new HandicapCalculation();
        calculation.asOfDate = asOfDate;

        String method = normalize(player.getHandicapMethod());
        if (HANDICAP_METHOD_GHIN.equals(method)) {
            calculateGhin(player, handicapGroupCode, calculation);
        } else if (isDbScoreHistoryMethod(method)) {
            calculateMyrtleBeach(player, handicapGroupCode, calculation);
        } else {
            calculation.calculationLabel = "Unsupported handicap method";
        }

        return calculation;
    }

    private void calculateGhin(Player player, String handicapGroupCode, HandicapCalculation calculation) {
        List<ScoreHistoryEntry> entries = scoreHistoryEntryRepository.findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                player,
                handicapGroupCode,
                Set.of(SOURCE_GHIN_FROZEN, SOURCE_TRIP_ROUND)
        );

        entries.sort(this::compareForGhinTripOrder);
        applyCalculation(entries, calculation, 20, true);
    }

    private void calculateMyrtleBeach(Player player, String handicapGroupCode, HandicapCalculation calculation) {
        List<ScoreHistoryEntry> entries = new ArrayList<>();
        entries.addAll(scoreHistoryEntryRepository.findByPlayerAndSourceTypeIn(player, Set.of(SOURCE_DB_HISTORY_FROZEN)));
        entries.addAll(scoreHistoryEntryRepository.findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                player,
                handicapGroupCode,
                Set.of(SOURCE_TRIP_ROUND)
        ));

        entries.sort(this::compareForMyrtleBeachOrder);
        applyCalculation(entries, calculation, 6, false);
    }

    private void applyCalculation(List<ScoreHistoryEntry> orderedEntries,
                                  HandicapCalculation calculation,
                                  int windowSize,
                                  boolean ghinMethod) {
        List<ScoreHistoryEntry> currentEligibleEntries = new ArrayList<>();
        List<ScoreHistoryEntry> pendingEntries = new ArrayList<>();

        for (ScoreHistoryEntry entry : orderedEntries) {
            if (entry == null) {
                continue;
            }
            LocalDate scoreDate = entry.getScoreDate();
            if (scoreDate == null) {
                continue;
            }
            if (scoreDate.isAfter(calculation.asOfDate)) {
                continue;
            }
            if (scoreDate.isEqual(calculation.asOfDate)) {
                pendingEntries.add(entry);
                if (entry.getId() != null) {
                    calculation.pendingEntryIds.add(entry.getId());
                }
                continue;
            }
            if (isValidForIndex(entry)) {
                currentEligibleEntries.add(entry);
            }
        }

        calculation.eligibleScoreCount = currentEligibleEntries.size();

        int currentLimit = Math.min(windowSize, currentEligibleEntries.size());
        for (int i = 0; i < currentLimit; i++) {
            ScoreHistoryEntry entry = currentEligibleEntries.get(i);
            calculation.windowEntries.add(entry);
            if (entry.getId() != null) {
                calculation.windowEntryIds.add(entry.getId());
            }
        }

        int currentNumberToUse = ghinMethod
                ? determineWhsDifferentialsToUse(calculation.windowEntries.size())
                : determineMyrtleBeachDifferentialsToUse(calculation.windowEntries.size());
        calculation.usedEntryIds.addAll(lowestEntryIds(calculation.windowEntries, currentNumberToUse));
        calculation.tripIndex = averageDifferentials(calculation.windowEntries, calculation.usedEntryIds);
        calculation.calculationLabel = buildCalculationLabel(ghinMethod, calculation.windowEntries.size(), calculation.usedEntryIds.size());

        List<ScoreHistoryEntry> validPendingEntries = new ArrayList<>();
        for (ScoreHistoryEntry pendingEntry : pendingEntries) {
            if (isValidForIndex(pendingEntry)) {
                validPendingEntries.add(pendingEntry);
            }
        }

        if (!validPendingEntries.isEmpty()) {
            List<ScoreHistoryEntry> pendingWindow = new ArrayList<>();
            for (ScoreHistoryEntry pendingEntry : validPendingEntries) {
                if (pendingWindow.size() < windowSize) {
                    pendingWindow.add(pendingEntry);
                }
            }
            for (ScoreHistoryEntry currentEntry : calculation.windowEntries) {
                if (pendingWindow.size() < windowSize) {
                    pendingWindow.add(currentEntry);
                }
            }

            int pendingNumberToUse = ghinMethod
                    ? determineWhsDifferentialsToUse(pendingWindow.size())
                    : determineMyrtleBeachDifferentialsToUse(pendingWindow.size());
            calculation.pendingUsedEntryIds.addAll(lowestEntryIds(pendingWindow, pendingNumberToUse));
            calculation.pendingTripIndex = averageDifferentials(pendingWindow, calculation.pendingUsedEntryIds);
            calculation.pendingCalculationLabel = buildCalculationLabel(ghinMethod, pendingWindow.size(), calculation.pendingUsedEntryIds.size())
                    + " Includes score(s) posted on " + calculation.asOfDate + ".";
        }

        calculation.displayEntries.addAll(pendingEntries);
        calculation.displayEntries.addAll(calculation.windowEntries);
        calculation.manualReviewCount = countManualReview(orderedEntries);
    }

    private boolean isValidForIndex(ScoreHistoryEntry entry) {
        if (entry == null) {
            return false;
        }
        if (entry.getDifferential() == null) {
            return false;
        }
        return !Boolean.TRUE.equals(entry.getManualDifferentialRequired());
    }

    private String buildCalculationLabel(boolean ghinMethod, int scoreCount, int usedCount) {
        if (ghinMethod) {
            return "GHIN/WHS: latest " + scoreCount + " valid score(s), using lowest " + usedCount + ".";
        }
        return "DB Score History: latest " + scoreCount + " valid score(s), using lowest " + usedCount + ".";
    }

    private Set<Long> lowestEntryIds(List<ScoreHistoryEntry> entries, int numberToUse) {
        Set<Long> result = new HashSet<>();
        if (entries == null || entries.isEmpty() || numberToUse <= 0) {
            return result;
        }

        List<ScoreHistoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort(new Comparator<ScoreHistoryEntry>() {
            @Override
            public int compare(ScoreHistoryEntry a, ScoreHistoryEntry b) {
                BigDecimal aDiff = a.getDifferential();
                BigDecimal bDiff = b.getDifferential();
                if (aDiff == null && bDiff == null) {
                    return compareIds(a.getId(), b.getId());
                }
                if (aDiff == null) {
                    return 1;
                }
                if (bDiff == null) {
                    return -1;
                }
                int diffCompare = aDiff.compareTo(bDiff);
                if (diffCompare != 0) {
                    return diffCompare;
                }
                return compareIds(a.getId(), b.getId());
            }
        });

        int limit = Math.min(numberToUse, sorted.size());
        for (int i = 0; i < limit; i++) {
            ScoreHistoryEntry entry = sorted.get(i);
            if (entry.getId() != null) {
                result.add(entry.getId());
            }
        }
        return result;
    }

    private BigDecimal averageDifferentials(List<ScoreHistoryEntry> entries, Set<Long> usedEntryIds) {
        if (entries == null || entries.isEmpty() || usedEntryIds == null || usedEntryIds.isEmpty()) {
            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (ScoreHistoryEntry entry : entries) {
            if (entry == null || entry.getId() == null || entry.getDifferential() == null) {
                continue;
            }
            if (!usedEntryIds.contains(entry.getId())) {
                continue;
            }
            sum = sum.add(entry.getDifferential());
            count++;
        }

        if (count == 0) {
            return null;
        }

        return sum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    private int determineWhsDifferentialsToUse(int scoreCount) {
        if (scoreCount < 3) { return 0; }
        if (scoreCount <= 5) { return 1; }
        if (scoreCount <= 8) { return 2; }
        if (scoreCount <= 11) { return 3; }
        if (scoreCount <= 14) { return 4; }
        if (scoreCount <= 16) { return 5; }
        if (scoreCount <= 18) { return 6; }
        if (scoreCount == 19) { return 7; }
        return 8;
    }

    private int determineMyrtleBeachDifferentialsToUse(int scoreCount) {
        if (scoreCount == 0) { return 0; }
        if (scoreCount <= 3) { return 1; }
        if (scoreCount <= 5) { return 2; }
        return 3;
    }

    private int countManualReview(List<ScoreHistoryEntry> entries) {
        int count = 0;
        for (ScoreHistoryEntry entry : entries) {
            if (entry != null && Boolean.TRUE.equals(entry.getManualDifferentialRequired())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Handicap card recency order for GHIN-method players:
     * 1) Scores posted during the trip are newer than the frozen GHIN snapshot.
     * 2) Frozen/manual GHIN rows preserve the GHIN posting sequence captured at import time.
     */
    private int compareForGhinTripOrder(ScoreHistoryEntry a, ScoreHistoryEntry b) {
        boolean aTrip = SOURCE_TRIP_ROUND.equalsIgnoreCase(a.getSourceType());
        boolean bTrip = SOURCE_TRIP_ROUND.equalsIgnoreCase(b.getSourceType());

        if (aTrip && !bTrip) { return -1; }
        if (!aTrip && bTrip) { return 1; }

        if (aTrip) {
            int dateCompare = compareDatesDescending(a.getScoreDate(), b.getScoreDate());
            if (dateCompare != 0) { return dateCompare; }
            return compareIdsDescending(a.getId(), b.getId());
        }

        int postingCompare = comparePostingOrderAscending(a.getPostingOrder(), b.getPostingOrder());
        if (postingCompare != 0) { return postingCompare; }

        return compareIds(a.getId(), b.getId());
    }

    private int compareForMyrtleBeachOrder(ScoreHistoryEntry a, ScoreHistoryEntry b) {
        int dateCompare = compareDatesDescending(a.getScoreDate(), b.getScoreDate());
        if (dateCompare != 0) { return dateCompare; }
        return compareIdsDescending(a.getId(), b.getId());
    }

    private int comparePostingOrderAscending(Integer a, Integer b) {
        if (a == null && b == null) { return 0; }
        if (a == null) { return 1; }
        if (b == null) { return -1; }
        return Integer.compare(a, b);
    }

    private int compareDatesDescending(LocalDate a, LocalDate b) {
        if (a == null && b == null) { return 0; }
        if (a == null) { return 1; }
        if (b == null) { return -1; }
        return b.compareTo(a);
    }

    private int compareIds(Long a, Long b) {
        if (a == null && b == null) { return 0; }
        if (a == null) { return 1; }
        if (b == null) { return -1; }
        return a.compareTo(b);
    }

    private int compareIdsDescending(Long a, Long b) {
        if (a == null && b == null) { return 0; }
        if (a == null) { return 1; }
        if (b == null) { return -1; }
        return b.compareTo(a);
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

    private boolean isDbScoreHistoryMethod(String method) {
        return HANDICAP_METHOD_DB_SCORE_HISTORY.equals(method) || HANDICAP_METHOD_LEGACY_MYRTLE_BEACH.equals(method);
    }

    private static class HandicapCalculation {
        private LocalDate asOfDate;
        private BigDecimal tripIndex;
        private BigDecimal pendingTripIndex;
        private int eligibleScoreCount;
        private int manualReviewCount;
        private String calculationLabel;
        private String pendingCalculationLabel;
        private List<ScoreHistoryEntry> displayEntries = new ArrayList<>();
        private List<ScoreHistoryEntry> windowEntries = new ArrayList<>();
        private Set<Long> pendingEntryIds = new HashSet<>();
        private Set<Long> windowEntryIds = new HashSet<>();
        private Set<Long> usedEntryIds = new HashSet<>();
        private Set<Long> pendingUsedEntryIds = new HashSet<>();
    }
}
