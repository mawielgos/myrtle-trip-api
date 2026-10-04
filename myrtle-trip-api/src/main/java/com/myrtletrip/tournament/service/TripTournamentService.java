package com.myrtletrip.tournament.service;

import com.myrtletrip.tournament.dto.SaveTripTournamentSetupRequest;
import com.myrtletrip.tournament.dto.TripTournamentSetupResponse;
import com.myrtletrip.tournament.model.TournamentCompetitionType;
import com.myrtletrip.trip.entity.TripPlannedRound;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripTournamentService {

    public static final String DEFAULT_TOURNAMENT_NAME = "Multi-Round Tournament";
    public static final String DEFAULT_STANDINGS_LABEL = "Tournament Standings";
    public static final String DEFAULT_LOW_NET_NAME = "2-Round Low Net";
    public static final String DEFAULT_LOW_GROSS_NAME = "2-Round Low Gross";

    private final TripTournamentReadModelService readModelService;
    private final TripTournamentCommandService commandService;

    public TripTournamentService(TripTournamentReadModelService readModelService,
                                 TripTournamentCommandService commandService) {
        this.readModelService = readModelService;
        this.commandService = commandService;
    }

    @Transactional(readOnly = true)
    public TripTournamentSetupResponse getTournamentSetup(Long tripId) {
        return readModelService.getTournamentSetup(tripId);
    }

    @Transactional(readOnly = true)
    public String getTournamentName(Long tripId) {
        return readModelService.getTournamentName(tripId);
    }

    @Transactional(readOnly = true)
    public String getStandingsLabel(Long tripId) {
        return readModelService.getStandingsLabel(tripId);
    }

    @Transactional(readOnly = true)
    public String getCompetitionName(Long tripId, TournamentCompetitionType competitionType) {
        return readModelService.getCompetitionName(tripId, competitionType);
    }

    @Transactional(readOnly = true)
    public List<TripPlannedRound> getIncludedTournamentPlannedRounds(Long tripId) {
        return readModelService.getIncludedTournamentPlannedRounds(tripId);
    }

    @Transactional
    public TripTournamentSetupResponse saveTournamentSetup(Long tripId, SaveTripTournamentSetupRequest request) {
        return commandService.saveTournamentSetup(tripId, request);
    }
}
