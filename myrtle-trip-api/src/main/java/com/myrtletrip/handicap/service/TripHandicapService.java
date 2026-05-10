package com.myrtletrip.handicap.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.trip.model.TripHandicapMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class TripHandicapService {

    private static final String HANDICAP_METHOD_GHIN = "GHIN";
    private static final String HANDICAP_METHOD_MYRTLE_BEACH = "MYRTLE_BEACH";
    private static final String HANDICAP_METHOD_DB_SCORE_HISTORY = "DB_SCORE_HISTORY";

    private static final String SOURCE_GHIN_FROZEN = "GHIN_FROZEN";
    private static final String SOURCE_DB_HISTORY_FROZEN = "DB_HISTORY_FROZEN";
    private static final String SOURCE_TRIP_ROUND = "TRIP_ROUND";

    private final PlayerRepository playerRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;

    public TripHandicapService(PlayerRepository playerRepository,
                               ScoreHistoryEntryRepository scoreHistoryEntryRepository) {
        this.playerRepository = playerRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
    }

    public List<com.myrtletrip.handicap.dto.PlayerTripIndexResponse> calculateTripIndexes(
            String handicapGroupCode,
            List<Player> players
    ) {
        if (handicapGroupCode == null || handicapGroupCode.isBlank()) {
            throw new IllegalArgumentException("handicapGroupCode is required");
        }
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("players are required");
        }

        return players.stream().map(player -> {
            com.myrtletrip.handicap.dto.PlayerTripIndexResponse response =
                    new com.myrtletrip.handicap.dto.PlayerTripIndexResponse();

            response.setPlayerId(player.getId());
            response.setPlayerName(player.getDisplayName());
            response.setHandicapMethod(player.getHandicapMethod());
            response.setTripIndex(calculateTripIndex(player, handicapGroupCode));

            return response;
        }).toList();
    }


    public BigDecimal calculateTripIndex(Player player, String handicapGroupCode, TripHandicapMethod tripHandicapMethod) {
        validateInputs(player, handicapGroupCode);

        TripHandicapMethod effectiveMethod = tripHandicapMethod == null
                ? TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY
                : tripHandicapMethod;

        if (TripHandicapMethod.GHIN_HISTORY.equals(effectiveMethod)) {
            return calculateGhinTripIndex(player, handicapGroupCode);
        }

        if (TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY.equals(effectiveMethod)) {
            return calculateCombinedGhinAndDbScoreHistoryTripIndex(player, handicapGroupCode);
        }

        throw new IllegalStateException("Unsupported non-frozen trip handicap method: " + effectiveMethod);
    }

    public BigDecimal calculateTripIndexAsOf(Player player, String handicapGroupCode, LocalDate asOfDate, TripHandicapMethod tripHandicapMethod) {
        validateInputs(player, handicapGroupCode);

        if (asOfDate == null) {
            throw new IllegalArgumentException("asOfDate is required");
        }

        TripHandicapMethod effectiveMethod = tripHandicapMethod == null
                ? TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY
                : tripHandicapMethod;

        if (TripHandicapMethod.GHIN_HISTORY.equals(effectiveMethod)) {
            return calculateGhinTripIndexAsOf(player, handicapGroupCode, asOfDate);
        }

        if (TripHandicapMethod.GHIN_PLUS_DB_SCORE_HISTORY.equals(effectiveMethod)) {
            return calculateCombinedGhinAndDbScoreHistoryTripIndexAsOf(player, handicapGroupCode, asOfDate);
        }

        throw new IllegalStateException("Unsupported non-frozen trip handicap method: " + effectiveMethod);
    }

    public BigDecimal calculateTripIndex(Long playerId, String handicapGroupCode) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        return calculateTripIndex(player, handicapGroupCode);
    }

    public BigDecimal calculateTripIndex(Player player, String handicapGroupCode) {
        validateInputs(player, handicapGroupCode);

        String handicapMethod = normalize(player.getHandicapMethod());

        if (HANDICAP_METHOD_GHIN.equals(handicapMethod)) {
            return calculateGhinTripIndex(player, handicapGroupCode);
        }

        if (isDbScoreHistoryMethod(handicapMethod)) {
            return calculateDbScoreHistoryTripIndex(player, handicapGroupCode);
        }

        throw new IllegalStateException("Unsupported handicap method for player "
                + player.getId() + ": " + player.getHandicapMethod());
    }

    public BigDecimal calculateTripIndexAsOf(Long playerId, String handicapGroupCode, LocalDate asOfDate) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + playerId));

        return calculateTripIndexAsOf(player, handicapGroupCode, asOfDate);
    }

    public BigDecimal calculateTripIndexAsOf(Player player, String handicapGroupCode, LocalDate asOfDate) {
        validateInputs(player, handicapGroupCode);

        if (asOfDate == null) {
            throw new IllegalArgumentException("asOfDate is required");
        }

        String handicapMethod = normalize(player.getHandicapMethod());

        if (HANDICAP_METHOD_GHIN.equals(handicapMethod)) {
            return calculateGhinTripIndexAsOf(player, handicapGroupCode, asOfDate);
        }

        if (isDbScoreHistoryMethod(handicapMethod)) {
            return calculateDbScoreHistoryTripIndexAsOf(player, handicapGroupCode, asOfDate);
        }

        throw new IllegalStateException("Unsupported handicap method for player "
                + player.getId() + ": " + player.getHandicapMethod());
    }

    public BigDecimal calculateGhinTripIndex(Player player, String handicapGroupCode) {
        validateInputs(player, handicapGroupCode);

        List<ScoreHistoryEntry> eligibleEntries = scoreHistoryEntryRepository
                .findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                        player,
                        handicapGroupCode,
                        Set.of(SOURCE_GHIN_FROZEN, SOURCE_TRIP_ROUND)
                );

        List<BigDecimal> newestTwentyDifferentials = eligibleEntries.stream()
                .filter(e -> e.getDifferential() != null)
                .sorted(this::compareForGhinTripOrder)
                .limit(20)
                .map(ScoreHistoryEntry::getDifferential)
                .toList();

        return calculateWhsIndexFromDifferentials(newestTwentyDifferentials);
    }

    public BigDecimal calculateGhinTripIndexAsOf(Player player, String handicapGroupCode, LocalDate asOfDate) {
        validateInputs(player, handicapGroupCode);

        List<ScoreHistoryEntry> eligibleEntries = scoreHistoryEntryRepository
                .findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                        player,
                        handicapGroupCode,
                        Set.of(SOURCE_GHIN_FROZEN, SOURCE_TRIP_ROUND)
                ).stream()
                .filter(e -> e.getDifferential() != null)
                .filter(e -> isEligibleAsOf(e, asOfDate))
                .sorted(this::compareForGhinTripOrder)
                .limit(20)
                .toList();

        List<BigDecimal> newestTwentyDifferentials = eligibleEntries.stream()
                .map(ScoreHistoryEntry::getDifferential)
                .toList();

        return calculateWhsIndexFromDifferentials(newestTwentyDifferentials);
    }

    public BigDecimal calculateDbScoreHistoryTripIndex(Player player, String handicapGroupCode) {
        validateInputs(player, handicapGroupCode);

        List<BigDecimal> newestSixDifferentials = loadDbScoreHistoryEligibleEntries(player, handicapGroupCode).stream()
                .filter(e -> e.getDifferential() != null)
                .sorted(Comparator.comparing(ScoreHistoryEntry::getScoreDate).reversed()
                        .thenComparing(ScoreHistoryEntry::getId, Comparator.reverseOrder()))
                .limit(6)
                .map(ScoreHistoryEntry::getDifferential)
                .sorted()
                .toList();

        return calculateDbScoreHistoryIndexFromDifferentials(newestSixDifferentials);
    }

    public BigDecimal calculateDbScoreHistoryTripIndexAsOf(Player player, String handicapGroupCode, LocalDate asOfDate) {
        validateInputs(player, handicapGroupCode);

        List<BigDecimal> newestSixDifferentials = loadDbScoreHistoryEligibleEntries(player, handicapGroupCode).stream()
                .filter(e -> e.getDifferential() != null)
                .filter(e -> isEligibleAsOf(e, asOfDate))
                .sorted(Comparator.comparing(ScoreHistoryEntry::getScoreDate).reversed()
                        .thenComparing(ScoreHistoryEntry::getId, Comparator.reverseOrder()))
                .limit(6)
                .map(ScoreHistoryEntry::getDifferential)
                .sorted()
                .toList();

        return calculateDbScoreHistoryIndexFromDifferentials(newestSixDifferentials);
    }


    public BigDecimal calculateCombinedGhinAndDbScoreHistoryTripIndex(Player player, String handicapGroupCode) {
        validateInputs(player, handicapGroupCode);

        List<BigDecimal> newestTwentyDifferentials = loadCombinedGhinAndDbScoreHistoryEligibleEntries(player, handicapGroupCode).stream()
                .filter(e -> e.getDifferential() != null)
                .sorted(this::compareForGhinTripOrder)
                .limit(20)
                .map(ScoreHistoryEntry::getDifferential)
                .toList();

        return calculateWhsIndexFromDifferentials(newestTwentyDifferentials);
    }

    public BigDecimal calculateCombinedGhinAndDbScoreHistoryTripIndexAsOf(Player player, String handicapGroupCode, LocalDate asOfDate) {
        validateInputs(player, handicapGroupCode);

        List<BigDecimal> newestTwentyDifferentials = loadCombinedGhinAndDbScoreHistoryEligibleEntries(player, handicapGroupCode).stream()
                .filter(e -> e.getDifferential() != null)
                .filter(e -> isEligibleAsOf(e, asOfDate))
                .sorted(this::compareForGhinTripOrder)
                .limit(20)
                .map(ScoreHistoryEntry::getDifferential)
                .toList();

        return calculateWhsIndexFromDifferentials(newestTwentyDifferentials);
    }

    private List<ScoreHistoryEntry> loadCombinedGhinAndDbScoreHistoryEligibleEntries(Player player, String handicapGroupCode) {
        List<ScoreHistoryEntry> ghinAndTripEntries = scoreHistoryEntryRepository
                .findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                        player,
                        handicapGroupCode,
                        Set.of(SOURCE_GHIN_FROZEN, SOURCE_TRIP_ROUND)
                );

        List<ScoreHistoryEntry> dbHistoryEntries = scoreHistoryEntryRepository
                .findByPlayerAndSourceTypeIn(player, Set.of(SOURCE_DB_HISTORY_FROZEN));

        return Stream.concat(ghinAndTripEntries.stream(), dbHistoryEntries.stream())
                .toList();
    }

    private List<ScoreHistoryEntry> loadDbScoreHistoryEligibleEntries(Player player, String handicapGroupCode) {
        List<ScoreHistoryEntry> frozenEntries = scoreHistoryEntryRepository
                .findByPlayerAndSourceTypeIn(player, Set.of(SOURCE_DB_HISTORY_FROZEN));

        List<ScoreHistoryEntry> tripRoundEntries = scoreHistoryEntryRepository
                .findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
                        player,
                        handicapGroupCode,
                        Set.of(SOURCE_TRIP_ROUND)
                );

        return Stream.concat(frozenEntries.stream(), tripRoundEntries.stream())
                .toList();
    }

    private int compareForGhinTripOrder(ScoreHistoryEntry a, ScoreHistoryEntry b) {
        boolean aTrip = SOURCE_TRIP_ROUND.equalsIgnoreCase(a.getSourceType());
        boolean bTrip = SOURCE_TRIP_ROUND.equalsIgnoreCase(b.getSourceType());

        if (aTrip && bTrip) {
            LocalDate aDate = a.getScoreDate();
            LocalDate bDate = b.getScoreDate();

            if (aDate == null && bDate == null) {
                Long aId = a.getId();
                Long bId = b.getId();
                if (aId == null && bId == null) {
                    return 0;
                }
                if (aId == null) {
                    return 1;
                }
                if (bId == null) {
                    return -1;
                }
                return bId.compareTo(aId);
            }

            if (aDate == null) {
                return 1;
            }
            if (bDate == null) {
                return -1;
            }

            int dateCompare = bDate.compareTo(aDate);
            if (dateCompare != 0) {
                return dateCompare;
            }

            Long aId = a.getId();
            Long bId = b.getId();
            if (aId == null && bId == null) {
                return 0;
            }
            if (aId == null) {
                return 1;
            }
            if (bId == null) {
                return -1;
            }

            return bId.compareTo(aId);
        }

        if (aTrip) {
            return -1;
        }
        if (bTrip) {
            return 1;
        }

        Integer aOrder = a.getPostingOrder() != null ? a.getPostingOrder() : Integer.MAX_VALUE;
        Integer bOrder = b.getPostingOrder() != null ? b.getPostingOrder() : Integer.MAX_VALUE;

        return Integer.compare(aOrder, bOrder);
    }

    private BigDecimal calculateWhsIndexFromDifferentials(List<BigDecimal> newestDifferentials) {
        int count = newestDifferentials.size();
        if (count == 0) {
            return null;
        }

        int differentialsToUse = determineWhsDifferentialsToUse(count);
        if (differentialsToUse == 0) {
            return null;
        }

        List<BigDecimal> lowestDifferentials = newestDifferentials.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .limit(differentialsToUse)
                .toList();

        if (lowestDifferentials.isEmpty()) {
            return null;
        }

        BigDecimal sum = lowestDifferentials.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(lowestDifferentials.size()), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDbScoreHistoryIndexFromDifferentials(List<BigDecimal> differentials) {
        int count = differentials.size();
        if (count == 0) {
            return null;
        }

        int numberToUse;
        if (count <= 3) {
            numberToUse = 1;
        } else if (count <= 5) {
            numberToUse = 2;
        } else {
            numberToUse = 3;
        }

        BigDecimal sum = differentials.stream()
                .limit(numberToUse)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(numberToUse), 1, RoundingMode.HALF_UP);
    }

    private int determineWhsDifferentialsToUse(int scoreCount) {
        if (scoreCount < 3) return 0;
        if (scoreCount <= 5) return 1;
        if (scoreCount <= 8) return 2;
        if (scoreCount <= 11) return 3;
        if (scoreCount <= 14) return 4;
        if (scoreCount <= 16) return 5;
        if (scoreCount <= 18) return 6;
        if (scoreCount == 19) return 7;
        return 8;
    }

    private boolean isEligibleAsOf(ScoreHistoryEntry entry, LocalDate asOfDate) {
        LocalDate scoreDate = entry.getScoreDate();
        return scoreDate != null && scoreDate.isBefore(asOfDate);
    }

    private boolean isDbScoreHistoryMethod(String handicapMethod) {
        return HANDICAP_METHOD_DB_SCORE_HISTORY.equals(handicapMethod)
                || HANDICAP_METHOD_MYRTLE_BEACH.equals(handicapMethod);
    }

    private void validateInputs(Player player, String handicapGroupCode) {
        if (player == null) {
            throw new IllegalArgumentException("Player is required");
        }
        if (handicapGroupCode == null || handicapGroupCode.isBlank()) {
            throw new IllegalArgumentException("handicapGroupCode is required");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}