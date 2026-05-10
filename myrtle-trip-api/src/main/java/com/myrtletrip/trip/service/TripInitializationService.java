package com.myrtletrip.trip.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.entity.CourseTeeComboHole;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.prize.repository.PrizeWinningRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.entity.RoundTeeHole;
import com.myrtletrip.round.model.RoundTeeRole;
import com.myrtletrip.round.repository.RoundCorrectionLogRepository;
import com.myrtletrip.round.repository.RoundGroupPlayerRepository;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TripInitializationService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final RoundRepository roundRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundGroupPlayerRepository roundGroupPlayerRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final RoundCorrectionLogRepository roundCorrectionLogRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final RoundTeeHoleRepository roundTeeHoleRepository;
    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final CourseHoleRepository courseHoleRepository;
    private final CourseTeeComboHoleRepository courseTeeComboHoleRepository;
    private final RoundHandicapService roundHandicapService;
    private final PrizeScheduleRepository prizeScheduleRepository;
    private final PrizeWinningRepository prizeWinningRepository;
    private final TripService tripService;

    public TripInitializationService(
            TripRepository tripRepository,
            TripPlayerRepository tripPlayerRepository,
            TripPlannedRoundRepository tripPlannedRoundRepository,
            RoundRepository roundRepository,
            RoundGroupRepository roundGroupRepository,
            RoundGroupPlayerRepository roundGroupPlayerRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundCorrectionLogRepository roundCorrectionLogRepository,
            RoundTeeRepository roundTeeRepository,
            RoundTeeHoleRepository roundTeeHoleRepository,
            ScorecardRepository scorecardRepository,
            HoleScoreRepository holeScoreRepository,
            TeamHoleScoreRepository teamHoleScoreRepository,
            CourseRepository courseRepository,
            CourseTeeRepository courseTeeRepository,
            CourseHoleRepository courseHoleRepository,
            CourseTeeComboHoleRepository courseTeeComboHoleRepository,
            RoundHandicapService roundHandicapService,
            PrizeScheduleRepository prizeScheduleRepository,
            PrizeWinningRepository prizeWinningRepository,
            TripService tripService
    ) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.roundRepository = roundRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundGroupPlayerRepository = roundGroupPlayerRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundCorrectionLogRepository = roundCorrectionLogRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.courseHoleRepository = courseHoleRepository;
        this.courseTeeComboHoleRepository = courseTeeComboHoleRepository;
        this.roundHandicapService = roundHandicapService;
        this.prizeScheduleRepository = prizeScheduleRepository;
        this.prizeWinningRepository = prizeWinningRepository;
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
            round.setHandicapPercent(100);
            round.setFinalized(false);

            round = roundRepository.save(round);

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


    @Transactional
    public void resetTripStart(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (!Boolean.TRUE.equals(trip.getInitialized()) && !TripStatus.IN_PROGRESS.equals(trip.getStatus())) {
            throw new IllegalStateException("Trip has not been started.");
        }

        if (TripStatus.COMPLETE.equals(trip.getStatus())) {
            throw new IllegalStateException("A completed trip cannot be reset to planning.");
        }

        if (roundRepository.countByTrip_IdAndFinalizedTrue(tripId) > 0) {
            throw new IllegalStateException("Cannot reset trip start after any round has been finalized.");
        }

        if (scorecardRepository.countByRound_Trip_IdAndGrossScoreIsNotNull(tripId) > 0
                || scorecardRepository.countByRound_Trip_IdAndAdjustedGrossScoreIsNotNull(tripId) > 0
                || scorecardRepository.countByRound_Trip_IdAndNetScoreIsNotNull(tripId) > 0
                || holeScoreRepository.countByScorecard_Round_Trip_IdAndStrokesIsNotNull(tripId) > 0
                || roundTeamRepository.countByRound_Trip_IdAndScrambleTotalScoreIsNotNull(tripId) > 0
                || teamHoleScoreRepository.countByRoundTeam_Round_Trip_Id(tripId) > 0) {
            throw new IllegalStateException("Cannot reset trip start after scoring has begun.");
        }

        if (roundCorrectionLogRepository.countByRound_Trip_Id(tripId) > 0) {
            throw new IllegalStateException("Cannot reset trip start after corrections have been recorded.");
        }

        // Delete initialized round artifacts only. Planned rounds, roster, handicap setup,
        // prize payout schedules, and manual score-history remain intact so the user can fix setup
        // and start the same trip again.
        //
        // Prize schedules are setup-level rows, but round-specific schedules point at initialized
        // round IDs. Clear those FK links before deleting rounds so the existing payout amounts
        // survive reset-start and can be reattached to the newly-created rounds on the next start.
        prizeWinningRepository.deleteByTrip_Id(tripId);
        prizeScheduleRepository.clearRoundReferencesByTripId(tripId);

        teamHoleScoreRepository.deleteByRoundTeam_Round_Trip_Id(tripId);
        holeScoreRepository.deleteByScorecard_Round_Trip_Id(tripId);
        scorecardRepository.deleteByRound_Trip_Id(tripId);

        roundGroupPlayerRepository.deleteByRoundGroup_Round_Trip_Id(tripId);
        roundGroupRepository.deleteByRound_Trip_Id(tripId);

        roundTeamPlayerRepository.deleteByRoundTeam_Round_Trip_Id(tripId);
        roundTeamRepository.deleteByRound_Trip_Id(tripId);

        roundCorrectionLogRepository.deleteByRound_Trip_Id(tripId);
        roundRepository.clearDefaultRoundTeeByTripId(tripId);
        roundTeeHoleRepository.deleteByRoundTee_Round_Trip_Id(tripId);
        roundTeeRepository.deleteByRound_Trip_Id(tripId);
        roundRepository.deleteByTrip_Id(tripId);

        trip.setInitialized(false);
        trip.setStatus(TripStatus.PLANNING);
        tripRepository.save(trip);
    }

    private Integer resolveScrambleTeamSize(TripPlannedRound plannedRound) {
        if (plannedRound == null || plannedRound.getFormat() != com.myrtletrip.round.model.RoundFormat.TEAM_SCRAMBLE) {
            return 4;
        }

        Integer size = plannedRound.getScrambleTeamSize();
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
