package com.myrtletrip.tournament.service;

import com.myrtletrip.tournament.dto.SaveTripTournamentSetupRequest;
import com.myrtletrip.tournament.dto.TripTournamentSetupResponse;
import com.myrtletrip.tournament.entity.TripTournament;
import com.myrtletrip.tournament.entity.TripTournamentRound;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.tournament.repository.TripTournamentRoundRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TripTournamentCommandService {

    private final TripTournamentReadModelService readModelService;
    private final TripRepository tripRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final TripTournamentRepository tripTournamentRepository;
    private final TripTournamentRoundRepository tripTournamentRoundRepository;

    public TripTournamentCommandService(TripTournamentReadModelService readModelService,
                                        TripRepository tripRepository,
                                        TripPlannedRoundRepository tripPlannedRoundRepository,
                                        TripTournamentRepository tripTournamentRepository,
                                        TripTournamentRoundRepository tripTournamentRoundRepository) {
        this.readModelService = readModelService;
        this.tripRepository = tripRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.tripTournamentRepository = tripTournamentRepository;
        this.tripTournamentRoundRepository = tripTournamentRoundRepository;
    }

    public TripTournamentSetupResponse saveTournamentSetup(Long tripId, SaveTripTournamentSetupRequest request) {
        Trip trip = loadTrip(tripId);
        assertEditable(trip);

        if (request == null) {
            throw new IllegalArgumentException("Tournament setup request is required.");
        }

        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        boolean lowNetEnabled = !enabled || request.getLowNetEnabled() == null ? true : Boolean.TRUE.equals(request.getLowNetEnabled());
        boolean lowGrossEnabled = enabled && Boolean.TRUE.equals(request.getLowGrossEnabled());
        String name = clean(request.getName(), TripTournamentService.DEFAULT_TOURNAMENT_NAME);
        String standingsLabel = clean(request.getStandingsLabel(), TripTournamentService.DEFAULT_STANDINGS_LABEL);
        String lowNetName = clean(request.getLowNetName(), TripTournamentService.DEFAULT_LOW_NET_NAME);
        String lowGrossName = clean(request.getLowGrossName(), TripTournamentService.DEFAULT_LOW_GROSS_NAME);
        List<Long> includedIds = request.getIncludedPlannedRoundIds() != null
                ? request.getIncludedPlannedRoundIds()
                : new ArrayList<Long>();

        List<TripPlannedRound> plannedRounds = loadActivePlannedRounds(trip);
        Map<Long, TripPlannedRound> plannedById = new HashMap<Long, TripPlannedRound>();
        for (TripPlannedRound plannedRound : plannedRounds) {
            plannedById.put(plannedRound.getId(), plannedRound);
        }

        List<TripPlannedRound> includedRounds = new ArrayList<TripPlannedRound>();
        Set<Long> seen = new HashSet<Long>();
        for (Long plannedRoundId : includedIds) {
            if (plannedRoundId == null || !seen.add(plannedRoundId)) {
                continue;
            }
            TripPlannedRound plannedRound = plannedById.get(plannedRoundId);
            if (plannedRound == null) {
                throw new IllegalArgumentException("Planned round does not belong to this trip: " + plannedRoundId);
            }
            if (!isConfiguredTournamentRound(plannedRound)) {
                throw new IllegalArgumentException("Round " + plannedRound.getRoundNumber() + " is not fully configured for tournament setup.");
            }
            if (!plannedRoundSupportsIndividualTournament(plannedRound)) {
                throw new IllegalArgumentException("Scramble rounds cannot be included in the multi-round tournament.");
            }
            includedRounds.add(plannedRound);
        }

        if (enabled && !lowNetEnabled && !lowGrossEnabled) {
            throw new IllegalArgumentException("Select at least one tournament competition: Low Net, Low Gross, or both.");
        }

        if (enabled && includedRounds.size() < 2) {
            throw new IllegalArgumentException("A multi-round tournament needs at least two included rounds.");
        }

        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (tournament == null) {
            tournament = new TripTournament();
            tournament.setTrip(trip);
        }
        tournament.setEnabled(enabled);
        tournament.setName(name);
        tournament.setStandingsLabel(standingsLabel);
        tournament.setLowNetEnabled(enabled ? lowNetEnabled : true);
        tournament.setLowGrossEnabled(enabled && lowGrossEnabled);
        tournament.setLowNetName(lowNetName);
        tournament.setLowGrossName(lowGrossName);
        tournament = tripTournamentRepository.save(tournament);

        if (tournament.getId() != null) {
            tripTournamentRoundRepository.deleteByTournament_Id(tournament.getId());
            tripTournamentRoundRepository.flush();
        }
        tournament.getRounds().clear();

        int sortOrder = 1;
        if (enabled) {
            for (TripPlannedRound includedRound : includedRounds) {
                TripTournamentRound tournamentRound = new TripTournamentRound();
                tournamentRound.setTournament(tournament);
                tournamentRound.setPlannedRound(includedRound);
                tournamentRound.setSortOrder(sortOrder);
                tournament.getRounds().add(tournamentRound);
                sortOrder++;
            }
        }

        tripTournamentRepository.save(tournament);
        syncLegacyPlannedRoundFlags(plannedRounds, enabled ? includedRounds : new ArrayList<TripPlannedRound>());

        return readModelService.toResponse(trip, tournament);
    }

    private List<TripPlannedRound> loadActivePlannedRounds(Trip trip) {
        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);
        List<TripPlannedRound> active = new ArrayList<TripPlannedRound>();
        int plannedRoundCount = resolvePlannedRoundCount(trip != null ? trip.getPlannedRoundCount() : null);
        for (TripPlannedRound plannedRound : plannedRounds) {
            if (plannedRound == null || plannedRound.getRoundNumber() == null) continue;
            if (plannedRound.getRoundNumber() < 1 || plannedRound.getRoundNumber() > plannedRoundCount) continue;
            active.add(plannedRound);
        }
        return active;
    }

    private int resolvePlannedRoundCount(Integer plannedRoundCount) {
        return plannedRoundCount == null || plannedRoundCount < 1 ? 5 : plannedRoundCount;
    }

    private void syncLegacyPlannedRoundFlags(List<TripPlannedRound> plannedRounds, List<TripPlannedRound> includedRounds) {
        Set<Long> includedIds = new HashSet<Long>();
        for (TripPlannedRound includedRound : includedRounds) includedIds.add(includedRound.getId());
        for (TripPlannedRound plannedRound : plannedRounds) {
            plannedRound.setIncludeInFourDayStandings(includedIds.contains(plannedRound.getId()));
        }
        tripPlannedRoundRepository.saveAll(plannedRounds);
    }

    private boolean isConfiguredTournamentRound(TripPlannedRound plannedRound) {
        return plannedRound != null
                && plannedRound.getRoundDate() != null
                && plannedRoundSupportsIndividualTournament(plannedRound)
                && plannedRound.getCourseId() != null
                && plannedRound.getStandardTeeId() != null;
    }

    private boolean plannedRoundSupportsIndividualTournament(TripPlannedRound plannedRound) {
        return plannedRound != null
                && plannedRound.getFormat() != null
                && !"TEAM_SCRAMBLE".equals(plannedRound.getFormat().name());
    }

    private Trip loadTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
    }

    private void assertEditable(Trip trip) {
        if (TripStatus.COMPLETE.equals(trip.getStatus()) && !trip.isCorrectionMode()) {
            throw new IllegalStateException("Tournament setup cannot be changed after the trip is complete unless Correction Mode is enabled.");
        }
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
