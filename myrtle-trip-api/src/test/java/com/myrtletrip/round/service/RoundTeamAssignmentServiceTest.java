package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.SaveRoundScrambleSeedingRequest;
import com.myrtletrip.round.dto.ScorecardParticipationRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoundTeamAssignmentServiceTest {

    @Test
    void getAssignmentPage_delegatesToReadModelService() {
        RoundTeamAssignmentReadModelService readModelService = mock(RoundTeamAssignmentReadModelService.class);
        RoundScrambleSeedingService scrambleSeedingService = mock(RoundScrambleSeedingService.class);
        RoundParticipationService participationService = mock(RoundParticipationService.class);
        RoundTeamAssignmentService service = new RoundTeamAssignmentService(
                readModelService,
                scrambleSeedingService,
                participationService
        );
        RoundTeamAssignmentPageResponse page = new RoundTeamAssignmentPageResponse();
        when(readModelService.getAssignmentPage(21L)).thenReturn(page);

        RoundTeamAssignmentPageResponse result = service.getAssignmentPage(21L);

        assertSame(page, result);
        verify(readModelService).getAssignmentPage(21L);
    }

    @Test
    void saveScrambleSeedingRounds_delegatesAndReturnsRefreshedPage() {
        RoundTeamAssignmentReadModelService readModelService = mock(RoundTeamAssignmentReadModelService.class);
        RoundScrambleSeedingService scrambleSeedingService = mock(RoundScrambleSeedingService.class);
        RoundParticipationService participationService = mock(RoundParticipationService.class);
        RoundTeamAssignmentService service = new RoundTeamAssignmentService(
                readModelService,
                scrambleSeedingService,
                participationService
        );
        RoundTeamAssignmentPageResponse page = new RoundTeamAssignmentPageResponse();
        SaveRoundScrambleSeedingRequest request = new SaveRoundScrambleSeedingRequest();
        when(readModelService.getAssignmentPage(21L)).thenReturn(page);

        RoundTeamAssignmentPageResponse result = service.saveScrambleSeedingRounds(21L, request);

        assertSame(page, result);
        verify(scrambleSeedingService).saveScrambleSeedingRounds(21L, request);
        verify(readModelService).getAssignmentPage(21L);
    }

    @Test
    void updateScorecardParticipation_delegatesAndReturnsRefreshedPage() {
        RoundTeamAssignmentReadModelService readModelService = mock(RoundTeamAssignmentReadModelService.class);
        RoundScrambleSeedingService scrambleSeedingService = mock(RoundScrambleSeedingService.class);
        RoundParticipationService participationService = mock(RoundParticipationService.class);
        RoundTeamAssignmentService service = new RoundTeamAssignmentService(
                readModelService,
                scrambleSeedingService,
                participationService
        );
        RoundTeamAssignmentPageResponse page = new RoundTeamAssignmentPageResponse();
        ScorecardParticipationRequest request = new ScorecardParticipationRequest();
        when(readModelService.getAssignmentPage(21L)).thenReturn(page);

        RoundTeamAssignmentPageResponse result =
                service.updateScorecardParticipation(21L, 501L, request);

        assertSame(page, result);
        verify(participationService).updateScorecardParticipation(21L, 501L, request);
        verify(readModelService).getAssignmentPage(21L);
    }
}
