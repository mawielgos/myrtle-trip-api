package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.ScorecardParticipationRequest;
import com.myrtletrip.round.dto.SaveRoundScrambleSeedingRequest;
import com.myrtletrip.round.service.RoundTeamAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rounds")
public class RoundTeamAssignmentController {

    private final RoundTeamAssignmentService roundTeamAssignmentService;

    public RoundTeamAssignmentController(RoundTeamAssignmentService roundTeamAssignmentService) {
        this.roundTeamAssignmentService = roundTeamAssignmentService;
    }

    @GetMapping("/{roundId}/team-assignment")
    public ResponseEntity<RoundTeamAssignmentPageResponse> getTeamAssignmentPage(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundTeamAssignmentService.getAssignmentPage(roundId));
    }

    @PatchMapping("/{roundId}/team-assignment/scorecards/{scorecardId}/participation")
    public ResponseEntity<RoundTeamAssignmentPageResponse> updateScorecardParticipation(
            @PathVariable Long roundId,
            @PathVariable Long scorecardId,
            @RequestBody ScorecardParticipationRequest request
    ) {
        return ResponseEntity.ok(roundTeamAssignmentService.updateScorecardParticipation(roundId, scorecardId, request));
    }

    @PutMapping("/{roundId}/team-assignment/scramble-seeding")
    public ResponseEntity<RoundTeamAssignmentPageResponse> saveScrambleSeedingRounds(
            @PathVariable Long roundId,
            @RequestBody SaveRoundScrambleSeedingRequest request
    ) {
        return ResponseEntity.ok(roundTeamAssignmentService.saveScrambleSeedingRounds(roundId, request));
    }
}
