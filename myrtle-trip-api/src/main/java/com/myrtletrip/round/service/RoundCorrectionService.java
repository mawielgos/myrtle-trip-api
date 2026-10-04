package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundCorrectionRequest;
import com.myrtletrip.round.dto.RoundCorrectionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoundCorrectionService {

    private final RoundCorrectionExecutionService roundCorrectionExecutionService;

    public RoundCorrectionService(RoundCorrectionExecutionService roundCorrectionExecutionService) {
        this.roundCorrectionExecutionService = roundCorrectionExecutionService;
    }

    @Transactional
    public RoundCorrectionResponse applyCorrection(Long roundId, RoundCorrectionRequest request) {
        return roundCorrectionExecutionService.applyCorrection(roundId, request);
    }
}
