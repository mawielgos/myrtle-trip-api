package com.myrtletrip.round.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundSetupRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RoundSetupService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final TripHandicapService tripHandicapService;

    public RoundSetupService(TripRepository tripRepository,
                             TripPlayerRepository tripPlayerRepository,
                             CourseRepository courseRepository,
                             CourseTeeRepository courseTeeRepository,
                             RoundRepository roundRepository,
                             ScorecardRepository scorecardRepository,
                             TripHandicapService tripHandicapService) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.tripHandicapService = tripHandicapService;
    }

    @Transactional
    public Long startRound(RoundSetupRequest request) {

        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        CourseTee tee = courseTeeRepository.findById(request.getCourseTeeId())
                .orElseThrow(() -> new IllegalArgumentException("CourseTee not found"));

        List<TripPlayer> roster = tripPlayerRepository.findByTrip(trip);

        if (roster.isEmpty()) {
            throw new IllegalStateException("Trip has no players");
        }

        RoundFormat format = request.getFormat();
        if (format == null) {
            throw new IllegalArgumentException("Round format is required");
        }

        Round round = new Round();
        round.setTrip(trip);
        round.setCourse(course);
        round.setCourseTee(tee);
        round.setRoundDate(request.getRoundDate());
        round.setFinalized(false);
        round.setFormat(format);

        round = roundRepository.save(round);

        for (TripPlayer tp : roster) {
            Player player = tp.getPlayer();

            int courseHandicap = calculateCourseHandicap(player, trip, tee);
            int playingHandicap = calculatePlayingHandicap(player, trip, tee, format, courseHandicap);

            Scorecard sc = new Scorecard();
            sc.setRound(round);
            sc.setPlayer(player);
            sc.setCourseHandicap(courseHandicap);
            sc.setPlayingHandicap(playingHandicap);

            scorecardRepository.save(sc);
        }

        return round.getId();
    }

    private int calculateCourseHandicap(Player player, Trip trip, CourseTee tee) {

        BigDecimal tripIndex = tripHandicapService.calculateTripIndex(player, trip.getTripCode());

        if (tripIndex == null) {
            return 0;
        }

        return tripIndex
                .multiply(BigDecimal.valueOf(tee.getSlope()))
                .divide(BigDecimal.valueOf(113), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int calculatePlayingHandicap(Player player,
                                         Trip trip,
                                         CourseTee tee,
                                         RoundFormat format,
                                         int courseHandicap) {
        // For now, playing handicap = course handicap.
        // Later this is where format-specific allowances can be applied.
        return courseHandicap;
    }
}