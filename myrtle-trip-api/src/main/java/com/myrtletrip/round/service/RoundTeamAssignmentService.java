package com.myrtletrip.round.service;

import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundScrambleSeedingRoundResponse;
import com.myrtletrip.round.dto.ScorecardParticipationRequest;
import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.SaveRoundScrambleSeedingRequest;
import com.myrtletrip.round.dto.RoundTeamPlayerResponse;
import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.dto.RoundTeeOptionResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundScrambleSeedRound;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundScrambleSeedRoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.entity.TripPlannedRoundEvent;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.event.model.RoundEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RoundTeamAssignmentService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final ScorecardRepository scorecardRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final RoundTeeResolver roundTeeResolver;
    private final RoundTeeProvisioningService roundTeeProvisioningService;
    private final TripHandicapService tripHandicapService;
    private final TripPlayerRepository tripPlayerRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final RoundScrambleSeedRoundRepository roundScrambleSeedRoundRepository;
    private final TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    private final RoundCapabilityService roundCapabilityService;
    private final RoundEventCapabilityService roundEventCapabilityService;

    public RoundTeamAssignmentService(
            RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundTeeRepository roundTeeRepository,
            ScorecardRepository scorecardRepository,
            TeamHoleScoreRepository teamHoleScoreRepository,
            RoundTeeResolver roundTeeResolver,
            RoundTeeProvisioningService roundTeeProvisioningService,
            TripHandicapService tripHandicapService,
            TripPlayerRepository tripPlayerRepository,
            TripPlannedRoundRepository tripPlannedRoundRepository,
            RoundScrambleSeedRoundRepository roundScrambleSeedRoundRepository,
            TripPlannedRoundEventRepository tripPlannedRoundEventRepository,
            RoundCapabilityService roundCapabilityService,
            RoundEventCapabilityService roundEventCapabilityService
    ) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.scorecardRepository = scorecardRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.roundTeeResolver = roundTeeResolver;
        this.roundTeeProvisioningService = roundTeeProvisioningService;
        this.tripHandicapService = tripHandicapService;
        this.tripPlayerRepository = tripPlayerRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.roundScrambleSeedRoundRepository = roundScrambleSeedRoundRepository;
        this.tripPlannedRoundEventRepository = tripPlannedRoundEventRepository;
        this.roundCapabilityService = roundCapabilityService;
        this.roundEventCapabilityService = roundEventCapabilityService;
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
        response.setScrambleTeamSize(resolveScrambleTeamSize(round));
        response.setScrambleSeedingMethod(resolveScrambleSeedingMethod(round));
        response.setScrambleHandicapDate(resolveScrambleHandicapDate(round));
        response.setCapabilities(roundCapabilityService.build(round));

        LocalDate seedingAsOfDate = determineSeedingAsOfDate(round);
        response.setSeedingAsOfDate(seedingAsOfDate);
        response.setSeedingLabel(buildSeedingLabel(round, seedingAsOfDate));
        response.setScrambleSeedingRounds(mapScrambleSeedingRounds(round));
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

    @Transactional
    public RoundTeamAssignmentPageResponse saveScrambleSeedingRounds(Long roundId, SaveRoundScrambleSeedingRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        if (!roundEventCapabilityService.isScrambleRound(round)) {
            throw new IllegalStateException("Scramble seeding rounds can only be changed for a round with a Scramble event.");
        }
        roundCapabilityService.assertCanEditScrambleSetup(round);
        if (round.getTrip() == null || round.getTrip().getId() == null) {
            throw new IllegalStateException("Round is not linked to a trip.");
        }

        if (request != null && request.getScrambleTeamSize() != null) {
            int nextSize = request.getScrambleTeamSize();
            if (nextSize < 2 || nextSize > 4) {
                throw new IllegalArgumentException("Scramble team size must be 2, 3, or 4.");
            }
            if (hasScrambleScores(round)) {
                throw new IllegalStateException("Cannot change Scramble team size after Scramble scoring has started.");
            }
            round.setScrambleTeamSize(nextSize);
        }

        if (request != null && request.getSeedingMethod() != null) {
            round.setScrambleSeedingMethod(normalizeScrambleSeedingMethod(request.getSeedingMethod()));
        }
        if (request != null && request.getScrambleHandicapDate() != null) {
            round.setScrambleHandicapDate(request.getScrambleHandicapDate());
        }
        roundRepository.save(round);

        Set<Long> includedIds = new HashSet<>();
        if (request != null && request.getIncludedPlannedRoundIds() != null) {
            includedIds.addAll(request.getIncludedPlannedRoundIds());
        }

        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTrip_IdOrderByRoundNumberAsc(round.getTrip().getId());

        // Delete and flush before re-inserting. Without the flush, Hibernate can queue
        // the new rows before the old rows are physically removed, which can violate
        // uk_round_scramble_seed_round when the user saves an unchanged selection.
        roundScrambleSeedRoundRepository.deleteByScrambleRound_Id(round.getId());
        roundScrambleSeedRoundRepository.flush();

        List<RoundScrambleSeedRound> selectedSeedRounds = new ArrayList<>();
        for (TripPlannedRound plannedRound : plannedRounds) {
            boolean eligible = isEligibleScrambleSeedingRound(plannedRound, round);
            if (eligible && plannedRound.getId() != null && includedIds.contains(plannedRound.getId())) {
                RoundScrambleSeedRound selectedSeedRound = new RoundScrambleSeedRound();
                selectedSeedRound.setScrambleRound(round);
                selectedSeedRound.setPlannedRound(plannedRound);
                selectedSeedRounds.add(selectedSeedRound);
            }
        }
        if (!selectedSeedRounds.isEmpty()) {
            roundScrambleSeedRoundRepository.saveAll(selectedSeedRounds);
        }

        return getAssignmentPage(roundId);
    }


    @Transactional
    public RoundTeamAssignmentPageResponse updateScorecardParticipation(Long roundId, Long scorecardId, ScorecardParticipationRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
        roundCapabilityService.assertCanAssignTeams(round);

        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));
        if (scorecard.getRound() == null || scorecard.getRound().getId() == null
                || !scorecard.getRound().getId().equals(roundId)) {
            throw new IllegalArgumentException("Scorecard does not belong to this round.");
        }

        ScorecardParticipationStatus nextStatus = parseParticipationStatus(request == null ? null : request.getParticipationStatus());
        scorecard.setParticipationStatus(nextStatus);
        if (nextStatus == ScorecardParticipationStatus.WITHDRAWN) {
            scorecard.setWithdrawalHoleNumber(normalizeWithdrawalHoleNumber(request == null ? null : request.getWithdrawalHoleNumber()));
        } else {
            scorecard.setWithdrawalHoleNumber(null);
        }
        if (nextStatus != ScorecardParticipationStatus.ACTIVE) {
            scorecard.setTeam(null);
            removeTeamPlayerRows(roundId, scorecard.getPlayer() == null ? null : scorecard.getPlayer().getId());
        }
        scorecardRepository.save(scorecard);
        return getAssignmentPage(roundId);
    }

    private ScorecardParticipationStatus parseParticipationStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ScorecardParticipationStatus.ACTIVE;
        }
        try {
            return ScorecardParticipationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported participation status: " + value);
        }
    }

    private Integer normalizeWithdrawalHoleNumber(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > 18) {
            throw new IllegalArgumentException("Withdrawal hole must be between 0 and 18.");
        }
        return value;
    }

    private void removeTeamPlayerRows(Long roundId, Long playerId) {
        if (roundId == null || playerId == null) {
            return;
        }
        List<RoundTeamPlayer> existingRows = roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(roundId);
        for (RoundTeamPlayer row : existingRows) {
            if (row != null && row.getPlayer() != null && playerId.equals(row.getPlayer().getId())) {
                roundTeamPlayerRepository.delete(row);
            }
        }
    }

    private List<RoundScrambleSeedingRoundResponse> mapScrambleSeedingRounds(Round round) {
        List<RoundScrambleSeedingRoundResponse> result = new ArrayList<>();
        if (round == null || round.getTrip() == null || round.getTrip().getId() == null
                || !roundEventCapabilityService.isScrambleRound(round)) {
            return result;
        }

        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTrip_IdOrderByRoundNumberAsc(round.getTrip().getId());
        Map<Integer, String> courseNameByRoundNumber = new java.util.HashMap<>();
        List<Round> existingRounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(round.getTrip().getId());
        for (Round existingRound : existingRounds) {
            if (existingRound.getRoundNumber() == null || existingRound.getCourse() == null) {
                continue;
            }
            courseNameByRoundNumber.put(existingRound.getRoundNumber(), existingRound.getCourse().getName());
        }

        Set<Long> selectedPlannedRoundIds = loadSelectedScrambleSeedRoundIds(round);

        for (TripPlannedRound plannedRound : plannedRounds) {
            RoundScrambleSeedingRoundResponse response = new RoundScrambleSeedingRoundResponse();
            response.setPlannedRoundId(plannedRound.getId());
            response.setRoundNumber(plannedRound.getRoundNumber());
            response.setRoundDate(plannedRound.getRoundDate());
            response.setFormat(plannedRound.getFormat() == null ? null : plannedRound.getFormat().name());
            response.setCourseName(plannedRound.getRoundNumber() == null ? null : courseNameByRoundNumber.get(plannedRound.getRoundNumber()));
            response.setIncluded(plannedRound.getId() != null && selectedPlannedRoundIds.contains(plannedRound.getId()));
            response.setEligible(isEligibleScrambleSeedingRound(plannedRound, round));
            result.add(response);
        }
        return result;
    }

    private Set<Long> loadSelectedScrambleSeedRoundIds(Round scrambleRound) {
        Set<Long> selectedIds = new HashSet<>();
        if (scrambleRound == null || scrambleRound.getId() == null) {
            return selectedIds;
        }

        List<RoundScrambleSeedRound> selectedSeedRounds = roundScrambleSeedRoundRepository.findByScrambleRound_Id(scrambleRound.getId());
        for (RoundScrambleSeedRound selectedSeedRound : selectedSeedRounds) {
            if (selectedSeedRound.getPlannedRound() != null && selectedSeedRound.getPlannedRound().getId() != null) {
                selectedIds.add(selectedSeedRound.getPlannedRound().getId());
            }
        }

        return selectedIds;
    }

    private boolean isEligibleScrambleSeedingRound(TripPlannedRound plannedRound, Round scrambleRound) {
        if (plannedRound == null || scrambleRound == null) {
            return false;
        }
        if (plannedRound.getId() == null) {
            return false;
        }
        if (plannedRoundHasEventType(plannedRound, RoundEventType.TEAM_SCRAMBLE)) {
            return false;
        }

        LocalDate plannedDate = plannedRound.getRoundDate();
        LocalDate scrambleDate = scrambleRound.getRoundDate();

        if (plannedDate != null && scrambleDate != null) {
            if (plannedDate.isBefore(scrambleDate)) {
                return true;
            }
            if (plannedDate.isAfter(scrambleDate)) {
                return false;
            }
        }

        if (plannedRound.getRoundNumber() == null || scrambleRound.getRoundNumber() == null) {
            return false;
        }
        return plannedRound.getRoundNumber() < scrambleRound.getRoundNumber();
    }

    private boolean plannedRoundHasEventType(TripPlannedRound plannedRound, RoundEventType eventType) {
        if (plannedRound == null || eventType == null) {
            return false;
        }
        if (plannedRound.getId() != null) {
            List<TripPlannedRoundEvent> events = tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(plannedRound.getId());
            if (events != null && !events.isEmpty()) {
                for (TripPlannedRoundEvent event : events) {
                    if (event != null && event.getEventType() == eventType) {
                        return true;
                    }
                }
                return false;
            }
        }
        return RoundEventType.fromLegacyRoundFormat(plannedRound.getFormat()) == eventType;
    }

    private int resolveScrambleTeamSize(Round round) {
        if (!roundEventCapabilityService.isScrambleRound(round)) {
            return 4;
        }
        Integer size = round.getScrambleTeamSize();
        return size == null || size < 2 || size > 4 ? 4 : size;
    }

    private String resolveScrambleSeedingMethod(Round round) {
        if (!roundEventCapabilityService.isScrambleRound(round)) {
            return "CURRENT_HANDICAP_INDEX";
        }
        return normalizeScrambleSeedingMethod(round.getScrambleSeedingMethod());
    }

    private String normalizeScrambleSeedingMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            return "CURRENT_HANDICAP_INDEX";
        }
        String normalized = method.trim().toUpperCase();
        if ("AVERAGE_GROSS_SCORE".equals(normalized) || "AVERAGE_NET_SCORE".equals(normalized)) {
            return normalized;
        }
        return "CURRENT_HANDICAP_INDEX";
    }

    private boolean hasScrambleScores(Round round) {
        if (round == null || round.getId() == null) {
            return false;
        }
        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(round.getId());
        for (RoundTeam team : teams) {
            if (team.getScrambleTotalScore() != null) {
                return true;
            }
        }
        return teamHoleScoreRepository.countByRoundTeam_Round_Id(round.getId()) > 0;
    }


    private LocalDate determineSeedingAsOfDate(Round round) {
        return resolveScrambleHandicapDate(round);
    }

    private LocalDate resolveScrambleHandicapDate(Round round) {
        if (round == null) {
            return null;
        }
        if (roundEventCapabilityService.isScrambleRound(round)
                && round.getScrambleHandicapDate() != null) {
            return round.getScrambleHandicapDate();
        }
        LocalDate fallbackDate = round.getRoundDate();
        return fallbackDate == null ? LocalDate.now() : fallbackDate;
    }

    private String buildSeedingLabel(Round round, LocalDate seedingAsOfDate) {
        if (roundEventCapabilityService.isScrambleRound(round)) {
            String method = resolveScrambleSeedingMethod(round);
            if ("AVERAGE_GROSS_SCORE".equals(method)) {
                return "Scramble teams seeded by average gross score from selected rounds.";
            }
            if ("AVERAGE_NET_SCORE".equals(method)) {
                return "Scramble teams seeded by average net score from selected rounds.";
            }
            return seedingAsOfDate == null
                    ? "Scramble teams seeded by projected handicap index."
                    : "Scramble teams seeded by projected handicap index as of " + seedingAsOfDate + ".";
        }
        return seedingAsOfDate == null
                ? "Team assignment index snapshot."
                : "Team assignment index snapshot as of " + seedingAsOfDate + ".";
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
            String method = resolveScrambleSeedingMethod(round);
            if ("AVERAGE_GROSS_SCORE".equals(method) || "AVERAGE_NET_SCORE".equals(method)) {
                response.setTripIndex(calculateSelectedRoundAverage(player.getId(), round, method));
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

    private BigDecimal calculateSelectedRoundAverage(Long playerId, Round scrambleRound, String method) {
        if (playerId == null || scrambleRound == null || scrambleRound.getTrip() == null || scrambleRound.getTrip().getId() == null) {
            return null;
        }

        Set<Long> selectedSeedRoundIds = loadSelectedScrambleSeedRoundIds(scrambleRound);
        if (selectedSeedRoundIds.isEmpty()) {
            return null;
        }

        Set<Integer> includedRoundNumbers = new HashSet<>();
        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTrip_IdOrderByRoundNumberAsc(scrambleRound.getTrip().getId());
        for (TripPlannedRound plannedRound : plannedRounds) {
            if (plannedRound.getId() != null
                    && selectedSeedRoundIds.contains(plannedRound.getId())
                    && plannedRound.getRoundNumber() != null
                    && isEligibleScrambleSeedingRound(plannedRound, scrambleRound)) {
                includedRoundNumbers.add(plannedRound.getRoundNumber());
            }
        }

        if (includedRoundNumbers.isEmpty()) {
            return null;
        }

        List<Round> tripRounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(scrambleRound.getTrip().getId());
        int total = 0;
        int count = 0;

        for (Round sourceRound : tripRounds) {
            if (sourceRound.getRoundNumber() == null || !includedRoundNumbers.contains(sourceRound.getRoundNumber())) {
                continue;
            }
            Optional<Scorecard> scorecardOpt = scorecardRepository.findByRound_IdAndPlayer_Id(sourceRound.getId(), playerId);
            if (scorecardOpt.isEmpty()) {
                continue;
            }
            Scorecard scorecard = scorecardOpt.get();
            Integer value = "AVERAGE_NET_SCORE".equals(method) ? scorecard.getNetScore() : scorecard.getGrossScore();
            if (value == null) {
                continue;
            }
            total += value;
            count++;
        }

        if (count == 0) {
            return null;
        }

        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), 1, java.math.RoundingMode.HALF_UP);
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
