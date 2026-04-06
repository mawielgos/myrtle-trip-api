package com.myrtletrip.scoreentry.controller;

import com.myrtletrip.scoreentry.dto.TeamHoleScoreRequest;
import com.myrtletrip.scoreentry.dto.TeamHoleScoreResponse;
import com.myrtletrip.scoreentry.service.TeamHoleScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-scores")
public class TeamHoleScoreController {

    private final TeamHoleScoreService teamHoleScoreService;

    public TeamHoleScoreController(TeamHoleScoreService teamHoleScoreService) {
        this.teamHoleScoreService = teamHoleScoreService;
    }

    @PutMapping("/teams/{roundTeamId}/holes/{holeNumber}")
    public ResponseEntity<Void> updateTeamHoleScore(@PathVariable Long roundTeamId,
                                                    @PathVariable int holeNumber,
                                                    @RequestBody TeamHoleScoreRequest request) {
        if (request.getStrokes() == null) {
            throw new IllegalArgumentException("Strokes are required");
        }

        teamHoleScoreService.updateTeamHoleScore(roundTeamId, holeNumber, request.getStrokes());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/teams/{roundTeamId}")
    public ResponseEntity<List<TeamHoleScoreResponse>> getTeamHoleScores(@PathVariable Long roundTeamId) {
        return ResponseEntity.ok(teamHoleScoreService.getTeamHoleScores(roundTeamId));
    }
}
