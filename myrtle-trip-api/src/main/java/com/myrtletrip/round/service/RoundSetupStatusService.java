package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundGroupPageResponse;
import com.myrtletrip.round.dto.RoundReadinessResponse;
import com.myrtletrip.round.dto.RoundSetupStatusResponse;
import com.myrtletrip.round.dto.RoundStatusResponse;
import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoundSetupStatusService {

    private final RoundQueryService roundQueryService;
    private final RoundReadinessService roundReadinessService;
    private final RoundGroupService roundGroupService;
    private final RoundTeamAssignmentService roundTeamAssignmentService;

    public RoundSetupStatusService(
            RoundQueryService roundQueryService,
            RoundReadinessService roundReadinessService,
            RoundGroupService roundGroupService,
            RoundTeamAssignmentService roundTeamAssignmentService
    ) {
        this.roundQueryService = roundQueryService;
        this.roundReadinessService = roundReadinessService;
        this.roundGroupService = roundGroupService;
        this.roundTeamAssignmentService = roundTeamAssignmentService;
    }

    @Transactional(readOnly = true)
    public RoundSetupStatusResponse getSetupStatus(Long roundId) {
        RoundStatusResponse round = roundQueryService.getRoundStatus(roundId);
        RoundReadinessResponse readiness = roundReadinessService.getReadiness(roundId);
        RoundGroupPageResponse groups = roundGroupService.getRoundGroups(roundId);
        RoundTeamAssignmentPageResponse teamAssignment =
                roundTeamAssignmentService.getAssignmentPage(roundId);

        RoundSetupStatusResponse response = new RoundSetupStatusResponse();
        response.setRoundId(roundId);
        response.setRound(round);
        response.setReadiness(readiness);
        response.setGroups(groups);
        response.setTeamAssignment(teamAssignment);

        return response;
    }
}

