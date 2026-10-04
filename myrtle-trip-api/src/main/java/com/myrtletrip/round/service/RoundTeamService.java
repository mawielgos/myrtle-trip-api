package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.dto.SaveRoundTeamsRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoundTeamService {

    private final RoundTeamCommandService roundTeamCommandService;
    private final RoundTeamReadModelService roundTeamReadModelService;

    public RoundTeamService(
            RoundTeamCommandService roundTeamCommandService,
            RoundTeamReadModelService roundTeamReadModelService
    ) {
        this.roundTeamCommandService = roundTeamCommandService;
        this.roundTeamReadModelService = roundTeamReadModelService;
    }

    @Transactional
    public List<RoundTeamResponse> saveTeams(Long roundId, SaveRoundTeamsRequest request) {
        roundTeamCommandService.saveTeams(roundId, request);
        return roundTeamReadModelService.getTeams(roundId);
    }

    @Transactional(readOnly = true)
    public List<RoundTeamResponse> getTeams(Long roundId) {
        return roundTeamReadModelService.getTeams(roundId);
    }
}
