package com.myrtletrip.trip.service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.trip.dto.SaveTripPlannedRoundsRequest;
import com.myrtletrip.trip.dto.TripPlannedRoundRequest;
import com.myrtletrip.trip.dto.TripPlannedRoundResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlannedRoundEvent;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripRepository;

@Service
public class TripPlannedRoundService {

    private static final int DEFAULT_PLANNED_ROUND_COUNT = 5;
    private static final int MIN_PLANNED_ROUND_COUNT = 1;
    private static final int MAX_PLANNED_ROUND_COUNT = 12;

    private final TripRepository tripRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final CourseHoleRepository courseHoleRepository;
    private final CourseTeeComboHoleRepository courseTeeComboHoleRepository;

    public TripPlannedRoundService(TripRepository tripRepository,
                                   TripPlannedRoundRepository tripPlannedRoundRepository,
                                   TripPlannedRoundEventRepository tripPlannedRoundEventRepository,
                                   CourseRepository courseRepository,
                                   CourseTeeRepository courseTeeRepository,
                                   CourseHoleRepository courseHoleRepository,
                                   CourseTeeComboHoleRepository courseTeeComboHoleRepository) {
        this.tripRepository = tripRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.tripPlannedRoundEventRepository = tripPlannedRoundEventRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.courseHoleRepository = courseHoleRepository;
        this.courseTeeComboHoleRepository = courseTeeComboHoleRepository;
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
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        assertTripSetupEditable(trip);
        
        if (request == null || request.getRounds() == null || request.getRounds().isEmpty()) {
            throw new IllegalArgumentException("Planned rounds are required.");
        }

        validatePlannedRounds(request.getRounds(), trip);

        List<TripPlannedRoundRequest> sequencedRequests = sortPlannedRoundRequestsByPlaySequence(request.getRounds());

        trip.setPlannedRoundCount(sequencedRequests.size());
        tripRepository.save(trip);

        tripPlannedRoundEventRepository.deleteByPlannedRound_Trip_Id(trip.getId());
        tripPlannedRoundRepository.deleteByTrip(trip);
        tripPlannedRoundRepository.flush();

        List<TripPlannedRound> roundsToSave = new ArrayList<TripPlannedRound>();

        for (int index = 0; index < sequencedRequests.size(); index++) {
            TripPlannedRoundRequest roundRequest = sequencedRequests.get(index);
            TripPlannedRound plannedRound = new TripPlannedRound();
            plannedRound.setTrip(trip);
            plannedRound.setRoundNumber(index + 1);
            plannedRound.setRoundDate(roundRequest.getRoundDate());
            plannedRound.setCourseId(roundRequest.getCourseId());
            plannedRound.setStandardTeeId(roundRequest.getDefaultTeeId());
            plannedRound.setWomenDefaultTeeId(roundRequest.getWomenDefaultTeeId());
            RoundFormat parsedFormat = parseRoundFormat(roundRequest.getFormat());
            plannedRound.setFormat(parsedFormat);
            plannedRound.setScrambleTeamSize(resolveScrambleTeamSize(parsedFormat, roundRequest.getScrambleTeamSize()));
            plannedRound.setIncludeInFourDayStandings(Boolean.TRUE.equals(roundRequest.getIncludeInFourDayStandings()));

            roundsToSave.add(plannedRound);
        }

        tripPlannedRoundRepository.saveAll(roundsToSave);
        tripPlannedRoundRepository.flush();

        List<TripPlannedRoundEvent> eventsToSave = new ArrayList<TripPlannedRoundEvent>();
        for (int index = 0; index < sequencedRequests.size(); index++) {
            addPlannedRoundEvents(roundsToSave.get(index), sequencedRequests.get(index), eventsToSave);
        }
        tripPlannedRoundEventRepository.saveAll(eventsToSave);
        tripPlannedRoundEventRepository.flush();

        return getPlannedRounds(tripId);
    }

    public int resolvePlannedRoundCount(Integer plannedRoundCount) {
        if (plannedRoundCount == null) {
            return DEFAULT_PLANNED_ROUND_COUNT;
        }
        return plannedRoundCount;
    }

    public void validateExistingPlannedRoundsWithinTripDates(Trip trip) {
        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);
        for (TripPlannedRound plannedRound : plannedRounds) {
            if (plannedRound == null) {
                continue;
            }
            validateRoundDateWithinTripDates(trip, plannedRound.getRoundDate(), plannedRound.getRoundNumber());
        }
    }

    private void validateRoundDateWithinTripDates(Trip trip, LocalDate roundDate, Integer roundNumber) {
        if (roundDate == null || trip == null) {
            return;
        }

        LocalDate tripStartDate = trip.getTripStartDate();
        LocalDate tripEndDate = trip.getTripEndDate();

        if (tripStartDate != null && roundDate.isBefore(tripStartDate)) {
            throw new IllegalArgumentException("Round " + roundNumber + " date must be on or after the trip start date.");
        }
        if (tripEndDate != null && roundDate.isAfter(tripEndDate)) {
            throw new IllegalArgumentException("Round " + roundNumber + " date must be on or before the trip end date.");
        }
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
        int targetCount = resolvePlannedRoundCount(trip.getPlannedRoundCount());
        List<TripPlannedRound> existingRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);

        if (existingRounds.size() == targetCount) {
            return;
        }

        if (existingRounds.size() > targetCount) {
            for (int i = targetCount; i < existingRounds.size(); i++) {
                TripPlannedRound plannedRound = existingRounds.get(i);
                if (plannedRound.getRoundDate() != null
                        || plannedRound.getCourseId() != null
                        || plannedRound.getStandardTeeId() != null
                        || plannedRound.getFormat() != null
                        || Boolean.TRUE.equals(plannedRound.getIncludeInFourDayStandings())) {
                    throw new IllegalArgumentException("Cannot reduce planned round count because Round " + plannedRound.getRoundNumber() + " already has setup details. Clear that round first.");
                }
            }

            for (int i = existingRounds.size() - 1; i >= targetCount; i--) {
                tripPlannedRoundRepository.delete(existingRounds.get(i));
            }
            tripPlannedRoundRepository.flush();
            return;
        }

        for (int roundNumber = existingRounds.size() + 1; roundNumber <= targetCount; roundNumber++) {
            TripPlannedRound plannedRound = new TripPlannedRound();
            plannedRound.setTrip(trip);
            plannedRound.setRoundNumber(roundNumber);
            plannedRound.setFormat(defaultFormatForRound(roundNumber));
            plannedRound.setIncludeInFourDayStandings(false);
            tripPlannedRoundRepository.save(plannedRound);
        }
        tripPlannedRoundRepository.flush();
    }

    public void createDefaultPlannedRounds(Trip trip) {
        if (tripPlannedRoundRepository.countByTrip(trip) > 0) {
            return;
        }

        int plannedRoundCount = resolvePlannedRoundCount(trip.getPlannedRoundCount());

        for (int i = 1; i <= plannedRoundCount; i++) {
            TripPlannedRound plannedRound = new TripPlannedRound();
            plannedRound.setTrip(trip);
            plannedRound.setRoundNumber(i);
            plannedRound.setFormat(defaultFormatForRound(i));
            plannedRound.setIncludeInFourDayStandings(defaultIncludeInFourDayStandings(i));
            tripPlannedRoundRepository.save(plannedRound);
        }
    }

    private List<TripPlannedRoundRequest> sortPlannedRoundRequestsByPlaySequence(List<TripPlannedRoundRequest> rounds) {
        List<TripPlannedRoundRequest> sorted = new ArrayList<TripPlannedRoundRequest>(rounds);
        sorted.sort(new Comparator<TripPlannedRoundRequest>() {
            @Override
            public int compare(TripPlannedRoundRequest left, TripPlannedRoundRequest right) {
                LocalDate leftDate = left != null ? left.getRoundDate() : null;
                LocalDate rightDate = right != null ? right.getRoundDate() : null;

                if (leftDate != null && rightDate != null && !leftDate.equals(rightDate)) {
                    return leftDate.compareTo(rightDate);
                }
                if (leftDate != null && rightDate == null) {
                    return -1;
                }
                if (leftDate == null && rightDate != null) {
                    return 1;
                }

                Integer leftRoundNumber = left != null ? left.getRoundNumber() : null;
                Integer rightRoundNumber = right != null ? right.getRoundNumber() : null;
                if (leftRoundNumber == null && rightRoundNumber == null) {
                    return 0;
                }
                if (leftRoundNumber == null) {
                    return 1;
                }
                if (rightRoundNumber == null) {
                    return -1;
                }
                return leftRoundNumber.compareTo(rightRoundNumber);
            }
        });
        return sorted;
    }

    private void validatePlannedRounds(List<TripPlannedRoundRequest> rounds, Trip trip) {
        Set<Integer> usedRoundNumbers = new HashSet<Integer>();

        if (rounds.size() < MIN_PLANNED_ROUND_COUNT || rounds.size() > MAX_PLANNED_ROUND_COUNT) {
            throw new IllegalArgumentException("Planned round count must be between " + MIN_PLANNED_ROUND_COUNT + " and " + MAX_PLANNED_ROUND_COUNT + ".");
        }

        for (TripPlannedRoundRequest round : rounds) {
            if (round == null) {
                throw new IllegalArgumentException("Planned round is required.");
            }
            if (round.getRoundNumber() == null) {
                throw new IllegalArgumentException("roundNumber is required.");
            }
            if (round.getRoundNumber() < 1) {
                throw new IllegalArgumentException("roundNumber must be >= 1.");
            }
            if (round.getRoundNumber() > MAX_PLANNED_ROUND_COUNT) {
                throw new IllegalArgumentException("roundNumber must be <= " + MAX_PLANNED_ROUND_COUNT + ".");
            }
            if (!usedRoundNumbers.add(round.getRoundNumber())) {
                throw new IllegalArgumentException("Duplicate planned round number: " + round.getRoundNumber());
            }
            validateRoundDateWithinTripDates(trip, round.getRoundDate(), round.getRoundNumber());

            if (round.getDefaultTeeId() != null && round.getCourseId() == null) {
                throw new IllegalArgumentException("courseId is required when a men's default tee is provided for round " + round.getRoundNumber());
            }
            if (round.getWomenDefaultTeeId() != null && round.getCourseId() == null) {
                throw new IllegalArgumentException("courseId is required when a women's default tee is provided for round " + round.getRoundNumber());
            }
            validatePlannedRoundTee(round.getRoundNumber(), round.getCourseId(), round.getDefaultTeeId(), "men's", "M");
            validatePlannedRoundTee(round.getRoundNumber(), round.getCourseId(), round.getWomenDefaultTeeId(), "women's", "F");
            validateRequestedScrambleTeamSize(round);
        }

    }

    private void validatePlannedRoundTee(Integer roundNumber, Long courseId, Long teeId, String label, String gender) {
        if (teeId == null) {
            return;
        }
        CourseTee tee = courseTeeRepository.findById(teeId)
                .orElseThrow(() -> new IllegalArgumentException("Round " + roundNumber + " " + label + " default tee was not found."));
        if (courseId != null && (tee.getCourse() == null || !courseId.equals(tee.getCourse().getId()))) {
            throw new IllegalArgumentException("Round " + roundNumber + " " + label + " default tee does not belong to the selected course.");
        }
        if (!tee.isEligibleForGender(gender)) {
            throw new IllegalArgumentException("Round " + roundNumber + " " + label + " default tee is not eligible for that gender.");
        }

        long holeCount = countConfiguredTeeHoles(tee);
        if (holeCount != 18) {
            throw new IllegalArgumentException(
                    "Round " + roundNumber + " " + label + " default tee has " + holeCount +
                            " holes configured. Open Course Master and enter all 18 holes before starting the trip."
            );
        }
    }

    private long countConfiguredTeeHoles(CourseTee tee) {
        if (tee == null || tee.getId() == null) {
            return 0;
        }

        long regularHoleCount = courseHoleRepository.countByCourseTee_Id(tee.getId());
        if (regularHoleCount == 18) {
            return regularHoleCount;
        }

        long comboHoleCount = courseTeeComboHoleRepository.countByComboTee_Id(tee.getId());
        if (comboHoleCount > 0) {
            return comboHoleCount;
        }

        return regularHoleCount;
    }

    private Integer resolveScrambleTeamSize(RoundFormat format, Integer requestedSize) {
        if (format != RoundFormat.TEAM_SCRAMBLE) {
            return 4;
        }

        int size = requestedSize == null ? 4 : requestedSize;
        if (size < 2 || size > 4) {
            throw new IllegalArgumentException("Scramble team size must be 2, 3, or 4.");
        }
        return size;
    }

    private void validateRequestedScrambleTeamSize(TripPlannedRoundRequest round) {
        if (round == null) {
            return;
        }

        boolean hasScrambleEvent = requestedRoundHasEventType(round, RoundEventType.TEAM_SCRAMBLE);
        if (!hasScrambleEvent && hasNoRequestedEvents(round)) {
            // Compatibility only: older clients may still send the legacy format field instead of event rows.
            RoundFormat parsedFormat = null;
            if (round.getFormat() != null && !round.getFormat().isBlank()) {
                parsedFormat = parseRoundFormat(round.getFormat());
            }
            hasScrambleEvent = parsedFormat == RoundFormat.TEAM_SCRAMBLE;
        }

        if (!hasScrambleEvent) {
            return;
        }

        int size = round.getScrambleTeamSize() == null ? 4 : round.getScrambleTeamSize();
        if (size < 2 || size > 4) {
            throw new IllegalArgumentException("Scramble team size must be 2, 3, or 4.");
        }
    }

    private boolean requestedRoundHasEventType(TripPlannedRoundRequest round, RoundEventType eventType) {
        if (round == null || eventType == null || round.getEvents() == null) {
            return false;
        }
        for (TripPlannedRoundRequest.TripPlannedRoundEventRequest event : round.getEvents()) {
            if (event != null && event.getEventType() == eventType) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNoRequestedEvents(TripPlannedRoundRequest round) {
        return round == null || round.getEvents() == null || round.getEvents().isEmpty();
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

    private void addPlannedRoundEvents(TripPlannedRound plannedRound,
                                       TripPlannedRoundRequest request,
                                       List<TripPlannedRoundEvent> eventsToSave) {
        List<TripPlannedRoundRequest.TripPlannedRoundEventRequest> requestedEvents = request.getEvents();
        if (requestedEvents == null || requestedEvents.isEmpty()) {
            requestedEvents = legacyPlannedRoundEvents(request);
        }

        validatePlannedRoundEvents(requestedEvents, plannedRound.getScrambleTeamSize());

        int defaultOrder = 1;
        for (TripPlannedRoundRequest.TripPlannedRoundEventRequest eventRequest : requestedEvents) {
            RoundEventType eventType = eventRequest.getEventType();
            Integer teamSize = eventRequest.getTeamSize() != null
                    ? eventRequest.getTeamSize()
                    : eventType.defaultTeamSize(plannedRound.getScrambleTeamSize());

            TripPlannedRoundEvent event = new TripPlannedRoundEvent();
            event.setPlannedRound(plannedRound);
            event.setEventType(eventType);
            event.setEventName(eventRequest.getEventName() == null || eventRequest.getEventName().isBlank()
                    ? eventType.defaultName(teamSize)
                    : eventRequest.getEventName().trim());
            event.setEventOrder(eventRequest.getEventOrder() == null ? defaultOrder : eventRequest.getEventOrder());
            event.setTeamSize(teamSize);
            event.setHandicapPercent(eventRequest.getHandicapPercent());
            eventsToSave.add(event);
            defaultOrder++;
        }
    }

    private List<TripPlannedRoundRequest.TripPlannedRoundEventRequest> legacyPlannedRoundEvents(TripPlannedRoundRequest request) {
        List<TripPlannedRoundRequest.TripPlannedRoundEventRequest> events = new ArrayList<TripPlannedRoundRequest.TripPlannedRoundEventRequest>();
        TripPlannedRoundRequest.TripPlannedRoundEventRequest event = new TripPlannedRoundRequest.TripPlannedRoundEventRequest();
        RoundEventType eventType = RoundEventType.fromLegacyRoundFormat(parseRoundFormat(request.getFormat()));
        Integer teamSize = eventType.defaultTeamSize(request.getScrambleTeamSize());
        event.setEventType(eventType);
        event.setEventName(eventType.defaultName(teamSize));
        event.setEventOrder(1);
        event.setTeamSize(teamSize);
        events.add(event);
        return events;
    }

    private void validatePlannedRoundEvents(List<TripPlannedRoundRequest.TripPlannedRoundEventRequest> events, Integer scrambleTeamSize) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("At least one event is required for each planned round.");
        }

        Set<RoundEventType> seen = new HashSet<RoundEventType>();
        RoundEventType teamEventType = null;

        for (TripPlannedRoundRequest.TripPlannedRoundEventRequest event : events) {
            if (event == null || event.getEventType() == null) {
                throw new IllegalArgumentException("Each planned round event must have an event type.");
            }

            RoundEventType eventType = event.getEventType();
            if (!seen.add(eventType)) {
                throw new IllegalArgumentException("Duplicate planned round event type: " + eventType);
            }

            if (eventType.isTeamEvent()) {
                if (teamEventType != null) {
                    throw new IllegalArgumentException("This planned round cannot include more than one team event. "
                            + "Create separate rounds for additional team games, or use one team event with individual side events.");
                }
                teamEventType = eventType;
            }
        }
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

    private RoundFormat parseRoundFormat(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return RoundFormat.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid round format: " + value);
        }
    }

    private boolean defaultIncludeInFourDayStandings(int roundNumber) {
        return false;
    }

private RoundFormat defaultFormatForRound(int roundNumber) {
        // Path B: new planned rounds should not inherit the old fixed Myrtle sequence.
        // The game is intentionally selected by the user on Trip Round Planning.
        return null;
    }
    private void assertTripSetupEditable(Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("Trip is required.");
        }
        if (TripStatus.IN_PROGRESS.equals(trip.getStatus())
                || TripStatus.COMPLETE.equals(trip.getStatus())
                || Boolean.TRUE.equals(trip.getInitialized())) {
            throw new IllegalStateException("Trip setup cannot be changed after the trip has started.");
        }
    }
}
