package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundScorecardSummaryResponse;
import com.myrtletrip.round.dto.RoundStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoundQueryService {

    private final RoundStatusReadModelService roundStatusReadModelService;
    private final RoundScorecardSummaryReadModelService roundScorecardSummaryReadModelService;

    public RoundQueryService(
            RoundStatusReadModelService roundStatusReadModelService,
            RoundScorecardSummaryReadModelService roundScorecardSummaryReadModelService
    ) {
        this.roundStatusReadModelService = roundStatusReadModelService;
        this.roundScorecardSummaryReadModelService = roundScorecardSummaryReadModelService;
    }

    @Transactional(readOnly = true)
    public RoundStatusResponse getRoundStatus(Long roundId) {
        return roundStatusReadModelService.getRoundStatus(roundId);
    }

    @Transactional(readOnly = true)
    public List<RoundScorecardSummaryResponse> getRoundScorecards(Long roundId) {
        return roundScorecardSummaryReadModelService.getRoundScorecards(roundId);
    }
}
