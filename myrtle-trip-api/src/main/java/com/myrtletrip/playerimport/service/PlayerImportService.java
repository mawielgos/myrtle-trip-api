package com.myrtletrip.playerimport.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.playerimport.dto.PlayerImportRow;
import com.myrtletrip.playerimport.model.ImportMatchStatus;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@Service
public class PlayerImportService {

    private static final String HANDICAP_METHOD_GHIN = "GHIN";

    private final PlayerRepository playerRepository;
    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;

    public PlayerImportService(
        PlayerRepository playerRepository,
        TripRepository tripRepository,
        TripPlayerRepository tripPlayerRepository
    ) {
        this.playerRepository = playerRepository;
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
    }

    public List<PlayerImportRow> previewCsv(Long tripId, MultipartFile file) throws IOException {

        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (Boolean.TRUE.equals(trip.getInitialized())) {
            throw new IllegalStateException("Players cannot be imported after the event has started.");
        }

        List<PlayerImportRow> rows = new ArrayList<>();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
        );

        CSVParser parser = CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
            .parse(reader);

        int rowNumber = 1;

        for (CSVRecord record : parser) {
            PlayerImportRow row = new PlayerImportRow();

            row.setRowNumber(rowNumber++);
            row.setFirstName(readValue(record, "First Name"));
            row.setLastName(readValue(record, "Last Name"));
            row.setEmail(cleanEmail(readValue(record, "Email")));
            row.setGhinNumber(readValue(record, "GHIN Number"));
            row.setGender(readValue(record, "Gender"));
            row.setHandicapIndex(parseHandicapIndex(readValue(record, "Handicap Index")));

            applyMatching(tripId, row);
            rows.add(row);
        }

        parser.close();
        return rows;
    }

    private void applyMatching(Long tripId, PlayerImportRow row) {

        if (isBlank(row.getFirstName()) || isBlank(row.getLastName())) {
            row.setMatchStatus(ImportMatchStatus.INVALID);
            row.setValidationMessage("Missing required first name or last name.");
            return;
        }

        if (!isBlank(row.getGhinNumber())) {
            Optional<Player> ghinMatch = playerRepository.findByGhinNumberIgnoreCase(row.getGhinNumber().trim());

            if (ghinMatch.isPresent()) {
                applyMatchedPlayer(tripId, row, ghinMatch.get(), ImportMatchStatus.MATCHED_BY_GHIN);
                return;
            }
        }

        if (!isBlank(row.getEmail())) {
            Optional<Player> emailMatch = playerRepository.findByNormalizedEmail(normalizeEmail(row.getEmail()));

            if (emailMatch.isPresent()) {
                applyMatchedPlayer(tripId, row, emailMatch.get(), ImportMatchStatus.MATCHED_BY_EMAIL);
                return;
            }
        }

        String normalizedName = normalize(row.getFirstName() + " " + row.getLastName());
        List<Player> nameMatches = playerRepository.findByNormalizedName(normalizedName);

        if (!nameMatches.isEmpty()) {
            Player possibleMatch = nameMatches.get(0);
            boolean alreadyInTrip =
                tripPlayerRepository.existsByTrip_IdAndPlayer_Id(tripId, possibleMatch.getId());

            row.setMatchedPlayerId(possibleMatch.getId());
            row.setMatchedPlayerName(getPlayerName(possibleMatch));
            row.setAlreadyInTrip(alreadyInTrip);

            if (alreadyInTrip && nameMatches.size() == 1) {
                row.setMatchStatus(ImportMatchStatus.ALREADY_IN_TRIP);
                row.setValidationMessage("Already on roster. Frozen index and display order will be updated from this CSV.");
                return;
            }

            row.setMatchStatus(ImportMatchStatus.POSSIBLE_DUPLICATE);
            row.setValidationMessage("Possible duplicate name match. Add GHIN/email or resolve manually before committing roster sync.");
            return;
        }

        row.setMatchStatus(ImportMatchStatus.NEW_PLAYER);
        row.setAlreadyInTrip(false);
    }

    private void applyMatchedPlayer(
        Long tripId,
        PlayerImportRow row,
        Player player,
        ImportMatchStatus status
    ) {
        row.setMatchedPlayerId(player.getId());
        row.setMatchedPlayerName(getPlayerName(player));

        boolean alreadyInTrip = tripPlayerRepository.existsByTrip_IdAndPlayer_Id(tripId, player.getId());
        row.setAlreadyInTrip(alreadyInTrip);

        if (alreadyInTrip) {
            row.setMatchStatus(ImportMatchStatus.ALREADY_IN_TRIP);
            row.setValidationMessage("Already on roster. Frozen index and display order will be updated from this CSV.");
        } else {
            row.setMatchStatus(status);
        }
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

    private BigDecimal parseHandicapIndex(String value) {
        String trimmed = trimToNull(value);

        if (trimmed == null) {
            return null;
        }

        try {
            BigDecimal parsed = new BigDecimal(trimmed);

            if (parsed.compareTo(new BigDecimal("-10.0")) < 0 ||
                    parsed.compareTo(new BigDecimal("54.0")) > 0) {
                return null;
            }

            return parsed.setScale(1, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String readValue(CSVRecord record, String columnName) {
        if (!record.isMapped(columnName)) {
            return null;
        }

        String value = record.get(columnName);
        return trimToNull(value);
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

        if (value.startsWith("F")) {
            return "F";
        }

        return "M";
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

    private String normalizeEmail(String value) {
        String cleaned = cleanEmail(value);

        if (cleaned == null) {
            return null;
        }

        return cleaned.trim().toLowerCase();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed;
    }
}
