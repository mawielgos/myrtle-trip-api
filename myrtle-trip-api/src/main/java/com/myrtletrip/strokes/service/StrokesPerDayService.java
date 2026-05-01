package com.myrtletrip.strokes.service;

import com.myrtletrip.handicap.service.CourseHandicapService;
import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.entity.RoundTeeHole;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.strokes.dto.StrokesPerDayPlayerResponse;
import com.myrtletrip.strokes.dto.StrokesPerDayPlayerRoundResponse;
import com.myrtletrip.strokes.dto.StrokesPerDayResponse;
import com.myrtletrip.strokes.dto.StrokesPerDayRoundResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final TripHandicapService tripHandicapService;
    private final CourseHandicapService courseHandicapService;
    private final ScorecardRepository scorecardRepository;

    public StrokesPerDayService(TripRepository tripRepository,
                                TripPlayerRepository tripPlayerRepository,
                                RoundRepository roundRepository,
                                RoundTeeHoleRepository roundTeeHoleRepository,
                                TripHandicapService tripHandicapService,
                                CourseHandicapService courseHandicapService,
                                ScorecardRepository scorecardRepository) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.roundRepository = roundRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.tripHandicapService = tripHandicapService;
        this.courseHandicapService = courseHandicapService;
        this.scorecardRepository = scorecardRepository;
    }

    public StrokesPerDayResponse getStrokesPerDay(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        List<Round> rounds = nonScrambleRounds(roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId));
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

    private StrokesPerDayRoundResponse toRoundResponse(Round round) {
        StrokesPerDayRoundResponse dto = new StrokesPerDayRoundResponse();
        dto.setRoundId(round.getId());
        dto.setRoundNumber(round.getRoundNumber());
        dto.setRoundDate(round.getRoundDate());
        if (round.getRoundDate() != null) {
            dto.setDayName(round.getRoundDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US));
        }

        RoundTee standardTee = round.getStandardRoundTee();
        RoundTee alternateTee = round.getAlternateRoundTee();

        dto.setCourseName(resolveCourseName(round, standardTee, alternateTee));
        dto.setCourseWebsiteUrl(round.getCourse() != null ? round.getCourse().getWebsiteUrl() : null);
        dto.setStandardTeeName(standardTee != null ? standardTee.getTeeName() : null);
        dto.setAlternateTeeName(alternateTee != null ? alternateTee.getTeeName() : null);
        dto.setStandardCourseRating(standardTee != null ? standardTee.getCourseRating() : null);
        dto.setAlternateCourseRating(alternateTee != null ? alternateTee.getCourseRating() : null);
        dto.setStandardSlope(standardTee != null ? standardTee.getSlope() : null);
        dto.setAlternateSlope(alternateTee != null ? alternateTee.getSlope() : null);
        dto.setStandardYardage(sumYardage(standardTee));
        dto.setAlternateYardage(sumYardage(alternateTee));

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
        dto.setStandardCourseHandicap(courseHandicapService.calculateCourseHandicap(tripIndex, round.getStandardRoundTee()));
        dto.setAlternateCourseHandicap(courseHandicapService.calculateCourseHandicap(tripIndex, round.getAlternateRoundTee()));
        dto.setStandardTeeSelected(false);
        dto.setAlternateTeeSelected(false);

        if (player != null && player.getId() != null && round.getId() != null) {
            Scorecard scorecard = scorecardRepository.findByRound_IdAndPlayer_Id(round.getId(), player.getId()).orElse(null);
            if (scorecard != null && scorecard.getRoundTee() != null && scorecard.getRoundTee().getId() != null) {
                Long selectedRoundTeeId = scorecard.getRoundTee().getId();

                RoundTee standardTee = round.getStandardRoundTee();
                if (standardTee != null && standardTee.getId() != null && standardTee.getId().equals(selectedRoundTeeId)) {
                    dto.setStandardTeeSelected(true);
                }

                RoundTee alternateTee = round.getAlternateRoundTee();
                if (alternateTee != null && alternateTee.getId() != null && alternateTee.getId().equals(selectedRoundTeeId)) {
                    dto.setAlternateTeeSelected(true);
                }
            }
        }

        return dto;
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
