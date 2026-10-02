package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.SaveRoundScrambleSeedingRequest;
import com.myrtletrip.round.dto.ScorecardParticipationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoundTeamAssignmentService {

    private final RoundTeamAssignmentReadModelService readModelService;
    private final RoundScrambleSeedingService roundScrambleSeedingService;
    private final RoundParticipationService roundParticipationService;

    public RoundTeamAssignmentService(
            RoundTeamAssignmentReadModelService readModelService,
            RoundScrambleSeedingService roundScrambleSeedingService,
            RoundParticipationService roundParticipationService
    ) {
        this.readModelService = readModelService;
        this.roundScrambleSeedingService = roundScrambleSeedingService;
        this.roundParticipationService = roundParticipationService;
    }

    @Transactional
    public RoundTeamAssignmentPageResponse getAssignmentPage(Long roundId) {
        return readModelService.getAssignmentPage(roundId);
    }

    @Transactional
    public RoundTeamAssignmentPageResponse saveScrambleSeedingRounds(
            Long roundId,
            SaveRoundScrambleSeedingRequest request
    ) {
        roundScrambleSeedingService.saveScrambleSeedingRounds(roundId, request);
        return getAssignmentPage(roundId);
    }

    @Transactional
    public RoundTeamAssignmentPageResponse updateScorecardParticipation(
            Long roundId,
            Long scorecardId,
            ScorecardParticipationRequest request
    ) {
        roundParticipationService.updateScorecardParticipation(roundId, scorecardId, request);
        return getAssignmentPage(roundId);
    }
}
