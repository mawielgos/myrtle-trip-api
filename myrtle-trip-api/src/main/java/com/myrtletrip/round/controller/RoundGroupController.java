package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundGroupAssignmentRequest;
import com.myrtletrip.round.dto.RoundGroupPageResponse;
import com.myrtletrip.round.service.RoundGroupService;
import com.myrtletrip.round.service.RoundRecalculationOrchestrationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rounds")
public class RoundGroupController {

    private final RoundGroupService roundGroupService;
    private final RoundRecalculationOrchestrationService roundRecalculationOrchestrationService;

    public RoundGroupController(RoundGroupService roundGroupService,
                                RoundRecalculationOrchestrationService roundRecalculationOrchestrationService) {
        this.roundGroupService = roundGroupService;
        this.roundRecalculationOrchestrationService = roundRecalculationOrchestrationService;
    }

    @GetMapping("/{roundId}/groups")
    public RoundGroupPageResponse getRoundGroups(@PathVariable Long roundId) {
        return roundGroupService.getRoundGroups(roundId);
    }

    @PutMapping("/{roundId}/groups")
    public RoundGroupPageResponse saveRoundGroups(
            @PathVariable Long roundId,
            @RequestBody RoundGroupAssignmentRequest request
    ) {
        RoundGroupPageResponse response = roundGroupService.saveRoundGroups(roundId, request);
        roundRecalculationOrchestrationService.handlePostRoundChange(roundId);
        return response;
    }
}
