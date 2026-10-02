package com.myrtletrip.round.service;

import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.round.dto.RoundScrambleSeedingRoundResponse;
import com.myrtletrip.round.dto.SaveRoundScrambleSeedingRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundScrambleSeedRound;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundScrambleSeedRoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlannedRoundEvent;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RoundScrambleSeedingService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final ScorecardRepository scorecardRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final RoundScrambleSeedRoundRepository roundScrambleSeedRoundRepository;
    private final TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    private final RoundCapabilityService roundCapabilityService;
    private final RoundEventCapabilityService roundEventCapabilityService;

    public RoundScrambleSeedingService(
            RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            ScorecardRepository scorecardRepository,
            TeamHoleScoreRepository teamHoleScoreRepository,
            TripPlannedRoundRepository tripPlannedRoundRepository,
            RoundScrambleSeedRoundRepository roundScrambleSeedRoundRepository,
            TripPlannedRoundEventRepository tripPlannedRoundEventRepository,
            RoundCapabilityService roundCapabilityService,
            RoundEventCapabilityService roundEventCapabilityService
    ) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.scorecardRepository = scorecardRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.roundScrambleSeedRoundRepository = roundScrambleSeedRoundRepository;
        this.tripPlannedRoundEventRepository = tripPlannedRoundEventRepository;
        this.roundCapabilityService = roundCapabilityService;
        this.roundEventCapabilityService = roundEventCapabilityService;
    }

    public void saveScrambleSeedingRounds(Long roundId, SaveRoundScrambleSeedingRequest request) {
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

    }


    public List<RoundScrambleSeedingRoundResponse> mapScrambleSeedingRounds(Round round) {
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


    public int resolveScrambleTeamSize(Round round) {
        if (!roundEventCapabilityService.isScrambleRound(round)) {
            return 4;
        }
        Integer size = round.getScrambleTeamSize();
        return size == null || size < 2 || size > 4 ? 4 : size;
    }


    public String resolveScrambleSeedingMethod(Round round) {
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



    public LocalDate determineSeedingAsOfDate(Round round) {
        return resolveScrambleHandicapDate(round);
    }


    public LocalDate resolveScrambleHandicapDate(Round round) {
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


    public String buildSeedingLabel(Round round, LocalDate seedingAsOfDate) {
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


    public BigDecimal calculateSelectedRoundAverage(Long playerId, Round scrambleRound, String method) {
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
}
