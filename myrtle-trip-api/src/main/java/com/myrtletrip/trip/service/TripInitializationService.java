package com.myrtletrip.trip.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
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
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripInitializationService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final RoundRepository roundRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final RoundTeeHoleRepository roundTeeHoleRepository;
    private final ScorecardRepository scorecardRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final CourseHoleRepository courseHoleRepository;
    private final RoundHandicapService roundHandicapService;
    private final TripService tripService;

    public TripInitializationService(
            TripRepository tripRepository,
            TripPlayerRepository tripPlayerRepository,
            TripPlannedRoundRepository tripPlannedRoundRepository,
            RoundRepository roundRepository,
            RoundTeeRepository roundTeeRepository,
            RoundTeeHoleRepository roundTeeHoleRepository,
            ScorecardRepository scorecardRepository,
            CourseRepository courseRepository,
            CourseTeeRepository courseTeeRepository,
            CourseHoleRepository courseHoleRepository,
            RoundHandicapService roundHandicapService,
            TripService tripService
    ) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.roundRepository = roundRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.scorecardRepository = scorecardRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.courseHoleRepository = courseHoleRepository;
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

        for (TripPlannedRound pr : plannedRounds) {

            Course course = courseRepository.findById(pr.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + pr.getCourseId()));

            CourseTee standardCourseTee = courseTeeRepository.findById(pr.getStandardTeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Standard tee not found: " + pr.getStandardTeeId()));

            if (!course.getId().equals(standardCourseTee.getCourse().getId())) {
                throw new IllegalArgumentException(
                        "Standard tee " + pr.getStandardTeeId() + " does not belong to course " + pr.getCourseId()
                );
            }

            CourseTee alternateCourseTee = null;
            if (pr.getAlternateTeeId() != null) {
                alternateCourseTee = courseTeeRepository.findById(pr.getAlternateTeeId())
                        .orElseThrow(() -> new IllegalArgumentException("Alternate tee not found: " + pr.getAlternateTeeId()));

                if (!course.getId().equals(alternateCourseTee.getCourse().getId())) {
                    throw new IllegalArgumentException(
                            "Alternate tee " + pr.getAlternateTeeId() + " does not belong to course " + pr.getCourseId()
                    );
                }
            }

            Round round = new Round();
            round.setTrip(trip);
            round.setRoundNumber(pr.getRoundNumber());
            round.setRoundDate(pr.getRoundDate());
            round.setCourse(course);
            round.setFormat(pr.getFormat());
            round.setHandicapPercent(100);
            round.setFinalized(false);

            round = roundRepository.save(round);

            RoundTee standardRoundTee = createRoundTee(round, course, standardCourseTee, RoundTeeRole.STANDARD);

            RoundTee alternateRoundTee = null;
            if (alternateCourseTee != null) {
                alternateRoundTee = createRoundTee(round, course, alternateCourseTee, RoundTeeRole.ALTERNATE);
            }

            round.setStandardRoundTee(standardRoundTee);
            round.setAlternateRoundTee(alternateRoundTee);
            round = roundRepository.save(round);

            for (TripPlayer tp : players) {
                Scorecard scorecard = new Scorecard();
                scorecard.setRound(round);
                scorecard.setPlayer(tp.getPlayer());
                scorecard.setRoundTee(standardRoundTee);

                roundHandicapService.populateCurrentHandicaps(scorecard, handicapGroupCode);

                scorecardRepository.save(scorecard);
            }
        }

        trip.setInitialized(true);
        trip.setStatus(TripStatus.IN_PROGRESS);
        tripRepository.save(trip);
    }

    private void validatePlannedRound(TripPlannedRound pr) {

        if (pr.getCourseId() == null) {
            throw new IllegalStateException("Planned round " + pr.getRoundNumber() + " is missing course.");
        }

        if (pr.getStandardTeeId() == null) {
            throw new IllegalStateException("Planned round " + pr.getRoundNumber() + " is missing standard tee.");
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
            RoundTeeRole teeRole
    ) {
        RoundTee rt = new RoundTee();
        rt.setRound(round);
        rt.setSourceCourseTee(courseTee);
        rt.setTeeRole(teeRole);
        rt.setCourseName(course.getName());
        rt.setTeeName(courseTee.getTeeName());
        rt.setCourseRating(courseTee.getCourseRating());
        rt.setSlope(courseTee.getSlope());
        rt.setParTotal(courseTee.getParTotal());
        rt = roundTeeRepository.save(rt);

        List<CourseHole> sourceHoles =
                courseHoleRepository.findByCourseTee_IdOrderByHoleNumberAsc(courseTee.getId());

        if (sourceHoles.size() != 18) {
            throw new IllegalStateException(
                    "Expected 18 holes for course tee " + courseTee.getId() + " but found " + sourceHoles.size()
            );
        }

        for (CourseHole sourceHole : sourceHoles) {
            RoundTeeHole roundTeeHole = new RoundTeeHole();
            roundTeeHole.setRoundTee(rt);
            roundTeeHole.setHoleNumber(sourceHole.getHoleNumber());
            roundTeeHole.setPar(sourceHole.getPar());
            roundTeeHole.setHandicap(sourceHole.getHandicap());
            roundTeeHole.setYardage(sourceHole.getYardage());
            roundTeeHoleRepository.save(roundTeeHole);
        }

        return rt;
    }
}
