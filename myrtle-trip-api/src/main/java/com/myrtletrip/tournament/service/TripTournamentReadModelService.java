package com.myrtletrip.tournament.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.tournament.dto.TripTournamentRoundResponse;
import com.myrtletrip.tournament.dto.TripTournamentSetupResponse;
import com.myrtletrip.tournament.entity.TripTournament;
import com.myrtletrip.tournament.entity.TripTournamentRound;
import com.myrtletrip.tournament.model.TournamentCompetitionType;
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
import java.util.List;
import java.util.Map;

@Service
public class TripTournamentReadModelService {

    private final TripRepository tripRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final TripTournamentRepository tripTournamentRepository;
    private final TripTournamentRoundRepository tripTournamentRoundRepository;
    private final CourseRepository courseRepository;

    public TripTournamentReadModelService(TripRepository tripRepository,
                                          TripPlannedRoundRepository tripPlannedRoundRepository,
                                          TripTournamentRepository tripTournamentRepository,
                                          TripTournamentRoundRepository tripTournamentRoundRepository,
                                          CourseRepository courseRepository) {
        this.tripRepository = tripRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.tripTournamentRepository = tripTournamentRepository;
        this.tripTournamentRoundRepository = tripTournamentRoundRepository;
        this.courseRepository = courseRepository;
    }

    public TripTournamentSetupResponse getTournamentSetup(Long tripId) {
        Trip trip = loadTrip(tripId);
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        return toResponse(trip, tournament);
    }

    public String getTournamentName(Long tripId) {
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (tournament == null || tournament.getName() == null || tournament.getName().isBlank()) {
            return TripTournamentService.DEFAULT_TOURNAMENT_NAME;
        }
        return tournament.getName();
    }

    public String getStandingsLabel(Long tripId) {
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (tournament == null || tournament.getStandingsLabel() == null || tournament.getStandingsLabel().isBlank()) {
            return TripTournamentService.DEFAULT_STANDINGS_LABEL;
        }
        return tournament.getStandingsLabel();
    }

    public String getCompetitionName(Long tripId, TournamentCompetitionType competitionType) {
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (competitionType != null && !competitionType.isNet()) {
            if (tournament != null && tournament.getLowGrossName() != null && !tournament.getLowGrossName().isBlank()) {
                return tournament.getLowGrossName();
            }
            return TripTournamentService.DEFAULT_LOW_GROSS_NAME;
        }
        if (tournament != null && tournament.getLowNetName() != null && !tournament.getLowNetName().isBlank()) {
            return tournament.getLowNetName();
        }
        return TripTournamentService.DEFAULT_LOW_NET_NAME;
    }

    public List<TripPlannedRound> getIncludedTournamentPlannedRounds(Long tripId) {
        Trip trip = loadTrip(tripId);
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (tournament == null) return getLegacyIncludedTournamentPlannedRounds(trip);
        if (!Boolean.TRUE.equals(tournament.getEnabled())) return new ArrayList<>();

        List<TripTournamentRound> tournamentRounds = tripTournamentRoundRepository
                .findByTournament_IdOrderBySortOrderAsc(tournament.getId());
        List<TripPlannedRound> included = new ArrayList<>();
        for (TripTournamentRound tournamentRound : tournamentRounds) {
            TripPlannedRound plannedRound = tournamentRound.getPlannedRound();
            if (isEligibleTournamentRound(trip, plannedRound)) included.add(plannedRound);
        }
        return included.isEmpty() ? getLegacyIncludedTournamentPlannedRounds(trip) : included;
    }

    TripTournamentSetupResponse toResponse(Trip trip, TripTournament tournament) {
        TripTournamentSetupResponse response = new TripTournamentSetupResponse();
        response.setTripId(trip.getId());
        response.setReadOnly(isReadOnly(trip));

        boolean enabled = tournament != null && Boolean.TRUE.equals(tournament.getEnabled());
        response.setTournamentId(tournament != null ? tournament.getId() : null);
        response.setEnabled(enabled);
        response.setName(tournament != null && tournament.getName() != null && !tournament.getName().isBlank()
                ? tournament.getName() : TripTournamentService.DEFAULT_TOURNAMENT_NAME);
        response.setStandingsLabel(tournament != null && tournament.getStandingsLabel() != null && !tournament.getStandingsLabel().isBlank()
                ? tournament.getStandingsLabel() : TripTournamentService.DEFAULT_STANDINGS_LABEL);
        response.setLowNetEnabled(tournament == null || tournament.getLowNetEnabled() == null || Boolean.TRUE.equals(tournament.getLowNetEnabled()));
        response.setLowGrossEnabled(tournament != null && Boolean.TRUE.equals(tournament.getLowGrossEnabled()));
        response.setLowNetName(tournament != null && tournament.getLowNetName() != null && !tournament.getLowNetName().isBlank()
                ? tournament.getLowNetName() : TripTournamentService.DEFAULT_LOW_NET_NAME);
        response.setLowGrossName(tournament != null && tournament.getLowGrossName() != null && !tournament.getLowGrossName().isBlank()
                ? tournament.getLowGrossName() : TripTournamentService.DEFAULT_LOW_GROSS_NAME);

        Map<Long, Integer> includedSortOrderByPlannedRoundId = new HashMap<>();
        if (tournament != null && tournament.getId() != null && Boolean.TRUE.equals(tournament.getEnabled())) {
            for (TripTournamentRound tournamentRound : tripTournamentRoundRepository.findByTournament_IdOrderBySortOrderAsc(tournament.getId())) {
                TripPlannedRound plannedRound = tournamentRound.getPlannedRound();
                if (isEligibleTournamentRound(trip, plannedRound) && plannedRound.getId() != null) {
                    includedSortOrderByPlannedRoundId.put(plannedRound.getId(), tournamentRound.getSortOrder());
                }
            }
        }
        if (includedSortOrderByPlannedRoundId.isEmpty() && (tournament == null || Boolean.TRUE.equals(tournament.getEnabled()))) {
            int legacySortOrder = 1;
            for (TripPlannedRound plannedRound : getLegacyIncludedTournamentPlannedRounds(trip)) {
                if (plannedRound.getId() != null) includedSortOrderByPlannedRoundId.put(plannedRound.getId(), legacySortOrder++);
            }
        }

        List<TripTournamentRoundResponse> roundResponses = new ArrayList<>();
        for (TripPlannedRound plannedRound : loadActivePlannedRounds(trip)) {
            TripTournamentRoundResponse rr = new TripTournamentRoundResponse();
            rr.setPlannedRoundId(plannedRound.getId());
            rr.setRoundNumber(plannedRound.getRoundNumber());
            rr.setRoundDate(plannedRound.getRoundDate());
            rr.setFormat(plannedRound.getFormat() != null ? plannedRound.getFormat().name() : null);
            rr.setCourseId(plannedRound.getCourseId());
            rr.setCourseName(resolveCourseName(plannedRound.getCourseId()));
            rr.setConfigured(isConfiguredTournamentRound(plannedRound));
            rr.setIncluded(includedSortOrderByPlannedRoundId.containsKey(plannedRound.getId()));
            rr.setSortOrder(includedSortOrderByPlannedRoundId.get(plannedRound.getId()));
            roundResponses.add(rr);
        }
        response.setRounds(roundResponses);
        return response;
    }

    private List<TripPlannedRound> getLegacyIncludedTournamentPlannedRounds(Trip trip) {
        List<TripPlannedRound> included = new ArrayList<>();
        for (TripPlannedRound plannedRound : loadActivePlannedRounds(trip)) {
            if (Boolean.TRUE.equals(plannedRound.getIncludeInFourDayStandings()) && isEligibleTournamentRound(trip, plannedRound)) {
                included.add(plannedRound);
            }
        }
        return included;
    }

    private List<TripPlannedRound> loadActivePlannedRounds(Trip trip) {
        List<TripPlannedRound> active = new ArrayList<>();
        int count = resolvePlannedRoundCount(trip != null ? trip.getPlannedRoundCount() : null);
        for (TripPlannedRound plannedRound : tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip)) {
            if (plannedRound == null || plannedRound.getRoundNumber() == null) continue;
            if (plannedRound.getRoundNumber() < 1 || plannedRound.getRoundNumber() > count) continue;
            active.add(plannedRound);
        }
        return active;
    }

    private boolean isEligibleTournamentRound(Trip trip, TripPlannedRound plannedRound) {
        return plannedRound != null
                && plannedRound.getRoundNumber() != null
                && plannedRound.getRoundNumber() >= 1
                && plannedRound.getRoundNumber() <= resolvePlannedRoundCount(trip != null ? trip.getPlannedRoundCount() : null)
                && plannedRoundSupportsIndividualTournament(plannedRound);
    }

    private boolean isConfiguredTournamentRound(TripPlannedRound plannedRound) {
        return plannedRound != null
                && plannedRound.getRoundDate() != null
                && plannedRoundSupportsIndividualTournament(plannedRound)
                && plannedRound.getCourseId() != null
                && plannedRound.getStandardTeeId() != null;
    }

    private boolean plannedRoundSupportsIndividualTournament(TripPlannedRound plannedRound) {
        return plannedRound != null && plannedRound.getFormat() != null && !"TEAM_SCRAMBLE".equals(plannedRound.getFormat().name());
    }

    private int resolvePlannedRoundCount(Integer plannedRoundCount) {
        return plannedRoundCount == null || plannedRoundCount < 1 ? 5 : plannedRoundCount;
    }

    private String resolveCourseName(Long courseId) {
        if (courseId == null) return null;
        Course course = courseRepository.findById(courseId).orElse(null);
        return course != null ? course.getName() : null;
    }

    private Trip loadTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
    }

    private boolean isReadOnly(Trip trip) {
        return TripStatus.COMPLETE.equals(trip.getStatus()) && !trip.isCorrectionMode();
    }
}
