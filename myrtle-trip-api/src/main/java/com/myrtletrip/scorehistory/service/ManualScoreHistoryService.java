package com.myrtletrip.scorehistory.service;

import com.myrtletrip.scorehistory.dto.DbScoreHistoryImportCandidateResponse;
import com.myrtletrip.scorehistory.dto.ManualScoreHistoryEntryResponse;
import com.myrtletrip.scorehistory.dto.SaveManualScoreHistoryEntryRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManualScoreHistoryService {

    public static final String SOURCE_TYPE_GHIN_FROZEN = "GHIN_FROZEN";

    private final ManualScoreHistoryReadModelService readModelService;
    private final ManualScoreHistoryCommandService commandService;

    public ManualScoreHistoryService(ManualScoreHistoryReadModelService readModelService,
                                     ManualScoreHistoryCommandService commandService) {
        this.readModelService = readModelService;
        this.commandService = commandService;
    }

    public List<ManualScoreHistoryEntryResponse> getManualEntries(Long tripId) {
        return readModelService.getManualEntries(tripId);
    }

    public List<DbScoreHistoryImportCandidateResponse> getImportableDbScoreHistory(Long tripId) {
        return readModelService.getImportableDbScoreHistory(tripId);
    }

    public ManualScoreHistoryEntryResponse createManualEntry(Long tripId, SaveManualScoreHistoryEntryRequest request) {
        return commandService.createManualEntry(tripId, request);
    }

    public List<ManualScoreHistoryEntryResponse> createManualEntries(Long tripId, List<SaveManualScoreHistoryEntryRequest> requests) {
        return commandService.createManualEntries(tripId, requests);
    }

    public ManualScoreHistoryEntryResponse updateManualEntry(Long tripId,
                                                             Long scoreHistoryEntryId,
                                                             SaveManualScoreHistoryEntryRequest request) {
        return commandService.updateManualEntry(tripId, scoreHistoryEntryId, request);
    }

    public void deleteManualEntry(Long tripId, Long scoreHistoryEntryId) {
        commandService.deleteManualEntry(tripId, scoreHistoryEntryId);
    }
}
