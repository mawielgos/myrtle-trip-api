package com.myrtletrip.playerimport.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.myrtletrip.playerimport.dto.PlayerImportCommitRequest;
import com.myrtletrip.playerimport.dto.PlayerImportRow;
import com.myrtletrip.playerimport.service.PlayerImportService;

@RestController
@RequestMapping("/api/trips/{tripId}/player-import")
@CrossOrigin
public class PlayerImportController {

    private final PlayerImportService playerImportService;

    public PlayerImportController(PlayerImportService playerImportService) {
        this.playerImportService = playerImportService;
    }

    @PostMapping("/preview")
    public ResponseEntity<List<PlayerImportRow>> previewImport(
        @PathVariable Long tripId,
        @RequestParam("file") MultipartFile file
    ) throws IOException {

        return ResponseEntity.ok(playerImportService.previewCsv(tripId, file));
    }

    @PostMapping("/commit")
    public ResponseEntity<Void> commitImport(
        @PathVariable Long tripId,
        @RequestBody PlayerImportCommitRequest request
    ) {

        playerImportService.commitImport(tripId, request.getRows());
        return ResponseEntity.ok().build();
    }
}
