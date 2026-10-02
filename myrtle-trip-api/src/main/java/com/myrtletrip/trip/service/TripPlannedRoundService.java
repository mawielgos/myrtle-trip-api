package com.myrtletrip.trip.service;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.trip.dto.SaveTripPlannedRoundsRequest;
import com.myrtletrip.trip.dto.TripPlannedRoundResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlannedRoundEvent;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripRepository;

@Service
public class TripPlannedRoundService {

    private final TripRepository tripRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final TripPlannedRoundLifecycleService plannedRoundLifecycleService;
    private final TripPlannedRoundCommandService plannedRoundCommandService;

    public TripPlannedRoundService(TripRepository tripRepository,
                                   TripPlannedRoundRepository tripPlannedRoundRepository,
                                   TripPlannedRoundEventRepository tripPlannedRoundEventRepository,
                                   CourseRepository courseRepository,
                                   CourseTeeRepository courseTeeRepository,
                                   TripPlannedRoundLifecycleService plannedRoundLifecycleService,
                                   TripPlannedRoundCommandService plannedRoundCommandService) {
        this.tripRepository = tripRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.tripPlannedRoundEventRepository = tripPlannedRoundEventRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.plannedRoundLifecycleService = plannedRoundLifecycleService;
        this.plannedRoundCommandService = plannedRoundCommandService;
    }

    public List<TripPlannedRoundResponse> getPlannedRounds(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        List<TripPlannedRound> rounds = loadActivePlannedRounds(trip);
        List<TripPlannedRoundResponse> responses = new ArrayList<TripPlannedRoundResponse>();

        for (TripPlannedRound round : rounds) {
            responses.add(toPlannedRoundResponse(round));
        }

        return responses;
    }

    public List<TripPlannedRoundResponse> savePlannedRounds(Long tripId, SaveTripPlannedRoundsRequest request) {
        plannedRoundCommandService.savePlannedRounds(tripId, request);
        return getPlannedRounds(tripId);
    }

    public int resolvePlannedRoundCount(Integer plannedRoundCount) {
        return plannedRoundLifecycleService.resolvePlannedRoundCount(plannedRoundCount);
    }

    public void validateExistingPlannedRoundsWithinTripDates(Trip trip) {
        plannedRoundCommandService.validateExistingPlannedRoundsWithinTripDates(trip);
    }

    public List<TripPlannedRound> findAllPlannedRounds(Trip trip) {
        return tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);
    }

    public List<TripPlannedRound> loadActivePlannedRounds(Trip trip) {
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

    public void syncPlannedRoundsToTripRoundCount(Trip trip) {
        plannedRoundLifecycleService.syncPlannedRoundsToTripRoundCount(trip);
    }

    public void createDefaultPlannedRounds(Trip trip) {
        plannedRoundLifecycleService.createDefaultPlannedRounds(trip);
    }

    private Integer resolvePlannedRoundScrambleTeamSize(TripPlannedRound plannedRound) {
        if (!plannedRoundHasEventType(plannedRound, RoundEventType.TEAM_SCRAMBLE)) {
            return 4;
        }
        int size = plannedRound.getScrambleTeamSize() == null ? 4 : plannedRound.getScrambleTeamSize();
        if (size < 2 || size > 4) {
            return 4;
        }
        return size;
    }

    public boolean hasPlannedRoundEventConfiguration(TripPlannedRound plannedRound) {
        if (plannedRound == null) {
            return false;
        }
        if (plannedRound.getId() != null) {
            List<TripPlannedRoundEvent> events = tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(plannedRound.getId());
            if (events != null && !events.isEmpty()) {
                return true;
            }
        }
        // Compatibility only: older saved planned rounds may not have trip_planned_round_event rows yet.
        return plannedRound.getFormat() != null;
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
        // Compatibility only: legacy planned rounds infer a single event from round.format.
        return RoundEventType.fromLegacyRoundFormat(plannedRound.getFormat()) == eventType;
    }

    private List<TripPlannedRoundResponse.TripPlannedRoundEventResponse> plannedRoundEventResponses(TripPlannedRound round) {
        List<TripPlannedRoundEvent> events = tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(round.getId());
        if (events.isEmpty()) {
            events = legacyPlannedRoundEventEntities(round);
        }

        List<TripPlannedRoundResponse.TripPlannedRoundEventResponse> responses = new ArrayList<TripPlannedRoundResponse.TripPlannedRoundEventResponse>();
        for (TripPlannedRoundEvent event : events) {
            TripPlannedRoundResponse.TripPlannedRoundEventResponse response = new TripPlannedRoundResponse.TripPlannedRoundEventResponse();
            response.setId(event.getId());
            response.setEventType(event.getEventType());
            response.setEventName(event.getEventName());
            response.setEventOrder(event.getEventOrder());
            response.setTeamSize(event.getTeamSize());
            response.setHandicapPercent(event.getHandicapPercent());
            responses.add(response);
        }
        return responses;
    }

    private List<TripPlannedRoundEvent> legacyPlannedRoundEventEntities(TripPlannedRound round) {
        List<TripPlannedRoundEvent> events = new ArrayList<TripPlannedRoundEvent>();
        RoundEventType eventType = RoundEventType.fromLegacyRoundFormat(round.getFormat());
        Integer teamSize = eventType.defaultTeamSize(round.getScrambleTeamSize());
        TripPlannedRoundEvent event = new TripPlannedRoundEvent();
        event.setPlannedRound(round);
        event.setEventType(eventType);
        event.setEventName(eventType.defaultName(teamSize));
        event.setEventOrder(1);
        event.setTeamSize(teamSize);
        event.setHandicapPercent(null);
        events.add(event);
        return events;
    }

    private TripPlannedRoundResponse toPlannedRoundResponse(TripPlannedRound round) {
        TripPlannedRoundResponse response = new TripPlannedRoundResponse();
        response.setPlannedRoundId(round.getId());
        response.setRoundNumber(round.getRoundNumber());
        response.setRoundDate(round.getRoundDate());
        response.setCourseId(round.getCourseId());
        response.setDefaultTeeId(round.getStandardTeeId());
        response.setWomenDefaultTeeId(round.getWomenDefaultTeeId());
        response.setFormat(round.getFormat() != null ? round.getFormat().name() : null);
        response.setScrambleTeamSize(resolvePlannedRoundScrambleTeamSize(round));
        response.setIncludeInFourDayStandings(Boolean.TRUE.equals(round.getIncludeInFourDayStandings()));
        response.setEvents(plannedRoundEventResponses(round));

        Course course = null;
        if (round.getCourseId() != null) {
            course = courseRepository.findById(round.getCourseId()).orElse(null);
        }

        CourseTee defaultTee = null;
        if (round.getStandardTeeId() != null) {
            defaultTee = courseTeeRepository.findById(round.getStandardTeeId()).orElse(null);
        }

        CourseTee womenDefaultTee = null;
        if (round.getWomenDefaultTeeId() != null) {
            womenDefaultTee = courseTeeRepository.findById(round.getWomenDefaultTeeId()).orElse(null);
        }

        response.setCourseName(course != null ? course.getName() : null);
        response.setDefaultTeeDisplay(formatCourseTeeDisplay(defaultTee));
        response.setWomenDefaultTeeDisplay(formatWomenCourseTeeDisplay(womenDefaultTee));

        return response;
    }

    private String formatCourseTeeDisplay(CourseTee tee) {
        if (tee == null) {
            return null;
        }

        String teeName = tee.getTeeName();
        if (tee.getCourseRating() == null || tee.getSlope() == null) {
            return teeName;
        }

        return teeName + " (Rating " + formatCourseRating(tee.getCourseRating()) + " / Slope " + tee.getSlope() + ")";
    }

    private String formatWomenCourseTeeDisplay(CourseTee tee) {
        if (tee == null) {
            return null;
        }

        String teeName = tee.getTeeName();
        if (tee.getWomenCourseRating() == null || tee.getWomenSlope() == null) {
            return teeName;
        }

        return teeName + " (Rating " + formatCourseRating(tee.getWomenCourseRating()) + " / Slope " + tee.getWomenSlope() + ")";
    }

    private String formatCourseRating(java.math.BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

}
