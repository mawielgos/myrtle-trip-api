package com.myrtletrip.player.controller;

import com.myrtletrip.handicap.service.FrozenGhinImportService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final FrozenGhinImportService service;

    public PlayerController(PlayerRepository playerRepository, FrozenGhinImportService service) {
        this.playerRepository = playerRepository;
        this.service = service;
    }

    @GetMapping
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    @PostMapping("/initialize/player/{id}")
    public String initializePlayer(@PathVariable Long id) throws Exception {
        Player player = playerRepository.findById(id).orElseThrow();
        service.initializeFrozenGhinForPlayer(player, "MYRTLE_2026");
        return "Done";
    }
}