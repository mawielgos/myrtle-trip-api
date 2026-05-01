package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.dto.RoundTeeCorrectionRequest;
import com.myrtletrip.round.dto.SaveRoundTeamsRequest;
import com.myrtletrip.round.service.RoundRecalculationOrchestrationService;
import com.myrtletrip.round.service.RoundTeamService;
import com.myrtletrip.round.service.ScorecardHandicapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rounds")
public class RoundTeamController {

    private final RoundTeamService roundTeamService;
    private final ScorecardHandicapService scorecardHandicapService;
    private final RoundRecalculationOrchestrationService roundRecalculationOrchestrationService;

    public RoundTeamController(RoundTeamService roundTeamService,
                               ScorecardHandicapService scorecardHandicapService,
                               RoundRecalculationOrchestrationService roundRecalculationOrchestrationService) {
        this.roundTeamService = roundTeamService;
        this.scorecardHandicapService = scorecardHandicapService;
        this.roundRecalculationOrchestrationService = roundRecalculationOrchestrationService;
    }

    @PutMapping("/{roundId}/teams")
    public ResponseEntity<List<RoundTeamResponse>> saveTeams(@PathVariable Long roundId,
                                                             @RequestBody SaveRoundTeamsRequest request) {
        return ResponseEntity.ok(roundTeamService.saveTeams(roundId, request));
    }

    @PutMapping("/{roundId}/tee-corrections")
    public ResponseEntity<Void> saveTeeCorrections(@PathVariable Long roundId,
                                                   @RequestBody List<RoundTeeCorrectionRequest> request) {
        scorecardHandicapService.applyTeeCorrections(roundId, request);
        roundRecalculationOrchestrationService.handlePostRoundChange(roundId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roundId}/teams")
    public ResponseEntity<List<RoundTeamResponse>> getTeams(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundTeamService.getTeams(roundId));
    }
}
