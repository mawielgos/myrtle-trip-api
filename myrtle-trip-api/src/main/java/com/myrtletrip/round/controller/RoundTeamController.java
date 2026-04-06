package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.dto.SaveRoundTeamsRequest;
import com.myrtletrip.round.service.RoundTeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rounds")
public class RoundTeamController {

    private final RoundTeamService roundTeamService;

    public RoundTeamController(RoundTeamService roundTeamService) {
        this.roundTeamService = roundTeamService;
    }

    @PutMapping("/{roundId}/teams")
    public ResponseEntity<List<RoundTeamResponse>> saveTeams(@PathVariable Long roundId,
                                                             @RequestBody SaveRoundTeamsRequest request) {
        return ResponseEntity.ok(roundTeamService.saveTeams(roundId, request));
    }

    @GetMapping("/{roundId}/teams")
    public ResponseEntity<List<RoundTeamResponse>> getTeams(@PathVariable Long roundId) {
        return ResponseEntity.ok(roundTeamService.getTeams(roundId));
    }
}
