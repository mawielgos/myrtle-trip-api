package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.ScorecardParticipationRequest;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class RoundTeamAssignmentServiceTest {

    @Test
    void updateScorecardParticipation_delegatesAndReturnsRefreshedPage() {
        RoundParticipationService participationService = mock(RoundParticipationService.class);
        RoundTeamAssignmentPageResponse page = new RoundTeamAssignmentPageResponse();

        RoundTeamAssignmentService service = spy(new RoundTeamAssignmentService(
                mock(RoundRepository.class),
                mock(RoundTeamRepository.class),
                mock(RoundTeamPlayerRepository.class),
                mock(RoundTeeRepository.class),
                mock(ScorecardRepository.class),
                mock(RoundTeeResolver.class),
                mock(RoundTeeProvisioningService.class),
                mock(TripHandicapService.class),
                mock(TripPlayerRepository.class),
                mock(RoundCapabilityService.class),
                mock(RoundEventCapabilityService.class),
                mock(RoundScrambleSeedingService.class),
                participationService
        ));

        doReturn(page).when(service).getAssignmentPage(21L);
        ScorecardParticipationRequest request = new ScorecardParticipationRequest();

        RoundTeamAssignmentPageResponse result =
                service.updateScorecardParticipation(21L, 501L, request);

        assertSame(page, result);
        verify(participationService).updateScorecardParticipation(21L, 501L, request);
    }
}
