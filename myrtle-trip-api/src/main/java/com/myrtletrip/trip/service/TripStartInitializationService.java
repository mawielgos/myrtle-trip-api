package com.myrtletrip.trip.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.entity.CourseTeeComboHole;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.event.repository.RoundEventRepository;
import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.entity.RoundTeeHole;
import com.myrtletrip.round.model.RoundTeeRole;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlannedRoundEvent;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TripStartInitializationService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    private final RoundRepository roundRepository;
    private final RoundEventRepository roundEventRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final RoundTeeHoleRepository roundTeeHoleRepository;
    private final ScorecardRepository scorecardRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final CourseHoleRepository courseHoleRepository;
    private final CourseTeeComboHoleRepository courseTeeComboHoleRepository;
    private final RoundHandicapService roundHandicapService;
    private final TripService tripService;

    public TripStartInitializationService(
            TripRepository tripRepository,
            TripPlayerRepository tripPlayerRepository,
            TripPlannedRoundRepository tripPlannedRoundRepository,
            TripPlannedRoundEventRepository tripPlannedRoundEventRepository,
            RoundRepository roundRepository,
            RoundEventRepository roundEventRepository,
            RoundTeeRepository roundTeeRepository,
            RoundTeeHoleRepository roundTeeHoleRepository,
            ScorecardRepository scorecardRepository,
            CourseRepository courseRepository,
            CourseTeeRepository courseTeeRepository,
            CourseHoleRepository courseHoleRepository,
            CourseTeeComboHoleRepository courseTeeComboHoleRepository,
            RoundHandicapService roundHandicapService,
            TripService tripService
    ) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.tripPlannedRoundEventRepository = tripPlannedRoundEventRepository;
        this.roundRepository = roundRepository;
        this.roundEventRepository = roundEventRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.scorecardRepository = scorecardRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.courseHoleRepository = courseHoleRepository;
        this.courseTeeComboHoleRepository = courseTeeComboHoleRepository;
        this.roundHandicapService = roundHandicapService;
        this.tripService = tripService;
    }

    @Transactional
    public void initializeTrip(Long tripId) throws Exception {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (Boolean.TRUE.equals(trip.getInitialized())) {
            throw new IllegalStateException("Trip is already initialized.");
        }

        List<TripPlayer> players = tripPlayerRepository.findByTrip(trip);
        if (players == null || players.isEmpty()) {
            throw new IllegalStateException("Trip must have players before initialization.");
        }
        tripService.validateTripCanStart(tripId);
        
        List<TripPlannedRound> plannedRounds =
                tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);

        if (plannedRounds == null || plannedRounds.isEmpty()) {
            throw new IllegalStateException("Trip must have planned rounds before initialization.");
        }

        for (TripPlannedRound pr : plannedRounds) {
            validatePlannedRound(pr);
        }

        String handicapGroupCode = trip.getTripCode();
        boolean tripHasFemalePlayers = hasFemalePlayers(players);

        for (TripPlannedRound pr : plannedRounds) {

            Course course = courseRepository.findById(pr.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + pr.getCourseId()));

            CourseTee defaultCourseTee = courseTeeRepository.findById(pr.getStandardTeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Default tee not found: " + pr.getStandardTeeId()));

            if (!course.getId().equals(defaultCourseTee.getCourse().getId())) {
                throw new IllegalArgumentException(
                        "Default tee " + pr.getStandardTeeId() + " does not belong to course " + pr.getCourseId()
                );
            }

            if (tripHasFemalePlayers && pr.getWomenDefaultTeeId() == null) {
                throw new IllegalStateException("Planned round " + pr.getRoundNumber() + " is missing women's default tee.");
            }

            CourseTee womenDefaultCourseTee = null;
            if (pr.getWomenDefaultTeeId() != null) {
                womenDefaultCourseTee = courseTeeRepository.findById(pr.getWomenDefaultTeeId())
                        .orElseThrow(() -> new IllegalArgumentException("Women's default tee not found: " + pr.getWomenDefaultTeeId()));

                if (!course.getId().equals(womenDefaultCourseTee.getCourse().getId())) {
                    throw new IllegalArgumentException(
                            "Women's default tee " + pr.getWomenDefaultTeeId() + " does not belong to course " + pr.getCourseId()
                    );
                }

                if (!womenDefaultCourseTee.isEligibleForGender("F")) {
                    throw new IllegalArgumentException("Women's default tee is not eligible for women: " + womenDefaultCourseTee.getTeeName());
                }
            }

            Round round = new Round();
            round.setTrip(trip);
            round.setRoundNumber(pr.getRoundNumber());
            round.setRoundDate(pr.getRoundDate());
            round.setCourse(course);
            round.setFormat(pr.getFormat());
            round.setScrambleTeamSize(resolveScrambleTeamSize(pr));
            round.setScrambleScoreEntryMode("TOTAL");
            round.setHandicapPercent(100);
            round.setFinalized(false);

            round = roundRepository.save(round);
            createRoundEvents(round, pr);

            RoundTee defaultRoundTee = createRoundTee(round, course, defaultCourseTee, RoundTeeRole.DEFAULT, "M");
            RoundTee womenDefaultRoundTee = null;
            if (womenDefaultCourseTee != null) {
                // Always create a separate women's default RoundTee. Even when the selected
                // source course tee is the same named tee as the men's default, the women's
                // rating/slope/par can differ and must drive handicap/stroke calculations.
                womenDefaultRoundTee = createRoundTee(round, course, womenDefaultCourseTee, RoundTeeRole.PLAYER_OPTION, "F");
            }

            round.setDefaultRoundTee(defaultRoundTee);
            round = roundRepository.save(round);

            for (TripPlayer tp : players) {
                Scorecard scorecard = new Scorecard();
                scorecard.setRound(round);
                scorecard.setPlayer(tp.getPlayer());

                RoundTee playerDefaultTee = defaultRoundTee;
                if (tp.getPlayer() != null
                        && "F".equalsIgnoreCase(tp.getPlayer().getGender())
                        && womenDefaultRoundTee != null) {
                    playerDefaultTee = womenDefaultRoundTee;
                }
                scorecard.setRoundTee(playerDefaultTee);

                roundHandicapService.populateCurrentHandicaps(scorecard, handicapGroupCode);

                scorecardRepository.save(scorecard);
            }
        }

        trip.setInitialized(true);
        trip.setStatus(TripStatus.IN_PROGRESS);
        tripRepository.save(trip);
    }


    private void createRoundEvents(Round round, TripPlannedRound plannedRound) {
        List<TripPlannedRoundEvent> plannedEvents = tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(plannedRound.getId());
        if (plannedEvents == null || plannedEvents.isEmpty()) {
            plannedEvents = legacyPlannedRoundEvents(plannedRound);
        }

        int defaultOrder = 1;
        for (TripPlannedRoundEvent plannedEvent : plannedEvents) {
            RoundEventType eventType = plannedEvent.getEventType();
            if (eventType == null) {
                continue;
            }

            Integer teamSize = plannedEvent.getTeamSize() != null
                    ? plannedEvent.getTeamSize()
                    : eventType.defaultTeamSize(round.getScrambleTeamSize());

            RoundEvent event = new RoundEvent();
            event.setRound(round);
            event.setEventType(eventType);
            event.setEventName(plannedEvent.getEventName() == null || plannedEvent.getEventName().isBlank()
                    ? eventType.defaultName(teamSize)
                    : plannedEvent.getEventName().trim());
            event.setEventOrder(plannedEvent.getEventOrder() == null ? defaultOrder : plannedEvent.getEventOrder());
            event.setActive(Boolean.TRUE);
            event.setUsesGross(eventType.usesGross());
            event.setUsesNet(eventType.usesNet());
            event.setUsesTeams(eventType.isTeamEvent());
            event.setTeamSize(teamSize);
            event.setHandicapPercent(plannedEvent.getHandicapPercent() == null ? round.getHandicapPercent() : plannedEvent.getHandicapPercent());
            roundEventRepository.save(event);
            defaultOrder++;
        }
    }

    private List<TripPlannedRoundEvent> legacyPlannedRoundEvents(TripPlannedRound plannedRound) {
        List<TripPlannedRoundEvent> events = new ArrayList<>();
        RoundEventType eventType = RoundEventType.fromLegacyRoundFormat(plannedRound.getFormat());
        Integer teamSize = eventType.defaultTeamSize(plannedRound.getScrambleTeamSize());
        TripPlannedRoundEvent event = new TripPlannedRoundEvent();
        event.setPlannedRound(plannedRound);
        event.setEventType(eventType);
        event.setEventName(eventType.defaultName(teamSize));
        event.setEventOrder(1);
        event.setTeamSize(teamSize);
        event.setHandicapPercent(null);
        events.add(event);
        return events;
    }

    private Integer resolveScrambleTeamSize(TripPlannedRound plannedRound) {
        Integer size = null;
        if (plannedRound != null && plannedRound.getId() != null) {
            List<TripPlannedRoundEvent> events = tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(plannedRound.getId());
            if (events != null) {
                for (TripPlannedRoundEvent event : events) {
                    if (event != null && event.getEventType() == RoundEventType.TEAM_SCRAMBLE) {
                        size = event.getTeamSize();
                        break;
                    }
                }
            }
        }
        if (size == null) {
            size = plannedRound == null ? null : plannedRound.getScrambleTeamSize();
        }
        if (size == null) {
            return 4;
        }
        if (size < 2 || size > 4) {
            throw new IllegalArgumentException("Scramble team size must be 2, 3, or 4 for round " + plannedRound.getRoundNumber());
        }
        return size;
    }

    private boolean hasFemalePlayers(List<TripPlayer> players) {
        if (players == null) {
            return false;
        }
        for (TripPlayer player : players) {
            if (player != null
                    && player.getPlayer() != null
                    && "F".equalsIgnoreCase(player.getPlayer().getGender())) {
                return true;
            }
        }
        return false;
    }

    private void validatePlannedRound(TripPlannedRound pr) {

        if (pr.getCourseId() == null) {
            throw new IllegalStateException("Planned round " + pr.getRoundNumber() + " is missing course.");
        }

        if (pr.getStandardTeeId() == null) {
            throw new IllegalStateException("Planned round " + pr.getRoundNumber() + " is missing men's default tee.");
        }

        if (pr.getFormat() == null) {
            throw new IllegalStateException("Planned round " + pr.getRoundNumber() + " is missing format.");
        }

        if (pr.getRoundDate() == null) {
            throw new IllegalStateException("Planned round " + pr.getRoundNumber() + " is missing date.");
        }
    }

    private RoundTee createRoundTee(
            Round round,
            Course course,
            CourseTee courseTee,
            RoundTeeRole teeRole,
            String gender
    ) {
        RoundTee rt = new RoundTee();
        rt.setRound(round);
        rt.setSourceCourseTee(courseTee);
        rt.setTeeRole(teeRole);
        rt.setCourseName(course.getName());
        rt.setTeeName(courseTee.getTeeName());
        rt.setCourseRating(courseTee.getRatingForGender(gender));
        rt.setSlope(courseTee.getSlopeForGender(gender));
        rt.setParTotal(courseTee.getParForGender(gender));
        rt = roundTeeRepository.save(rt);

        List<CourseHole> sourceHoles = resolveSourceHoles(courseTee);

        if (sourceHoles.size() != 18) {
            throw new IllegalStateException(
                    "Expected 18 hole mappings for course tee " + courseTee.getId()
                            + " but found " + sourceHoles.size()
                            + ". If this is a combo tee, verify all 18 combo holes point to source tees with hole detail."
            );
        }

        for (CourseHole sourceHole : sourceHoles) {
            RoundTeeHole roundTeeHole = new RoundTeeHole();
            roundTeeHole.setRoundTee(rt);
            roundTeeHole.setHoleNumber(sourceHole.getHoleNumber());
            if ("F".equalsIgnoreCase(gender) && sourceHole.getWomenPar() != null && sourceHole.getWomenHandicap() != null) {
                roundTeeHole.setPar(sourceHole.getWomenPar());
                roundTeeHole.setHandicap(sourceHole.getWomenHandicap());
            } else {
                roundTeeHole.setPar(sourceHole.getPar());
                roundTeeHole.setHandicap(sourceHole.getHandicap());
            }
            roundTeeHole.setYardage(sourceHole.getYardage());
            roundTeeHoleRepository.save(roundTeeHole);
        }

        return rt;
    }
    private List<CourseHole> resolveSourceHoles(CourseTee courseTee) {
        List<CourseHole> directHoles = courseHoleRepository.findByCourseTee_IdOrderByHoleNumberAsc(courseTee.getId());
        if (directHoles != null && directHoles.size() == 18) {
            return directHoles;
        }

        List<CourseTeeComboHole> comboMappings =
                courseTeeComboHoleRepository.findByComboTee_IdOrderByHoleNumberAsc(courseTee.getId());
        if (comboMappings == null || comboMappings.size() != 18) {
            return new ArrayList<>();
        }

        List<CourseHole> resolvedHoles = new ArrayList<>();
        for (CourseTeeComboHole mapping : comboMappings) {
            if (mapping == null
                    || mapping.getHoleNumber() == null
                    || mapping.getSourceTee() == null
                    || mapping.getSourceTee().getId() == null) {
                return new ArrayList<>();
            }

            CourseHole sourceHole = courseHoleRepository
                    .findByCourseTee_IdAndHoleNumber(mapping.getSourceTee().getId(), mapping.getHoleNumber())
                    .orElse(null);
            if (sourceHole == null) {
                return new ArrayList<>();
            }

            resolvedHoles.add(sourceHole);
        }

        return resolvedHoles;
    }

}
