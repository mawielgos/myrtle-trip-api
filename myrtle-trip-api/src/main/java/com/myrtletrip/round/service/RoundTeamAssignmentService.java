package com.myrtletrip.round.service;

import com.myrtletrip.course.entity.CourseTee;
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
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class RoundTeamAssignmentService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundTeeResolver roundTeeResolver;
    private final RoundTeeProvisioningService roundTeeProvisioningService;

    public RoundTeamAssignmentService(
            RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundTeeRepository roundTeeRepository,
            ScorecardRepository scorecardRepository,
            RoundTeeResolver roundTeeResolver,
            RoundTeeProvisioningService roundTeeProvisioningService
    ) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundTeeResolver = roundTeeResolver;
        this.roundTeeProvisioningService = roundTeeProvisioningService;
    }

    @Transactional
    public RoundTeamAssignmentPageResponse getAssignmentPage(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        roundTeeProvisioningService.ensureRoundTeeOptions(round);

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);

        RoundTeamAssignmentPageResponse response = new RoundTeamAssignmentPageResponse();
        response.setRoundId(round.getId());
        response.setDefaultRoundTeeId(round.getDefaultRoundTee() == null ? null : round.getDefaultRoundTee().getId());
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
                    .map(roundTeamPlayer -> mapAssignedPlayer(roundTeamPlayer, roundId, round))
                    .toList();

            teamResponse.setPlayers(players);
            teamResponses.add(teamResponse);
        }

        scorecards.stream()
                .filter(scorecard -> scorecard.getTeam() == null || scorecard.getTeam().getId() == null)
                .sorted(Comparator.comparing(scorecard -> buildPlayerName(scorecard.getPlayer()), String.CASE_INSENSITIVE_ORDER))
                .map(scorecard -> mapUnassignedPlayer(scorecard, round))
                .forEach(unassignedPlayers::add);

        response.setTeams(teamResponses);
        response.setUnassignedPlayers(unassignedPlayers);

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
                BigDecimal menRating = sourceTee.getRatingForGender("M");
                Integer menSlope = sourceTee.getSlopeForGender("M");
                Integer menPar = sourceTee.getParForGender("M");

                BigDecimal womenRating = sourceTee.getRatingForGender("F");
                Integer womenSlope = sourceTee.getSlopeForGender("F");
                Integer womenPar = sourceTee.getParForGender("F");

                response.setMenCourseRating(menRating);
                response.setMenSlope(menSlope);
                response.setMenParTotal(menPar);

                response.setWomenCourseRating(womenRating);
                response.setWomenSlope(womenSlope);
                response.setWomenParTotal(womenPar);

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
                sb.append("CR ").append(courseRating);
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

    private RoundTeamPlayerResponse mapAssignedPlayer(RoundTeamPlayer roundTeamPlayer, Long roundId, Round round) {
        Player player = roundTeamPlayer.getPlayer();

        RoundTeamPlayerResponse response = new RoundTeamPlayerResponse();
        response.setPlayerId(player.getId());
        response.setPlayerName(buildPlayerName(player));
        response.setPlayerOrder(roundTeamPlayer.getPlayerOrder());

        Optional<Scorecard> scorecardOpt = scorecardRepository.findByRound_IdAndPlayer_Id(roundId, player.getId());
        scorecardOpt.ifPresent(scorecard -> {
            response.setScorecardId(scorecard.getId());
            applyScorecardTee(response, scorecard, round);
        });

        applyGender(response, player);
        return response;
    }

    private RoundTeamPlayerResponse mapUnassignedPlayer(Scorecard scorecard, Round round) {
        Player player = scorecard.getPlayer();

        RoundTeamPlayerResponse response = new RoundTeamPlayerResponse();
        response.setScorecardId(scorecard.getId());
        response.setPlayerId(player.getId());
        response.setPlayerName(buildPlayerName(player));
        response.setPlayerOrder(null);

        applyScorecardTee(response, scorecard, round);
        applyGender(response, player);

        return response;
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

        response.setUseAlternateTee(false);
        response.setStandardTeeEligible(true);
        response.setAlternateTeeEligible(false);
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
