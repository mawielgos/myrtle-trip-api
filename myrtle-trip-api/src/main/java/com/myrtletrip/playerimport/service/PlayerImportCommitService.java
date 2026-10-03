package com.myrtletrip.playerimport.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.playerimport.dto.PlayerImportRow;
import com.myrtletrip.playerimport.model.ImportMatchStatus;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@Service
public class PlayerImportCommitService {

    private static final String HANDICAP_METHOD_GHIN = "GHIN";

    private final PlayerRepository playerRepository;
    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;

    public PlayerImportCommitService(
        PlayerRepository playerRepository,
        TripRepository tripRepository,
        TripPlayerRepository tripPlayerRepository
    ) {
        this.playerRepository = playerRepository;
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
    }

    @Transactional
    public void commitImport(Long tripId, List<PlayerImportRow> rows) {

        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (Boolean.TRUE.equals(trip.getInitialized())) {
            throw new IllegalStateException("Players cannot be imported after the event has started.");
        }

        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("No import rows were submitted.");
        }

        for (PlayerImportRow row : rows) {
            if (row == null || row.getMatchStatus() == null) {
                throw new IllegalArgumentException("Import contains an invalid row.");
            }

            if (row.getMatchStatus() == ImportMatchStatus.INVALID) {
                throw new IllegalArgumentException("Fix or remove invalid rows before committing roster sync.");
            }

            if (row.getMatchStatus() == ImportMatchStatus.POSSIBLE_DUPLICATE) {
                throw new IllegalArgumentException("Resolve possible duplicate rows before committing roster sync.");
            }
        }

        Set<Long> desiredPlayerIds = new HashSet<>();
        Set<Long> seenPlayerIds = new HashSet<>();
        int displayOrder = 1;

        for (PlayerImportRow row : rows) {
            Player player = resolvePlayerForCommit(row);

            if (player == null) {
                throw new IllegalArgumentException("Could not resolve player on row " + row.getRowNumber() + ".");
            }

            if (seenPlayerIds.contains(player.getId())) {
                throw new IllegalArgumentException("The CSV contains the same player more than once: " + getPlayerName(player));
            }

            seenPlayerIds.add(player.getId());
            desiredPlayerIds.add(player.getId());

            syncPlayerFromImportRow(player, row);

            TripPlayer tripPlayer = tripPlayerRepository.findByTrip_IdAndPlayer_Id(tripId, player.getId())
                .orElseGet(() -> {
                    TripPlayer created = new TripPlayer();
                    created.setTrip(trip);
                    created.setPlayer(player);
                    return created;
                });

            tripPlayer.setDisplayOrder(displayOrder++);
            tripPlayer.setFrozenHandicapIndex(row.getHandicapIndex());
            tripPlayerRepository.save(tripPlayer);
        }

        List<TripPlayer> existingTripPlayers = tripPlayerRepository.findByTrip_Id(tripId);
        for (TripPlayer existingTripPlayer : existingTripPlayers) {
            Player existingPlayer = existingTripPlayer.getPlayer();

            if (existingPlayer == null || existingPlayer.getId() == null) {
                continue;
            }

            if (!desiredPlayerIds.contains(existingPlayer.getId())) {
                tripPlayerRepository.delete(existingTripPlayer);
            }
        }
    }

    private Player resolvePlayerForCommit(PlayerImportRow row) {

        if (row.getMatchedPlayerId() != null && row.getMatchStatus() != ImportMatchStatus.NEW_PLAYER) {
            return playerRepository.findById(row.getMatchedPlayerId()).orElse(null);
        }

        if (row.getMatchStatus() == ImportMatchStatus.NEW_PLAYER) {
            Player player = new Player();
            player.setFirstName(trimToNull(row.getFirstName()));
            player.setLastName(trimToNull(row.getLastName()));
            player.setDisplayName(buildDisplayName(row.getFirstName(), row.getLastName()));
            player.setEmail(cleanEmail(row.getEmail()));
            player.setGhinNumber(trimToNull(row.getGhinNumber()));
            if (!isBlank(row.getGhinNumber())) {
                player.setHandicapMethod(HANDICAP_METHOD_GHIN);
            }
            player.setGender(normalizeGender(row.getGender()));
            player.setActive(true);
            player.setCreatedSource("IMPORT");
            player.setCreatedBy("SYSTEM");
            player.setCreatedAt(LocalDateTime.now());

            return playerRepository.save(player);
        }

        return null;
    }

    private void syncPlayerFromImportRow(Player player, PlayerImportRow row) {
        if (player == null || row == null) {
            return;
        }

        String importedGhinNumber = trimToNull(row.getGhinNumber());
        if (importedGhinNumber != null) {
            player.setGhinNumber(importedGhinNumber);
            player.setHandicapMethod(HANDICAP_METHOD_GHIN);
        }

        String importedEmail = cleanEmail(row.getEmail());
        if (importedEmail != null) {
            player.setEmail(importedEmail);
        }

        String importedGender = trimToNull(row.getGender());
        if (importedGender != null) {
            player.setGender(normalizeGender(importedGender));
        }

        if (trimToNull(player.getDisplayName()) == null) {
            player.setDisplayName(buildDisplayName(player.getFirstName(), player.getLastName()));
        }

        playerRepository.save(player);
    }

    private String buildDisplayName(String firstName, String lastName) {
        String first = trimToNull(firstName);
        String last = trimToNull(lastName);

        if (first == null && last == null) {
            return null;
        }
        if (first == null) {
            return last;
        }
        if (last == null) {
            return first;
        }
        return first + " " + last;
    }

    private String getPlayerName(Player player) {
        if (!isBlank(player.getDisplayName())) {
            return player.getDisplayName();
        }
        return buildDisplayName(player.getFirstName(), player.getLastName());
    }

    private String normalizeGender(String gender) {
        if (isBlank(gender)) {
            return "M";
        }
        String value = gender.trim().toUpperCase();
        return value.startsWith("F") ? "F" : "M";
    }

    private String cleanEmail(String value) {
        String cleaned = trimToNull(value);
        if (cleaned == null) {
            return null;
        }
        cleaned = cleaned.trim();
        if (cleaned.toLowerCase().startsWith("mailto:")) {
            cleaned = cleaned.substring(7).trim();
        }
        int openAngle = cleaned.indexOf('<');
        int closeAngle = cleaned.indexOf('>');
        if (openAngle >= 0 && closeAngle > openAngle) {
            cleaned = cleaned.substring(openAngle + 1, closeAngle).trim();
        }
        int semicolon = cleaned.indexOf(';');
        if (semicolon >= 0) {
            cleaned = cleaned.substring(0, semicolon).trim();
        }
        cleaned = cleaned.replaceAll("^[\\s\"'<>;,]+", "");
        cleaned = cleaned.replaceAll("[\\s\"'<>;,]+$", "");
        return trimToNull(cleaned);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
