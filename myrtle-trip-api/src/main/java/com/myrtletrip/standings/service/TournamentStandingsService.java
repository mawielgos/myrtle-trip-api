package com.myrtletrip.standings.service;

import com.myrtletrip.standings.dto.TournamentStandingsResponse;
import com.myrtletrip.tournament.model.TournamentCompetitionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentStandingsService {

    private final TournamentStandingsCalculationService calculationService;
    private final TournamentStandingsPayoutService payoutService;

    public TournamentStandingsService(TournamentStandingsCalculationService calculationService,
                                      TournamentStandingsPayoutService payoutService) {
        this.calculationService = calculationService;
        this.payoutService = payoutService;
    }

    @Transactional(readOnly = true)
    public TournamentStandingsResponse getTournamentStandings(Long tripId) {
        return getTournamentStandings(tripId, TournamentCompetitionType.LOW_NET.name());
    }

    @Transactional(readOnly = true)
    public TournamentStandingsResponse getTournamentStandings(Long tripId, String competition) {
        TournamentCompetitionType competitionType = TournamentCompetitionType.parse(competition);
        TournamentStandingsResponse response = calculationService.calculate(tripId, competitionType);
        payoutService.apply(
                tripId,
                competitionType,
                Boolean.TRUE.equals(response.getLeaderboardFinal()),
                response.getRows()
        );
        return response;
    }
}
