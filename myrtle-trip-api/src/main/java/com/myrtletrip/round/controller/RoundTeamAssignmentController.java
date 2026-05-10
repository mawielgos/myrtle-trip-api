package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
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

    @PutMapping("/{roundId}/team-assignment/scramble-seeding")
    public ResponseEntity<RoundTeamAssignmentPageResponse> saveScrambleSeedingRounds(
            @PathVariable Long roundId,
            @RequestBody SaveRoundScrambleSeedingRequest request
    ) {
        return ResponseEntity.ok(roundTeamAssignmentService.saveScrambleSeedingRounds(roundId, request));
    }
}
