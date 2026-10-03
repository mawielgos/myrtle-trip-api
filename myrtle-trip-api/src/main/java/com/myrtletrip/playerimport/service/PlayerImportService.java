package com.myrtletrip.playerimport.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.myrtletrip.playerimport.dto.PlayerImportRow;

@Service
public class PlayerImportService {

    private final PlayerImportPreviewService previewService;
    private final PlayerImportCommitService commitService;

    public PlayerImportService(
        PlayerImportPreviewService previewService,
        PlayerImportCommitService commitService
    ) {
        this.previewService = previewService;
        this.commitService = commitService;
    }

    public List<PlayerImportRow> previewCsv(Long tripId, MultipartFile file) throws IOException {
        return previewService.previewCsv(tripId, file);
    }

    public void commitImport(Long tripId, List<PlayerImportRow> rows) {
        commitService.commitImport(tripId, rows);
    }
}
