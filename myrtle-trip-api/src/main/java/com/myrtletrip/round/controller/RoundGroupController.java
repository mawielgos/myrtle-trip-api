package com.myrtletrip.round.controller;

import com.myrtletrip.round.dto.RoundGroupAssignmentRequest;
import com.myrtletrip.round.dto.RoundGroupPageResponse;
import com.myrtletrip.round.service.RoundGroupService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rounds")
public class RoundGroupController {

    private final RoundGroupService roundGroupService;

    public RoundGroupController(RoundGroupService roundGroupService) {
        this.roundGroupService = roundGroupService;
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
        return roundGroupService.saveRoundGroups(roundId, request);
    }
}
