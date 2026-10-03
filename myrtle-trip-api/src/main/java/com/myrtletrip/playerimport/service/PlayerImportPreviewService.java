package com.myrtletrip.playerimport.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.playerimport.dto.PlayerImportRow;
import com.myrtletrip.playerimport.model.ImportMatchStatus;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@Service
public class PlayerImportPreviewService {

    private final PlayerRepository playerRepository;
    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;

    public PlayerImportPreviewService(
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
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(reader)) {

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
        }

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
            boolean alreadyInTrip = tripPlayerRepository.existsByTrip_IdAndPlayer_Id(tripId, possibleMatch.getId());
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

    private void applyMatchedPlayer(Long tripId, PlayerImportRow row, Player player, ImportMatchStatus status) {
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

    private BigDecimal parseHandicapIndex(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) return null;
        try {
            BigDecimal parsed = new BigDecimal(trimmed);
            if (parsed.compareTo(new BigDecimal("-10.0")) < 0 || parsed.compareTo(new BigDecimal("54.0")) > 0) {
                return null;
            }
            return parsed.setScale(1, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String readValue(CSVRecord record, String columnName) {
        if (!record.isMapped(columnName)) return null;
        return trimToNull(record.get(columnName));
    }

    private String getPlayerName(Player player) {
        if (!isBlank(player.getDisplayName())) return player.getDisplayName();
        return buildDisplayName(player.getFirstName(), player.getLastName());
    }

    private String buildDisplayName(String firstName, String lastName) {
        String first = trimToNull(firstName);
        String last = trimToNull(lastName);
        if (first == null && last == null) return null;
        if (first == null) return last;
        if (last == null) return first;
        return first + " " + last;
    }

    private String cleanEmail(String value) {
        String cleaned = trimToNull(value);
        if (cleaned == null) return null;
        cleaned = cleaned.trim();
        if (cleaned.toLowerCase().startsWith("mailto:")) cleaned = cleaned.substring(7).trim();
        int openAngle = cleaned.indexOf('<');
        int closeAngle = cleaned.indexOf('>');
        if (openAngle >= 0 && closeAngle > openAngle) cleaned = cleaned.substring(openAngle + 1, closeAngle).trim();
        int semicolon = cleaned.indexOf(';');
        if (semicolon >= 0) cleaned = cleaned.substring(0, semicolon).trim();
        cleaned = cleaned.replaceAll("^[\\s\"'<>;,]+", "");
        cleaned = cleaned.replaceAll("[\\s\"'<>;,]+$", "");
        return trimToNull(cleaned);
    }

    private String normalizeEmail(String value) {
        String cleaned = cleanEmail(value);
        return cleaned == null ? null : cleaned.trim().toLowerCase();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
