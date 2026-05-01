package com.myrtletrip.round.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundSetupRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.entity.RoundTeeHole;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.model.RoundTeeRole;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundSetupService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final CourseHoleRepository courseHoleRepository;
    private final RoundRepository roundRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final RoundTeeHoleRepository roundTeeHoleRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundHandicapService roundHandicapService;

    public RoundSetupService(TripRepository tripRepository,
                             TripPlayerRepository tripPlayerRepository,
                             CourseRepository courseRepository,
                             CourseTeeRepository courseTeeRepository,
                             CourseHoleRepository courseHoleRepository,
                             RoundRepository roundRepository,
                             RoundTeeRepository roundTeeRepository,
                             RoundTeeHoleRepository roundTeeHoleRepository,
                             ScorecardRepository scorecardRepository,
                             RoundHandicapService roundHandicapService) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.courseHoleRepository = courseHoleRepository;
        this.roundRepository = roundRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundHandicapService = roundHandicapService;
    }

    @Transactional
    public Long startRound(RoundSetupRequest request) {
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        CourseTee defaultCourseTee = courseTeeRepository.findById(request.getDefaultCourseTeeId())
                .orElseThrow(() -> new IllegalArgumentException("Default course tee not found"));

        if (!defaultCourseTee.getCourse().getId().equals(course.getId())) {
            throw new IllegalArgumentException("Selected default tee does not belong to the selected course");
        }

        List<TripPlayer> roster = tripPlayerRepository.findByTrip(trip);
        if (roster.isEmpty()) {
            throw new IllegalStateException("Trip has no players");
        }

        RoundFormat format = request.getFormat();
        if (format == null) {
            throw new IllegalArgumentException("Round format is required");
        }
        if (request.getRoundDate() == null) {
            throw new IllegalArgumentException("Round date is required");
        }

        int handicapPercent = normalizeHandicapPercent(request.getHandicapPercent());
        int nextRoundNumber = determineNextRoundNumber(trip.getId());

        Round round = new Round();
        round.setTrip(trip);
        round.setRoundNumber(nextRoundNumber);
        round.setCourse(course);
        round.setRoundDate(request.getRoundDate());
        round.setFinalized(false);
        round.setFormat(format);
        round.setHandicapPercent(handicapPercent);
        round = roundRepository.save(round);

        Map<Long, RoundTee> roundTeeByCourseTeeId = createRoundTeeOptions(round, course, defaultCourseTee.getId());
        RoundTee defaultRoundTee = roundTeeByCourseTeeId.get(defaultCourseTee.getId());
        if (defaultRoundTee == null) {
            throw new IllegalStateException("Default round tee was not created");
        }

        round.setDefaultRoundTee(defaultRoundTee);
        round = roundRepository.save(round);

        String handicapGroupCode = trip.getTripCode();
        for (TripPlayer tripPlayer : roster) {
            Player player = tripPlayer.getPlayer();

            Scorecard scorecard = new Scorecard();
            scorecard.setRound(round);
            scorecard.setPlayer(player);
            scorecard.setRoundTee(defaultRoundTee);

            roundHandicapService.populateCurrentHandicaps(scorecard, handicapGroupCode);
            scorecardRepository.save(scorecard);
        }

        return round.getId();
    }

    private Map<Long, RoundTee> createRoundTeeOptions(Round round, Course course, Long defaultCourseTeeId) {
        List<CourseTee> courseTees = courseTeeRepository.findByCourse_IdAndActiveTrueOrderByTeeNameAscEffectiveDateDesc(course.getId());
        if (courseTees == null || courseTees.isEmpty()) {
            throw new IllegalStateException("Course has no active tees");
        }

        Map<Long, RoundTee> result = new HashMap<>();
        for (CourseTee courseTee : courseTees) {
            if (courseTee == null || courseTee.getId() == null) {
                continue;
            }
            if (!courseTee.isEligibleForGender("M") && !courseTee.isEligibleForGender("F")) {
                continue;
            }

            RoundTeeRole role = courseTee.getId().equals(defaultCourseTeeId)
                    ? RoundTeeRole.DEFAULT
                    : RoundTeeRole.PLAYER_OPTION;
            RoundTee roundTee = createRoundTee(round, courseTee, role);
            result.put(courseTee.getId(), roundTee);
        }

        return result;
    }

    private int determineNextRoundNumber(Long tripId) {
        List<Round> existingRounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        if (existingRounds == null || existingRounds.isEmpty()) {
            return 1;
        }

        int maxRoundNumber = 0;
        for (Round existingRound : existingRounds) {
            if (existingRound != null && existingRound.getRoundNumber() != null && existingRound.getRoundNumber() > maxRoundNumber) {
                maxRoundNumber = existingRound.getRoundNumber();
            }
        }
        return maxRoundNumber + 1;
    }

    private RoundTee createRoundTee(Round round, CourseTee sourceCourseTee, RoundTeeRole role) {
        RoundTee roundTee = new RoundTee();
        roundTee.setRound(round);
        roundTee.setSourceCourseTee(sourceCourseTee);
        roundTee.setTeeRole(role);
        roundTee.setCourseName(round.getCourse().getName());
        roundTee.setTeeName(sourceCourseTee.getTeeName());
        roundTee.setCourseRating(sourceCourseTee.getCourseRating());
        roundTee.setSlope(sourceCourseTee.getSlope());
        roundTee.setParTotal(sourceCourseTee.getParTotal());
        roundTee = roundTeeRepository.save(roundTee);

        List<CourseHole> sourceHoles = courseHoleRepository.findByCourseTee_IdOrderByHoleNumberAsc(sourceCourseTee.getId());
        if (sourceHoles.size() != 18) {
            throw new IllegalStateException("Expected 18 holes for course tee " + sourceCourseTee.getId() + " but found " + sourceHoles.size());
        }

        for (CourseHole sourceHole : sourceHoles) {
            RoundTeeHole roundTeeHole = new RoundTeeHole();
            roundTeeHole.setRoundTee(roundTee);
            roundTeeHole.setHoleNumber(sourceHole.getHoleNumber());
            roundTeeHole.setPar(sourceHole.getPar());
            roundTeeHole.setHandicap(sourceHole.getHandicap());
            roundTeeHole.setYardage(sourceHole.getYardage());
            roundTeeHoleRepository.save(roundTeeHole);
        }

        return roundTee;
    }

    private int normalizeHandicapPercent(Integer handicapPercent) {
        if (handicapPercent == null) return 100;
        if (handicapPercent < 0 || handicapPercent > 100) {
            throw new IllegalArgumentException("handicapPercent must be between 0 and 100");
        }
        return handicapPercent;
    }
}
