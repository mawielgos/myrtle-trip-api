package com.myrtletrip.strokes.service;

import com.myrtletrip.handicap.service.CourseHandicapService;
import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.entity.RoundTeeHole;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.round.service.RoundTeeProvisioningService;
import com.myrtletrip.round.service.ScorecardHandicapService;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.strokes.dto.StrokesPerDayPlayerResponse;
import com.myrtletrip.strokes.dto.StrokesPerDayPlayerRoundResponse;
import com.myrtletrip.strokes.dto.StrokesPerDayResponse;
import com.myrtletrip.strokes.dto.StrokesPerDayRoundResponse;
import com.myrtletrip.strokes.dto.StrokesPerDayTeeOptionResponse;
import com.myrtletrip.strokes.dto.StrokesPerDayTeePlanItemRequest;
import com.myrtletrip.strokes.dto.StrokesPerDayTeePlanSaveRequest;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class StrokesPerDayService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final RoundRepository roundRepository;
    private final RoundTeeHoleRepository roundTeeHoleRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final RoundTeeProvisioningService roundTeeProvisioningService;
    private final TripHandicapService tripHandicapService;
    private final CourseHandicapService courseHandicapService;
    private final ScorecardRepository scorecardRepository;
    private final ScorecardHandicapService scorecardHandicapService;

    public StrokesPerDayService(TripRepository tripRepository,
                                TripPlayerRepository tripPlayerRepository,
                                RoundRepository roundRepository,
                                RoundTeeHoleRepository roundTeeHoleRepository,
                                RoundTeeRepository roundTeeRepository,
                                RoundTeeProvisioningService roundTeeProvisioningService,
                                TripHandicapService tripHandicapService,
                                CourseHandicapService courseHandicapService,
                                ScorecardRepository scorecardRepository,
                                ScorecardHandicapService scorecardHandicapService) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.roundRepository = roundRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.roundTeeProvisioningService = roundTeeProvisioningService;
        this.tripHandicapService = tripHandicapService;
        this.courseHandicapService = courseHandicapService;
        this.scorecardRepository = scorecardRepository;
        this.scorecardHandicapService = scorecardHandicapService;
    }

    @Transactional
    public StrokesPerDayResponse getStrokesPerDay(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        List<Round> rounds = nonScrambleRounds(roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId));
        for (Round round : rounds) {
            roundTeeProvisioningService.ensureRoundTeeOptions(round);
        }
        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(tripId);

        StrokesPerDayResponse response = new StrokesPerDayResponse();
        response.setTripId(trip.getId());
        response.setTripName(trip.getName());
        response.setTripCode(trip.getTripCode());
        response.setTripYear(trip.getTripYear());

        List<StrokesPerDayRoundResponse> roundResponses = new ArrayList<>();
        for (Round round : rounds) {
            roundResponses.add(toRoundResponse(round));
        }
        response.setRounds(roundResponses);

        List<StrokesPerDayPlayerResponse> playerResponses = new ArrayList<>();
        for (TripPlayer tripPlayer : tripPlayers) {
            playerResponses.add(toPlayerResponse(tripPlayer, rounds, trip.getTripCode()));
        }
        response.setPlayers(playerResponses);

        return response;
    }



    @Transactional
    public StrokesPerDayResponse saveTeePlan(Long tripId, StrokesPerDayTeePlanSaveRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        if (request == null || request.getChanges() == null || request.getChanges().isEmpty()) {
            return getStrokesPerDay(trip.getId());
        }

        for (StrokesPerDayTeePlanItemRequest change : request.getChanges()) {
            applyTeePlanChange(trip.getId(), change);
        }

        return getStrokesPerDay(trip.getId());
    }

    private void applyTeePlanChange(Long tripId, StrokesPerDayTeePlanItemRequest change) {
        if (change == null) {
            throw new IllegalArgumentException("Tee plan change is required");
        }
        if (change.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId is required");
        }
        if (change.getRoundId() == null) {
            throw new IllegalArgumentException("roundId is required");
        }
        if (change.getRoundTeeId() == null) {
            throw new IllegalArgumentException("roundTeeId is required");
        }

        Round round = roundRepository.findById(change.getRoundId())
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + change.getRoundId()));

        if (round.getTrip() == null || round.getTrip().getId() == null || !round.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("Round " + change.getRoundId() + " does not belong to trip " + tripId);
        }

        String statusCode = resolveRoundStatusCode(round);
        if (!"PLANNING".equals(statusCode)) {
            throw new IllegalStateException("Tee planning is locked for round " + round.getId() + " because it is " + resolveRoundStatusLabel(statusCode));
        }

        RoundTee roundTee = roundTeeRepository.findById(change.getRoundTeeId())
                .orElseThrow(() -> new IllegalArgumentException("Round tee not found: " + change.getRoundTeeId()));
        if (roundTee.getRound() == null || roundTee.getRound().getId() == null
                || !roundTee.getRound().getId().equals(round.getId())) {
            throw new IllegalArgumentException("Round tee " + change.getRoundTeeId() + " does not belong to round " + round.getId());
        }

        Scorecard scorecard = scorecardRepository.findByRound_IdAndPlayer_Id(round.getId(), change.getPlayerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Scorecard not found for round " + round.getId() + " and player " + change.getPlayerId()
                ));

        scorecardHandicapService.setScorecardTee(scorecard.getId(), roundTee.getId());
    }


    private StrokesPerDayRoundResponse toRoundResponse(Round round) {
        StrokesPerDayRoundResponse dto = new StrokesPerDayRoundResponse();
        dto.setRoundId(round.getId());
        dto.setRoundNumber(round.getRoundNumber());
        dto.setRoundDate(round.getRoundDate());
        if (round.getRoundDate() != null) {
            dto.setDayName(round.getRoundDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US));
        }

        RoundTee defaultTee = round.getDefaultRoundTee();

        dto.setCourseName(resolveCourseName(round, defaultTee, null));
        dto.setCourseWebsiteUrl(round.getCourse() != null ? round.getCourse().getWebsiteUrl() : null);
        dto.setStandardTeeName(defaultTee != null ? defaultTee.getTeeName() : null);
        dto.setAlternateTeeName(null);
        dto.setStandardCourseRating(defaultTee != null ? defaultTee.getCourseRating() : null);
        dto.setAlternateCourseRating(null);
        dto.setStandardSlope(defaultTee != null ? defaultTee.getSlope() : null);
        dto.setAlternateSlope(null);
        dto.setStandardYardage(sumYardage(defaultTee));
        dto.setAlternateYardage(null);

        String statusCode = resolveRoundStatusCode(round);
        dto.setStatusCode(statusCode);
        dto.setStatusLabel(resolveRoundStatusLabel(statusCode));
        dto.setTeePlanningLocked(!"PLANNING".equals(statusCode));

        return dto;
    }

    private StrokesPerDayPlayerResponse toPlayerResponse(TripPlayer tripPlayer,
                                                         List<Round> rounds,
                                                         String handicapGroupCode) {
        Player player = tripPlayer.getPlayer();

        StrokesPerDayPlayerResponse dto = new StrokesPerDayPlayerResponse();
        dto.setPlayerId(player != null ? player.getId() : null);
        dto.setPlayerName(player != null ? player.getDisplayName() : null);
        dto.setDisplayOrder(tripPlayer.getDisplayOrder());

        List<StrokesPerDayPlayerRoundResponse> roundResponses = new ArrayList<>();
        for (Round round : rounds) {
            roundResponses.add(toPlayerRoundResponse(player, round, handicapGroupCode));
        }
        dto.setRounds(roundResponses);

        return dto;
    }

    private StrokesPerDayPlayerRoundResponse toPlayerRoundResponse(Player player,
                                                                   Round round,
                                                                   String handicapGroupCode) {
        StrokesPerDayPlayerRoundResponse dto = new StrokesPerDayPlayerRoundResponse();
        dto.setRoundId(round.getId());
        dto.setRoundNumber(round.getRoundNumber());

        BigDecimal tripIndex = null;
        if (player != null && handicapGroupCode != null && !handicapGroupCode.isBlank() && round.getRoundDate() != null) {
            tripIndex = tripHandicapService.calculateTripIndexAsOf(player, handicapGroupCode, round.getRoundDate());
        }
        dto.setTripIndex(tripIndex);

        RoundTee defaultTee = round.getDefaultRoundTee();
        dto.setStandardCourseHandicap(courseHandicapService.calculateCourseHandicap(tripIndex, defaultTee));
        dto.setAlternateCourseHandicap(null);
        dto.setStandardTeeSelected(false);
        dto.setAlternateTeeSelected(false);

        Scorecard scorecard = null;
        if (player != null && player.getId() != null && round.getId() != null) {
            scorecard = scorecardRepository.findByRound_IdAndPlayer_Id(round.getId(), player.getId()).orElse(null);
        }

        RoundTee selectedTee = defaultTee;
        if (scorecard != null && scorecard.getRoundTee() != null) {
            selectedTee = scorecard.getRoundTee();
        }

        String playerGender = player == null ? "M" : normalizeGender(player.getGender());

        if (selectedTee != null) {
            dto.setSelectedRoundTeeId(selectedTee.getId());
            dto.setSelectedTeeName(selectedTee.getTeeName());
            dto.setSelectedCourseRating(resolveCourseRatingForGender(selectedTee, playerGender));
            dto.setSelectedSlope(resolveSlopeForGender(selectedTee, playerGender));
            dto.setSelectedYardage(sumYardage(selectedTee));

            Integer selectedCourseHandicap = calculateCourseHandicap(
                    tripIndex,
                    resolveCourseRatingForGender(selectedTee, playerGender),
                    resolveSlopeForGender(selectedTee, playerGender),
                    resolveParForGender(selectedTee, playerGender)
            );
            dto.setSelectedCourseHandicap(selectedCourseHandicap);
            dto.setSelectedPlayingHandicap(calculatePlayingHandicap(selectedCourseHandicap, round.getHandicapPercent()));

            if (defaultTee != null && defaultTee.getId() != null && defaultTee.getId().equals(selectedTee.getId())) {
                dto.setStandardTeeSelected(true);
            }
        }

        if (scorecard != null) {
            if (scorecard.getCourseHandicap() != null) {
                dto.setSelectedCourseHandicap(scorecard.getCourseHandicap());
            }
            if (scorecard.getPlayingHandicap() != null) {
                dto.setSelectedPlayingHandicap(scorecard.getPlayingHandicap());
            }
        }

        dto.setEligibleTeeOptions(buildEligibleTeeOptions(round, playerGender, tripIndex, round.getHandicapPercent(), selectedTee));

        return dto;
    }


    private List<StrokesPerDayTeeOptionResponse> buildEligibleTeeOptions(Round round,
                                                                         String playerGender,
                                                                         BigDecimal tripIndex,
                                                                         Integer handicapPercent,
                                                                         RoundTee selectedTee) {
        List<StrokesPerDayTeeOptionResponse> result = new ArrayList<>();
        if (round == null || round.getId() == null) {
            return result;
        }

        List<RoundTee> roundTees = roundTeeRepository.findByRound_IdOrderByTeeNameAsc(round.getId());
        for (RoundTee roundTee : roundTees) {
            if (!isEligibleForGender(roundTee, playerGender)) {
                continue;
            }

            BigDecimal courseRating = resolveCourseRatingForGender(roundTee, playerGender);
            Integer slope = resolveSlopeForGender(roundTee, playerGender);
            Integer parTotal = resolveParForGender(roundTee, playerGender);
            Integer yardage = sumYardage(roundTee);
            Integer courseHandicap = calculateCourseHandicap(tripIndex, courseRating, slope, parTotal);
            Integer playingHandicap = calculatePlayingHandicap(courseHandicap, handicapPercent);

            StrokesPerDayTeeOptionResponse option = new StrokesPerDayTeeOptionResponse();
            option.setRoundTeeId(roundTee.getId());
            option.setSourceCourseTeeId(
                    roundTee.getSourceCourseTee() == null ? null : roundTee.getSourceCourseTee().getId()
            );
            option.setTeeName(roundTee.getTeeName());
            option.setCourseRating(courseRating);
            option.setSlope(slope);
            option.setParTotal(parTotal);
            option.setYardage(yardage);
            option.setCourseHandicap(courseHandicap);
            option.setPlayingHandicap(playingHandicap);
            option.setSelected(selectedTee != null && selectedTee.getId() != null && selectedTee.getId().equals(roundTee.getId()));
            option.setDisplayName(buildTeeOptionDisplayName(roundTee.getTeeName(), courseRating, slope, yardage, playingHandicap));

            result.add(option);
        }

        result.sort((a, b) -> {
            BigDecimal aRating = a.getCourseRating();
            BigDecimal bRating = b.getCourseRating();

            if (aRating == null && bRating == null) {
                return safeString(a.getTeeName()).compareToIgnoreCase(safeString(b.getTeeName()));
            }
            if (aRating == null) {
                return 1;
            }
            if (bRating == null) {
                return -1;
            }

            int ratingCompare = bRating.compareTo(aRating);
            if (ratingCompare != 0) {
                return ratingCompare;
            }

            return safeString(a.getTeeName()).compareToIgnoreCase(safeString(b.getTeeName()));
        });

        return result;
    }

    private boolean isEligibleForGender(RoundTee roundTee, String gender) {
        if (roundTee == null) {
            return false;
        }
        CourseTee sourceTee = roundTee.getSourceCourseTee();
        if (sourceTee == null) {
            return roundTee.getCourseRating() != null && roundTee.getSlope() != null && roundTee.getParTotal() != null;
        }
        return sourceTee.isEligibleForGender(gender);
    }

    private BigDecimal resolveCourseRatingForGender(RoundTee roundTee, String gender) {
        if (roundTee == null) {
            return null;
        }
        CourseTee sourceTee = roundTee.getSourceCourseTee();
        if (sourceTee != null) {
            return sourceTee.getRatingForGender(gender);
        }
        return roundTee.getCourseRating();
    }

    private Integer resolveSlopeForGender(RoundTee roundTee, String gender) {
        if (roundTee == null) {
            return null;
        }
        CourseTee sourceTee = roundTee.getSourceCourseTee();
        if (sourceTee != null) {
            return sourceTee.getSlopeForGender(gender);
        }
        return roundTee.getSlope();
    }

    private Integer resolveParForGender(RoundTee roundTee, String gender) {
        if (roundTee == null) {
            return null;
        }
        CourseTee sourceTee = roundTee.getSourceCourseTee();
        if (sourceTee != null) {
            return sourceTee.getParForGender(gender);
        }
        return roundTee.getParTotal();
    }

    private Integer calculateCourseHandicap(BigDecimal handicapIndex,
                                            BigDecimal courseRating,
                                            Integer slope,
                                            Integer parTotal) {
        if (handicapIndex == null || courseRating == null || slope == null || parTotal == null) {
            return null;
        }

        BigDecimal courseHandicap = handicapIndex
                .multiply(BigDecimal.valueOf(slope))
                .divide(BigDecimal.valueOf(113), 10, RoundingMode.HALF_UP)
                .add(courseRating.subtract(BigDecimal.valueOf(parTotal)));

        return courseHandicap.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private String buildTeeOptionDisplayName(String teeName,
                                             BigDecimal courseRating,
                                             Integer slope,
                                             Integer yardage,
                                             Integer playingHandicap) {
        StringBuilder sb = new StringBuilder();
        sb.append(teeName == null || teeName.trim().isEmpty() ? "Tee" : teeName.trim());

        List<String> parts = new ArrayList<>();
        if (courseRating != null || slope != null) {
            parts.add((courseRating == null ? "—" : courseRating.toPlainString())
                    + " / "
                    + (slope == null ? "—" : slope.toString()));
        }
        if (yardage != null) {
            parts.add(String.format("%,d yds", yardage));
        }
        if (playingHandicap != null) {
            parts.add(formatStrokesLabel(playingHandicap) + " strokes");
        }

        if (!parts.isEmpty()) {
            sb.append(" — ");
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) {
                    sb.append(" • ");
                }
                sb.append(parts.get(i));
            }
        }

        return sb.toString();
    }

    private String formatStrokesLabel(Integer strokes) {
        if (strokes == null) {
            return "";
        }
        if (strokes < 0) {
            return "+" + Math.abs(strokes);
        }
        return strokes.toString();
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return "M";
        }
        return gender.trim().toUpperCase(Locale.US);
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String resolveRoundStatusCode(Round round) {
        if (round != null && Boolean.TRUE.equals(round.getFinalized())) {
            return "FINALIZED";
        }

        if (round != null && round.getId() != null) {
            List<Scorecard> scorecards = scorecardRepository.findByRound_Id(round.getId());
            for (Scorecard scorecard : scorecards) {
                if (scorecard.getThruHole() != null && scorecard.getThruHole() > 0) {
                    return "IN_PROGRESS";
                }
                if (scorecard.getGrossScore() != null || scorecard.getNetScore() != null) {
                    return "IN_PROGRESS";
                }
            }
        }

        return "PLANNING";
    }

    private String resolveRoundStatusLabel(String statusCode) {
        if ("FINALIZED".equals(statusCode)) {
            return "Finalized";
        }
        if ("IN_PROGRESS".equals(statusCode)) {
            return "In Progress";
        }
        return "Planning";
    }

    private Integer calculatePlayingHandicap(Integer courseHandicap, Integer handicapPercent) {
        if (courseHandicap == null) {
            return null;
        }

        int percent = handicapPercent == null ? 100 : handicapPercent;
        if (percent < 0) {
            percent = 0;
        }
        if (percent > 100) {
            percent = 100;
        }

        return (int) Math.round(courseHandicap * (percent / 100.0));
    }

    private List<Round> nonScrambleRounds(List<Round> rounds) {
        List<Round> filtered = new ArrayList<>();

        for (Round round : rounds) {
            if (round.getFormat() != RoundFormat.TEAM_SCRAMBLE) {
                filtered.add(round);
            }
        }

        return filtered;
    }

    private String resolveCourseName(Round round, RoundTee standardTee, RoundTee alternateTee) {
        if (round != null && round.getCourse() != null && round.getCourse().getName() != null) {
            return round.getCourse().getName();
        }
        if (standardTee != null && standardTee.getCourseName() != null) {
            return standardTee.getCourseName();
        }
        if (alternateTee != null && alternateTee.getCourseName() != null) {
            return alternateTee.getCourseName();
        }
        return null;
    }

    private Integer sumYardage(RoundTee roundTee) {
        if (roundTee == null || roundTee.getId() == null) {
            return null;
        }

        List<RoundTeeHole> holes = roundTeeHoleRepository.findByRoundTee_IdOrderByHoleNumberAsc(roundTee.getId());
        int total = 0;
        boolean hasYardage = false;

        for (RoundTeeHole hole : holes) {
            if (hole.getYardage() != null) {
                total += hole.getYardage();
                hasYardage = true;
            }
        }

        return hasYardage ? total : null;
    }
}
