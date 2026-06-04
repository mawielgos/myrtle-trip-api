package com.myrtletrip.tournament.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.tournament.dto.SaveTripTournamentSetupRequest;
import com.myrtletrip.tournament.dto.TripTournamentRoundResponse;
import com.myrtletrip.tournament.dto.TripTournamentSetupResponse;
import com.myrtletrip.tournament.entity.TripTournament;
import com.myrtletrip.tournament.entity.TripTournamentRound;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.tournament.repository.TripTournamentRoundRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.entity.TripPlannedRoundEvent;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TripTournamentService {

    public static final String DEFAULT_TOURNAMENT_NAME = "Multi-Round Tournament";
    public static final String DEFAULT_STANDINGS_LABEL = "Tournament Standings";
    public static final String DEFAULT_LOW_NET_NAME = "2-Round Low Net";
    public static final String DEFAULT_LOW_GROSS_NAME = "2-Round Low Gross";

    private final TripRepository tripRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final TripTournamentRepository tripTournamentRepository;
    private final TripTournamentRoundRepository tripTournamentRoundRepository;
    private final TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    private final CourseRepository courseRepository;

    public TripTournamentService(TripRepository tripRepository,
                                 TripPlannedRoundRepository tripPlannedRoundRepository,
                                 TripTournamentRepository tripTournamentRepository,
                                 TripTournamentRoundRepository tripTournamentRoundRepository,
                                 TripPlannedRoundEventRepository tripPlannedRoundEventRepository,
                                 CourseRepository courseRepository) {
        this.tripRepository = tripRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.tripTournamentRepository = tripTournamentRepository;
        this.tripTournamentRoundRepository = tripTournamentRoundRepository;
        this.tripPlannedRoundEventRepository = tripPlannedRoundEventRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public TripTournamentSetupResponse getTournamentSetup(Long tripId) {
        Trip trip = loadTrip(tripId);
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        return toResponse(trip, tournament);
    }

    @Transactional
    public TripTournamentSetupResponse saveTournamentSetup(Long tripId, SaveTripTournamentSetupRequest request) {
        Trip trip = loadTrip(tripId);
        assertEditable(trip);

        if (request == null) {
            throw new IllegalArgumentException("Tournament setup request is required.");
        }

        boolean enabled = Boolean.TRUE.equals(request.getEnabled());
        boolean lowNetEnabled = !enabled || request.getLowNetEnabled() == null ? true : Boolean.TRUE.equals(request.getLowNetEnabled());
        boolean lowGrossEnabled = enabled && Boolean.TRUE.equals(request.getLowGrossEnabled());
        String name = clean(request.getName(), DEFAULT_TOURNAMENT_NAME);
        String standingsLabel = clean(request.getStandingsLabel(), DEFAULT_STANDINGS_LABEL);
        String lowNetName = clean(request.getLowNetName(), DEFAULT_LOW_NET_NAME);
        String lowGrossName = clean(request.getLowGrossName(), DEFAULT_LOW_GROSS_NAME);
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

        return toResponse(trip, tournament);
    }

    @Transactional(readOnly = true)
    public String getTournamentName(Long tripId) {
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (tournament == null || tournament.getName() == null || tournament.getName().isBlank()) {
            return DEFAULT_TOURNAMENT_NAME;
        }
        return tournament.getName();
    }

    @Transactional(readOnly = true)
    public String getStandingsLabel(Long tripId) {
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (tournament == null || tournament.getStandingsLabel() == null || tournament.getStandingsLabel().isBlank()) {
            return DEFAULT_STANDINGS_LABEL;
        }
        return tournament.getStandingsLabel();
    }

    @Transactional(readOnly = true)
    public String getCompetitionName(Long tripId, com.myrtletrip.tournament.model.TournamentCompetitionType competitionType) {
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (competitionType != null && !competitionType.isNet()) {
            if (tournament != null && tournament.getLowGrossName() != null && !tournament.getLowGrossName().isBlank()) {
                return tournament.getLowGrossName();
            }
            return DEFAULT_LOW_GROSS_NAME;
        }
        if (tournament != null && tournament.getLowNetName() != null && !tournament.getLowNetName().isBlank()) {
            return tournament.getLowNetName();
        }
        return DEFAULT_LOW_NET_NAME;
    }

    @Transactional(readOnly = true)
    public List<TripPlannedRound> getIncludedTournamentPlannedRounds(Long tripId) {
        Trip trip = loadTrip(tripId);
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);

        if (tournament == null) {
            return getLegacyIncludedTournamentPlannedRounds(trip);
        }
        if (!Boolean.TRUE.equals(tournament.getEnabled())) {
            return new ArrayList<TripPlannedRound>();
        }

        List<TripTournamentRound> tournamentRounds = tripTournamentRoundRepository.findByTournament_IdOrderBySortOrderAsc(tournament.getId());
        List<TripPlannedRound> included = new ArrayList<TripPlannedRound>();
        for (TripTournamentRound tournamentRound : tournamentRounds) {
            TripPlannedRound plannedRound = tournamentRound.getPlannedRound();
            if (!isEligibleTournamentRound(trip, plannedRound)) {
                continue;
            }
            included.add(plannedRound);
        }

        if (included.isEmpty()) {
            return getLegacyIncludedTournamentPlannedRounds(trip);
        }
        return included;
    }


    private List<TripPlannedRound> getLegacyIncludedTournamentPlannedRounds(Trip trip) {
        List<TripPlannedRound> plannedRounds = loadActivePlannedRounds(trip);
        List<TripPlannedRound> included = new ArrayList<TripPlannedRound>();
        for (TripPlannedRound plannedRound : plannedRounds) {
            if (Boolean.TRUE.equals(plannedRound.getIncludeInFourDayStandings())
                    && isEligibleTournamentRound(trip, plannedRound)) {
                included.add(plannedRound);
            }
        }
        return included;
    }

    private List<TripPlannedRound> loadActivePlannedRounds(Trip trip) {
        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);
        List<TripPlannedRound> active = new ArrayList<TripPlannedRound>();
        int plannedRoundCount = resolvePlannedRoundCount(trip != null ? trip.getPlannedRoundCount() : null);

        for (TripPlannedRound plannedRound : plannedRounds) {
            if (plannedRound == null || plannedRound.getRoundNumber() == null) {
                continue;
            }
            if (plannedRound.getRoundNumber() < 1 || plannedRound.getRoundNumber() > plannedRoundCount) {
                continue;
            }
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

    private int resolvePlannedRoundCount(Integer plannedRoundCount) {
        if (plannedRoundCount == null || plannedRoundCount < 1) {
            return 5;
        }
        return plannedRoundCount;
    }

    private void syncLegacyPlannedRoundFlags(List<TripPlannedRound> plannedRounds, List<TripPlannedRound> includedRounds) {
        Set<Long> includedIds = new HashSet<Long>();
        for (TripPlannedRound includedRound : includedRounds) {
            includedIds.add(includedRound.getId());
        }

        for (TripPlannedRound plannedRound : plannedRounds) {
            plannedRound.setIncludeInFourDayStandings(includedIds.contains(plannedRound.getId()));
        }
        tripPlannedRoundRepository.saveAll(plannedRounds);
    }

    private TripTournamentSetupResponse toResponse(Trip trip, TripTournament tournament) {
        TripTournamentSetupResponse response = new TripTournamentSetupResponse();
        response.setTripId(trip.getId());
        response.setReadOnly(isReadOnly(trip));

        boolean enabled = tournament != null && Boolean.TRUE.equals(tournament.getEnabled());
        response.setTournamentId(tournament != null ? tournament.getId() : null);
        response.setEnabled(enabled);
        response.setName(tournament != null && tournament.getName() != null && !tournament.getName().isBlank()
                ? tournament.getName()
                : DEFAULT_TOURNAMENT_NAME);
        response.setStandingsLabel(tournament != null && tournament.getStandingsLabel() != null && !tournament.getStandingsLabel().isBlank()
                ? tournament.getStandingsLabel()
                : DEFAULT_STANDINGS_LABEL);
        response.setLowNetEnabled(tournament == null || tournament.getLowNetEnabled() == null || Boolean.TRUE.equals(tournament.getLowNetEnabled()));
        response.setLowGrossEnabled(tournament != null && Boolean.TRUE.equals(tournament.getLowGrossEnabled()));
        response.setLowNetName(tournament != null && tournament.getLowNetName() != null && !tournament.getLowNetName().isBlank()
                ? tournament.getLowNetName()
                : DEFAULT_LOW_NET_NAME);
        response.setLowGrossName(tournament != null && tournament.getLowGrossName() != null && !tournament.getLowGrossName().isBlank()
                ? tournament.getLowGrossName()
                : DEFAULT_LOW_GROSS_NAME);

        Map<Long, Integer> includedSortOrderByPlannedRoundId = new HashMap<Long, Integer>();
        if (tournament != null && tournament.getId() != null && Boolean.TRUE.equals(tournament.getEnabled())) {
            List<TripTournamentRound> tournamentRounds = tripTournamentRoundRepository.findByTournament_IdOrderBySortOrderAsc(tournament.getId());
            for (TripTournamentRound tournamentRound : tournamentRounds) {
                TripPlannedRound plannedRound = tournamentRound.getPlannedRound();
                if (isEligibleTournamentRound(trip, plannedRound) && plannedRound.getId() != null) {
                    includedSortOrderByPlannedRoundId.put(plannedRound.getId(), tournamentRound.getSortOrder());
                }
            }
        }

        if (includedSortOrderByPlannedRoundId.isEmpty() && (tournament == null || Boolean.TRUE.equals(tournament.getEnabled()))) {
            int legacySortOrder = 1;
            for (TripPlannedRound plannedRound : getLegacyIncludedTournamentPlannedRounds(trip)) {
                if (plannedRound.getId() != null) {
                    includedSortOrderByPlannedRoundId.put(plannedRound.getId(), legacySortOrder);
                    legacySortOrder++;
                }
            }
        }

        List<TripPlannedRound> plannedRounds = loadActivePlannedRounds(trip);
        List<TripTournamentRoundResponse> roundResponses = new ArrayList<TripTournamentRoundResponse>();
        for (TripPlannedRound plannedRound : plannedRounds) {
            TripTournamentRoundResponse roundResponse = new TripTournamentRoundResponse();
            roundResponse.setPlannedRoundId(plannedRound.getId());
            roundResponse.setRoundNumber(plannedRound.getRoundNumber());
            roundResponse.setRoundDate(plannedRound.getRoundDate());
            roundResponse.setFormat(plannedRound.getFormat() != null ? plannedRound.getFormat().name() : null);
            roundResponse.setCourseId(plannedRound.getCourseId());
            roundResponse.setCourseName(resolveCourseName(plannedRound.getCourseId()));
            roundResponse.setConfigured(isConfiguredTournamentRound(plannedRound));
            roundResponse.setIncluded(includedSortOrderByPlannedRoundId.containsKey(plannedRound.getId()));
            roundResponse.setSortOrder(includedSortOrderByPlannedRoundId.get(plannedRound.getId()));
            roundResponses.add(roundResponse);
        }
        response.setRounds(roundResponses);
        return response;
    }

    private String resolveCourseName(Long courseId) {
        if (courseId == null) {
            return null;
        }
        Course course = courseRepository.findById(courseId).orElse(null);
        return course != null ? course.getName() : null;
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

    private boolean plannedRoundHasEventType(TripPlannedRound plannedRound, RoundEventType eventType) {
        if (plannedRound == null || eventType == null) {
            return false;
        }
        if (plannedRound.getId() != null) {
            List<TripPlannedRoundEvent> events = tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(plannedRound.getId());
            if (events != null && !events.isEmpty()) {
                for (TripPlannedRoundEvent event : events) {
                    if (event != null && event.getEventType() == eventType) {
                        return true;
                    }
                }
                return false;
            }
        }
        // Compatibility only: older planned rounds may not have trip_planned_round_event rows yet.
        return RoundEventType.fromLegacyRoundFormat(plannedRound.getFormat()) == eventType;
    }

    private Trip loadTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
    }

    private void assertEditable(Trip trip) {
        if (isReadOnly(trip)) {
            throw new IllegalStateException("Tournament setup cannot be changed after the trip is complete unless Correction Mode is enabled.");
        }
    }

    private boolean isReadOnly(Trip trip) {
        return TripStatus.COMPLETE.equals(trip.getStatus()) && !trip.isCorrectionMode();
    }

    private String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
