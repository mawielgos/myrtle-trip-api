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

import java.util.List;

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

        CourseTee standardCourseTee = courseTeeRepository.findById(request.getStandardCourseTeeId())
                .orElseThrow(() -> new IllegalArgumentException("Standard course tee not found"));

        if (!standardCourseTee.getCourse().getId().equals(course.getId())) {
            throw new IllegalArgumentException("Selected standard tee does not belong to the selected course");
        }

        CourseTee alternateCourseTee = null;
        if (request.getAlternateCourseTeeId() != null) {
            alternateCourseTee = courseTeeRepository.findById(request.getAlternateCourseTeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Alternate course tee not found"));

            if (!alternateCourseTee.getCourse().getId().equals(course.getId())) {
                throw new IllegalArgumentException("Selected alternate tee does not belong to the selected course");
            }
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

        RoundTee standardRoundTee = createRoundTee(round, standardCourseTee, RoundTeeRole.STANDARD);
        RoundTee alternateRoundTee = alternateCourseTee != null
                ? createRoundTee(round, alternateCourseTee, RoundTeeRole.ALTERNATE)
                : null;

        round.setStandardRoundTee(standardRoundTee);
        round.setAlternateRoundTee(alternateRoundTee);
        round = roundRepository.save(round);

        String handicapGroupCode = trip.getTripCode();

        for (TripPlayer tripPlayer : roster) {
            Player player = tripPlayer.getPlayer();

            Scorecard scorecard = new Scorecard();
            scorecard.setRound(round);
            scorecard.setPlayer(player);
            scorecard.setRoundTee(standardRoundTee);

            roundHandicapService.populateCurrentHandicaps(scorecard, handicapGroupCode);

            scorecardRepository.save(scorecard);
        }

        return round.getId();
    }

    private int determineNextRoundNumber(Long tripId) {
        List<Round> existingRounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);

        if (existingRounds == null || existingRounds.isEmpty()) {
            return 1;
        }

        int maxRoundNumber = 0;

        for (Round existingRound : existingRounds) {
            if (existingRound == null || existingRound.getRoundNumber() == null) {
                continue;
            }

            if (existingRound.getRoundNumber() > maxRoundNumber) {
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
            throw new IllegalStateException(
                    "Expected 18 holes for course tee " + sourceCourseTee.getId() + " but found " + sourceHoles.size()
            );
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
        if (handicapPercent == null) {
            return 100;
        }

        if (handicapPercent < 0 || handicapPercent > 100) {
            throw new IllegalArgumentException("handicapPercent must be between 0 and 100");
        }

        return handicapPercent;
    }
}
