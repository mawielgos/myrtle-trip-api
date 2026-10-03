package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundGroupAssignmentRequest;
import com.myrtletrip.round.dto.RoundGroupPageResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

@Service
@Transactional
public class RoundGroupService {

    private final RoundGroupReadModelService roundGroupReadModelService;
    private final RoundGroupCommandService roundGroupCommandService;

    public RoundGroupService(
            RoundGroupReadModelService roundGroupReadModelService,
            RoundGroupCommandService roundGroupCommandService
    ) {
        this.roundGroupReadModelService = roundGroupReadModelService;
        this.roundGroupCommandService = roundGroupCommandService;
    }

    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public RoundGroupPageResponse getRoundGroups(Long roundId) {
        return roundGroupReadModelService.getRoundGroups(roundId);
    }

    public RoundGroupPageResponse saveRoundGroups(Long roundId, RoundGroupAssignmentRequest request) {
        roundGroupCommandService.saveRoundGroups(roundId, request);
        return roundGroupReadModelService.getRoundGroups(roundId);
    }
}
