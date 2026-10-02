package com.myrtletrip.round.service;

import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.RoundTeamPlayerResponse;
import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.dto.RoundTeeOptionResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RoundTeamAssignmentReadModelService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundTeeResolver roundTeeResolver;
    private final RoundTeeProvisioningService roundTeeProvisioningService;
    private final TripHandicapService tripHandicapService;
    private final TripPlayerRepository tripPlayerRepository;
    private final RoundCapabilityService roundCapabilityService;
    private final RoundEventCapabilityService roundEventCapabilityService;
    private final RoundScrambleSeedingService roundScrambleSeedingService;

    public RoundTeamAssignmentReadModelService(
            RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundTeeRepository roundTeeRepository,
            ScorecardRepository scorecardRepository,
            RoundTeeResolver roundTeeResolver,
            RoundTeeProvisioningService roundTeeProvisioningService,
            TripHandicapService tripHandicapService,
            TripPlayerRepository tripPlayerRepository,
            RoundCapabilityService roundCapabilityService,
            RoundEventCapabilityService roundEventCapabilityService,
            RoundScrambleSeedingService roundScrambleSeedingService
    ) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundTeeResolver = roundTeeResolver;
        this.roundTeeProvisioningService = roundTeeProvisioningService;
        this.tripHandicapService = tripHandicapService;
        this.tripPlayerRepository = tripPlayerRepository;
        this.roundCapabilityService = roundCapabilityService;
        this.roundEventCapabilityService = roundEventCapabilityService;
        this.roundScrambleSeedingService = roundScrambleSeedingService;
    }

    @Transactional
    public RoundTeamAssignmentPageResponse getAssignmentPage(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        roundTeeProvisioningService.ensureRoundTeeOptions(round);

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
        List<Scorecard> allScorecards = scorecardRepository.findByRound_Id(roundId);
        List<Scorecard> scorecards = allScorecards.stream()
                .filter(this::isActiveScorecard)
                .toList();
        List<Scorecard> inactiveScorecards = allScorecards.stream()
                .filter(scorecard -> !isActiveScorecard(scorecard))
                .toList();

        RoundTeamAssignmentPageResponse response = new RoundTeamAssignmentPageResponse();
        response.setRoundId(round.getId());
        response.setDefaultRoundTeeId(round.getDefaultRoundTee() == null ? null : round.getDefaultRoundTee().getId());
        response.setScrambleTeamSize(roundScrambleSeedingService.resolveScrambleTeamSize(round));
        response.setScrambleSeedingMethod(roundScrambleSeedingService.resolveScrambleSeedingMethod(round));
        response.setScrambleHandicapDate(roundScrambleSeedingService.resolveScrambleHandicapDate(round));
        response.setCapabilities(roundCapabilityService.build(round));

        LocalDate seedingAsOfDate = roundScrambleSeedingService.determineSeedingAsOfDate(round);
        response.setSeedingAsOfDate(seedingAsOfDate);
        response.setSeedingLabel(roundScrambleSeedingService.buildSeedingLabel(round, seedingAsOfDate));
        response.setScrambleSeedingRounds(roundScrambleSeedingService.mapScrambleSeedingRounds(round));
        response.setTeeOptions(mapRoundTeeOptions(roundId));

        List<RoundTeamResponse> teamResponses = new ArrayList<>();
        List<RoundTeamPlayerResponse> unassignedPlayers = new ArrayList<>();

        for (RoundTeam team : teams) {
            RoundTeamResponse teamResponse = new RoundTeamResponse();
            teamResponse.setRoundTeamId(team.getId());
            teamResponse.setTeamNumber(team.getTeamNumber());
            teamResponse.setTeamName(team.getTeamName());

            List<RoundTeamPlayerResponse> players = roundTeamPlayerRepository
                    .findByRoundTeam_IdOrderByPlayerOrderAsc(team.getId())
                    .stream()
                    .map(roundTeamPlayer -> mapAssignedPlayer(roundTeamPlayer, roundId, round, seedingAsOfDate))
                    .toList();

            teamResponse.setPlayers(players);
            teamResponses.add(teamResponse);
        }

        scorecards.stream()
                .filter(scorecard -> scorecard.getTeam() == null || scorecard.getTeam().getId() == null)
                .sorted(Comparator.comparing(scorecard -> buildPlayerName(scorecard.getPlayer()), String.CASE_INSENSITIVE_ORDER))
                .map(scorecard -> mapUnassignedPlayer(scorecard, round, seedingAsOfDate))
                .forEach(unassignedPlayers::add);

        response.setTeams(teamResponses);
        response.setUnassignedPlayers(unassignedPlayers);
        response.setInactivePlayers(inactiveScorecards.stream()
                .sorted(Comparator.comparing(scorecard -> buildPlayerName(scorecard.getPlayer()), String.CASE_INSENSITIVE_ORDER))
                .map(scorecard -> mapUnavailablePlayer(scorecard, round, seedingAsOfDate))
                .toList());

        return response;
    }
    private List<RoundTeeOptionResponse> mapRoundTeeOptions(Long roundId) {
        List<RoundTee> tees = roundTeeRepository.findByRound_IdOrderByTeeNameAsc(roundId);
        List<RoundTeeOptionResponse> result = new ArrayList<>();

        for (RoundTee tee : tees) {
            RoundTeeOptionResponse response = new RoundTeeOptionResponse();

            CourseTee sourceTee = tee.getSourceCourseTee();

            response.setRoundTeeId(tee.getId());
            response.setSourceCourseTeeId(sourceTee == null ? null : sourceTee.getId());
            response.setTeeName(tee.getTeeName());

            if (sourceTee == null) {
                response.setMenCourseRating(tee.getCourseRating());
                response.setMenSlope(tee.getSlope());
                response.setMenParTotal(tee.getParTotal());

                response.setEligibleForMen(true);
                response.setEligibleForWomen(false);

                String display = buildTeeDisplayName(
                        tee.getTeeName(),
                        tee.getCourseRating(),
                        tee.getSlope(),
                        tee.getParTotal()
                );

                response.setDisplayName(display);
                response.setDisplayNameForMen(display);
                response.setDisplayNameForWomen(display);
            } else {
                BigDecimal menRating = tee.getCourseRating();
                Integer menSlope = tee.getSlope();
                Integer menPar = tee.getParTotal();

                BigDecimal womenRating = sourceTee.getRatingForGender("F");
                Integer womenSlope = sourceTee.getSlopeForGender("F");
                Integer womenPar = sourceTee.getParForGender("F");

                response.setMenCourseRating(menRating);
                response.setMenSlope(menSlope);
                response.setMenParTotal(menPar);

                response.setWomenCourseRating(womenRating);
                response.setWomenSlope(womenSlope);
                response.setWomenParTotal(womenPar);

                // One round_tee row is stored per source course tee because the database
                // enforces uq_round_tee_round_source_course_tee. Eligibility is therefore
                // based on the source CourseTee's gender-specific rating/slope/par, not on
                // whether the round_tee snapshot happens to match the men's or women's values.
                response.setEligibleForMen(sourceTee.isEligibleForGender("M"));
                response.setEligibleForWomen(sourceTee.isEligibleForGender("F"));

                response.setDisplayNameForMen(buildTeeDisplayName(
                        tee.getTeeName(),
                        menRating,
                        menSlope,
                        menPar
                ));

                response.setDisplayNameForWomen(buildTeeDisplayName(
                        tee.getTeeName(),
                        womenRating,
                        womenSlope,
                        womenPar
                ));

                response.setDisplayName(response.getDisplayNameForMen());
            }

            result.add(response);
        }

        result.sort((a, b) -> {
            BigDecimal aRating = a.getMenCourseRating();
            BigDecimal bRating = b.getMenCourseRating();

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


    private String buildTeeDisplayName(
            String teeName,
            BigDecimal courseRating,
            Integer slope,
            Integer par
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(teeName == null || teeName.trim().isEmpty() ? "Tee" : teeName.trim());

        if (courseRating != null || slope != null || par != null) {
            sb.append(" (");

            boolean hasPrior = false;

            if (courseRating != null) {
                sb.append("CR ").append(formatCourseRating(courseRating));
                hasPrior = true;
            }

            if (slope != null) {
                if (hasPrior) {
                    sb.append(" / ");
                }
                sb.append("Slope ").append(slope);
                hasPrior = true;
            }

            if (par != null) {
                if (hasPrior) {
                    sb.append(" / ");
                }
                sb.append("Par ").append(par);
            }

            sb.append(")");
        }

        return sb.toString();
    }

    private String formatCourseRating(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    private RoundTeamPlayerResponse mapAssignedPlayer(RoundTeamPlayer roundTeamPlayer, Long roundId, Round round, LocalDate seedingAsOfDate) {
        Player player = roundTeamPlayer.getPlayer();

        RoundTeamPlayerResponse response = new RoundTeamPlayerResponse();
        response.setPlayerId(player.getId());
        response.setPlayerName(buildPlayerName(player));
        response.setPlayerOrder(roundTeamPlayer.getPlayerOrder());

        Optional<Scorecard> scorecardOpt = scorecardRepository.findByRound_IdAndPlayer_Id(roundId, player.getId());
        scorecardOpt.ifPresent(scorecard -> {
            response.setScorecardId(scorecard.getId());
            applyScorecardTee(response, scorecard, round);
            applyParticipationStatus(response, scorecard);
        });

        applyGender(response, player);
        applyTripIndex(response, player, round, seedingAsOfDate);
        return response;
    }

    private RoundTeamPlayerResponse mapUnassignedPlayer(Scorecard scorecard, Round round, LocalDate seedingAsOfDate) {
        Player player = scorecard.getPlayer();

        RoundTeamPlayerResponse response = new RoundTeamPlayerResponse();
        response.setScorecardId(scorecard.getId());
        response.setPlayerId(player.getId());
        response.setPlayerName(buildPlayerName(player));
        response.setPlayerOrder(null);

        applyScorecardTee(response, scorecard, round);
        applyParticipationStatus(response, scorecard);
        applyGender(response, player);
        applyTripIndex(response, player, round, seedingAsOfDate);

        return response;
    }

    private RoundTeamPlayerResponse mapUnavailablePlayer(Scorecard scorecard, Round round, LocalDate seedingAsOfDate) {
        RoundTeamPlayerResponse response = mapUnassignedPlayer(scorecard, round, seedingAsOfDate);
        applyParticipationStatus(response, scorecard);
        return response;
    }

    private boolean isActiveScorecard(Scorecard scorecard) {
        return scorecard == null || scorecard.getParticipationStatus() == null
                || scorecard.getParticipationStatus() == ScorecardParticipationStatus.ACTIVE;
    }

    private void applyParticipationStatus(RoundTeamPlayerResponse response, Scorecard scorecard) {
        ScorecardParticipationStatus status = scorecard == null ? null : scorecard.getParticipationStatus();
        response.setParticipationStatus(status == null ? ScorecardParticipationStatus.ACTIVE.name() : status.name());
        response.setWithdrawalHoleNumber(scorecard == null ? null : scorecard.getWithdrawalHoleNumber());
    }

    private void applyTripIndex(RoundTeamPlayerResponse response, Player player, Round round, LocalDate seedingAsOfDate) {
        if (player == null || round == null || round.getTrip() == null) {
            return;
        }

        if (roundEventCapabilityService.isScrambleRound(round)) {
            String method = roundScrambleSeedingService.resolveScrambleSeedingMethod(round);
            if ("AVERAGE_GROSS_SCORE".equals(method) || "AVERAGE_NET_SCORE".equals(method)) {
                response.setTripIndex(roundScrambleSeedingService.calculateSelectedRoundAverage(player.getId(), round, method));
                return;
            }
        }

        if (seedingAsOfDate == null) {
            return;
        }

        String handicapGroupCode = round.getTrip().getTripCode();
        if (handicapGroupCode == null || handicapGroupCode.trim().isEmpty()) {
            return;
        }

        BigDecimal frozenIndex = resolveFrozenTripPlayerIndex(player, round);
        if (frozenIndex != null) {
            response.setTripIndex(frozenIndex);
            return;
        }

        try {
            response.setTripIndex(tripHandicapService.calculateTripIndexAsOf(player, handicapGroupCode, seedingAsOfDate, round.getTrip().getHandicapMethod()));
        } catch (RuntimeException ignored) {
            response.setTripIndex(null);
        }
    }

    private BigDecimal resolveFrozenTripPlayerIndex(Player player, Round round) {
        if (player == null || player.getId() == null || round == null || round.getTrip() == null || round.getTrip().getId() == null) {
            return null;
        }

        Optional<TripPlayer> tripPlayer = tripPlayerRepository.findByTrip_IdAndPlayer_Id(round.getTrip().getId(), player.getId());
        if (tripPlayer.isEmpty()) {
            return null;
        }

        return tripPlayer.get().getFrozenHandicapIndex();
    }



    private void applyScorecardTee(RoundTeamPlayerResponse response, Scorecard scorecard, Round round) {
        RoundTee resolvedTee = roundTeeResolver.resolve(scorecard);
        response.setRoundTeeId(resolvedTee.getId());
        response.setRoundTeeName(resolvedTee.getTeeName());

        Long defaultTeeId = round.getDefaultRoundTee() == null ? null : round.getDefaultRoundTee().getId();
        boolean teeOverride =
                defaultTeeId != null &&
                resolvedTee.getId() != null &&
                !defaultTeeId.equals(resolvedTee.getId());

        response.setTeeOverride(teeOverride);

    }

    private void applyGender(RoundTeamPlayerResponse response, Player player) {
        response.setGender(normalizeGender(player == null ? null : player.getGender()));
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return "M";
        }

        String normalized = gender.trim().toUpperCase();

        if ("FEMALE".equals(normalized) || "W".equals(normalized) || "WOMAN".equals(normalized)) {
            return "F";
        }

        return "F".equals(normalized) ? "F" : "M";
    }

    private String buildPlayerName(Player player) {
        String firstName = player.getFirstName() == null ? "" : player.getFirstName().trim();
        String lastName = player.getLastName() == null ? "" : player.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}
