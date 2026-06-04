package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.dto.RoundTeeCorrectionRequest;
import com.myrtletrip.round.dto.SaveRoundTeamsRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.service.RoundRecalculationOrchestrationService;
import com.myrtletrip.round.service.RoundTeamService;
import com.myrtletrip.round.service.ScorecardHandicapService;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rounds")
public class RoundTeamController {

    private final RoundTeamService roundTeamService;
    private final ScorecardHandicapService scorecardHandicapService;
    private final RoundRecalculationOrchestrationService roundRecalculationOrchestrationService;
    private final RoundRepository roundRepository;
    private final TripEditingGuardService tripEditingGuardService;

    public RoundTeamController(RoundTeamService roundTeamService,
                               ScorecardHandicapService scorecardHandicapService,
                               RoundRecalculationOrchestrationService roundRecalculationOrchestrationService,
                               RoundRepository roundRepository,
                               TripEditingGuardService tripEditingGuardService) {
        this.roundTeamService = roundTeamService;
        this.scorecardHandicapService = scorecardHandicapService;
        this.roundRecalculationOrchestrationService = roundRecalculationOrchestrationService;
        this.roundRepository = roundRepository;
        this.tripEditingGuardService = tripEditingGuardService;
    }

    @PutMapping("/{roundId}/teams")
    public ResponseEntity<List<RoundTeamResponse>> saveTeams(@PathVariable Long roundId,
                                                             @RequestBody SaveRoundTeamsRequest request) {
        return ResponseEntity.ok(roundTeamService.saveTeams(roundId, request));
    }

    @PutMapping("/{roundId}/tee-corrections")
    public ResponseEntity<Void> saveTeeCorrections(@PathVariable Long roundId,
                                                   @RequestBody List<RoundTeeCorrectionRequest> request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        if (!Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Use Save Assignments before finalization. Tee corrections are only available after a round is finalized.");
        }

        tripEditingGuardService.assertCorrectionAllowedForRound(round);
        scorecardHandicapService.applyTeeCorrections(roundId, request);
        roundRecalculationOrchestrationService.handlePostRoundChange(roundId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roundId}/teams")
    public ResponseEntity<List<RoundTeamResponse>> getTeams(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundTeamService.getTeams(roundId));
    }
}
