package com.myrtletrip.player.controller;

import com.myrtletrip.handicap.source.frozen.FrozenGhinImportService;
import com.myrtletrip.player.dto.PlayerDetailResponse;
import com.myrtletrip.player.dto.PlayerListResponse;
import com.myrtletrip.player.dto.SavePlayerRequest;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/players")
@CrossOrigin
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final FrozenGhinImportService service;

    public PlayerController(PlayerRepository playerRepository, FrozenGhinImportService service) {
        this.playerRepository = playerRepository;
        this.service = service;
    }

    @GetMapping
    public List<PlayerListResponse> getAllPlayers() {
        List<Player> players = playerRepository.findAll();
        players.sort(Comparator.comparing(this::buildSortName, String.CASE_INSENSITIVE_ORDER));

        List<PlayerListResponse> response = new ArrayList<>();

        for (Player player : players) {
            response.add(toListResponse(player));
        }

        return response;
    }

    @GetMapping("/{id}")
    public PlayerDetailResponse getPlayer(@PathVariable Long id) {
        Player player = findPlayer(id);
        return toDetailResponse(player);
    }

    @PostMapping
    public PlayerDetailResponse createPlayer(@RequestBody SavePlayerRequest request) {
        validateRequest(request);

        Player player = new Player();
        applyRequest(player, request);

        Player saved = playerRepository.save(player);
        return toDetailResponse(saved);
    }

    @PutMapping("/{id}")
    public PlayerDetailResponse updatePlayer(@PathVariable Long id,
                                             @RequestBody SavePlayerRequest request) {
        validateRequest(request);

        Player player = findPlayer(id);
        applyRequest(player, request);

        Player saved = playerRepository.save(player);
        return toDetailResponse(saved);
    }

    @PutMapping("/{id}/active")
    public PlayerDetailResponse setPlayerActive(@PathVariable Long id,
                                                @RequestParam boolean active) {
        Player player = findPlayer(id);
        player.setActive(active);

        Player saved = playerRepository.save(player);
        return toDetailResponse(saved);
    }

    @PostMapping("/initialize/player/{id}")
    public String initializePlayer(@PathVariable Long id) throws Exception {
        Player player = findPlayer(id);
        service.initializeFrozenGhinForPlayer(player, "MYRTLE_2026");
        return "Done";
    }

    private Player findPlayer(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found: " + id));
    }

    private void validateRequest(SavePlayerRequest request) {
        if (request == null) {
            throw new RuntimeException("Player request is required");
        }

        if (isBlank(request.getDisplayName())) {
            throw new RuntimeException("Display name is required");
        }

        String gender = normalizeGender(request.getGender());
        if (!"M".equals(gender) && !"F".equals(gender)) {
            throw new RuntimeException("Gender must be M or F");
        }
    }

    private void applyRequest(Player player, SavePlayerRequest request) {
        player.setFirstName(trimToNull(request.getFirstName()));
        player.setLastName(trimToNull(request.getLastName()));
        player.setDisplayName(request.getDisplayName().trim());
        player.setGhinNumber(trimToNull(request.getGhinNumber()));
        player.setEmail(trimToNull(request.getEmail()));
        player.setCell(trimToNull(request.getCell()));
        player.setVenmoId(trimToNull(request.getVenmoId()));
        player.setZelleId(trimToNull(request.getZelleId()));
        player.setHandicapMethod(trimToNull(request.getHandicapMethod()));
        player.setGender(normalizeGender(request.getGender()));

        if (request.getActive() == null) {
            player.setActive(true);
        } else {
            player.setActive(request.getActive());
        }
    }

    private PlayerListResponse toListResponse(Player player) {
        PlayerListResponse response = new PlayerListResponse();
        response.setPlayerId(player.getId());
        response.setDisplayName(player.getDisplayName());
        response.setFirstName(player.getFirstName());
        response.setLastName(player.getLastName());
        response.setGhinNumber(player.getGhinNumber());
        response.setActive(player.isActive());
        response.setHandicapMethod(player.getHandicapMethod());
        response.setEmail(player.getEmail());
        response.setCell(player.getCell());
        response.setGender(normalizeGender(player.getGender()));
        return response;
    }

    private PlayerDetailResponse toDetailResponse(Player player) {
        PlayerDetailResponse response = new PlayerDetailResponse();
        response.setPlayerId(player.getId());
        response.setFirstName(player.getFirstName());
        response.setLastName(player.getLastName());
        response.setDisplayName(player.getDisplayName());
        response.setGhinNumber(player.getGhinNumber());
        response.setActive(player.isActive());
        response.setEmail(player.getEmail());
        response.setCell(player.getCell());
        response.setVenmoId(player.getVenmoId());
        response.setZelleId(player.getZelleId());
        response.setHandicapMethod(player.getHandicapMethod());
        response.setGender(normalizeGender(player.getGender()));
        return response;
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

    private String normalizeGender(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "M";
        }

        String normalized = value.trim().toUpperCase();
        if ("F".equals(normalized)) {
            return "F";
        }
        return "M";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String buildSortName(Player player) {
        if (!isBlank(player.getDisplayName())) {
            return player.getDisplayName().trim();
        }

        String firstName = trimToNull(player.getFirstName());
        String lastName = trimToNull(player.getLastName());
        if (firstName == null && lastName == null) {
            return "zzzz";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
