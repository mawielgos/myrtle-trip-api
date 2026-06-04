package com.myrtletrip.round.service;

import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.entity.CourseTeeComboHole;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.entity.RoundTeeHole;
import com.myrtletrip.round.model.RoundTeeRole;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundTeeProvisioningService {

    private final CourseTeeRepository courseTeeRepository;
    private final CourseHoleRepository courseHoleRepository;
    private final CourseTeeComboHoleRepository courseTeeComboHoleRepository;
    private final RoundRepository roundRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final RoundTeeHoleRepository roundTeeHoleRepository;
    private final TripPlayerRepository tripPlayerRepository;

    public RoundTeeProvisioningService(CourseTeeRepository courseTeeRepository,
                                       CourseHoleRepository courseHoleRepository,
                                       CourseTeeComboHoleRepository courseTeeComboHoleRepository,
                                       RoundRepository roundRepository,
                                       RoundTeeRepository roundTeeRepository,
                                       RoundTeeHoleRepository roundTeeHoleRepository,
                                       TripPlayerRepository tripPlayerRepository) {
        this.courseTeeRepository = courseTeeRepository;
        this.courseHoleRepository = courseHoleRepository;
        this.courseTeeComboHoleRepository = courseTeeComboHoleRepository;
        this.roundRepository = roundRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.tripPlayerRepository = tripPlayerRepository;
    }

    @Transactional
    public Map<Long, RoundTee> ensureRoundTeeOptions(Round round) {
        if (round == null || round.getId() == null) {
            throw new IllegalArgumentException("Round is required before creating round tee options.");
        }
        if (round.getCourse() == null || round.getCourse().getId() == null) {
            throw new IllegalStateException("Round " + round.getId() + " does not have a course.");
        }

        List<CourseTee> courseTees = courseTeeRepository
                .findByCourse_IdAndActiveTrueOrderByTeeNameAscEffectiveDateDesc(round.getCourse().getId());
        if (courseTees == null || courseTees.isEmpty()) {
            throw new IllegalStateException("Course has no active tees.");
        }

        Map<Long, RoundTee> byCourseTeeId = loadExistingByCourseTee(round.getId());
        List<String> activePlayerGenders = resolveActivePlayerGenders(round);

        Long defaultCourseTeeId = null;
        if (round.getDefaultRoundTee() != null && round.getDefaultRoundTee().getSourceCourseTee() != null) {
            defaultCourseTeeId = round.getDefaultRoundTee().getSourceCourseTee().getId();
        }

        for (CourseTee courseTee : courseTees) {
            if (courseTee == null || courseTee.getId() == null) {
                continue;
            }
            String teeGenderContext = resolveTeeGenderContext(courseTee, activePlayerGenders);
            if (teeGenderContext == null) {
                continue;
            }
            if (byCourseTeeId.containsKey(courseTee.getId())) {
                continue;
            }

            List<CourseHole> sourceHoles = resolveSourceHoles(courseTee);
            if (sourceHoles.size() != 18 || !hasCompleteHoleDataForGender(sourceHoles, teeGenderContext)) {
                // Do not let one partially-configured tee block round setup.
                // It simply will not be offered as a player tee option until its hole detail is complete
                // for at least one gender that is actually present on this trip.
                continue;
            }

            RoundTeeRole role = courseTee.getId().equals(defaultCourseTeeId)
                    ? RoundTeeRole.DEFAULT
                    : RoundTeeRole.PLAYER_OPTION;

            RoundTee created = createRoundTee(round, courseTee, role, sourceHoles, teeGenderContext);
            byCourseTeeId.put(courseTee.getId(), created);
        }

        if (round.getDefaultRoundTee() == null) {
            RoundTee fallbackDefault = chooseFallbackDefault(byCourseTeeId);
            if (fallbackDefault != null) {
                round.setDefaultRoundTee(fallbackDefault);
                roundRepository.save(round);
            }
        }

        return byCourseTeeId;
    }

    @Transactional
    public void refreshRoundTeeSnapshotsFromCourseMaster(Round round) {
        if (round == null || round.getId() == null) {
            throw new IllegalArgumentException("Round is required before refreshing round tee snapshots.");
        }

        List<RoundTee> roundTees = roundTeeRepository.findByRound_IdOrderByTeeNameAsc(round.getId());
        for (RoundTee roundTee : roundTees) {
            if (roundTee == null || roundTee.getSourceCourseTee() == null) {
                continue;
            }

            CourseTee sourceTee = roundTee.getSourceCourseTee();
            Long sourceCourseTeeId = sourceTee.getId();
            CourseTee currentSourceTee = courseTeeRepository.findById(sourceCourseTeeId).orElse(sourceTee);

            roundTee.setSourceCourseTee(currentSourceTee);
            roundTee.setTeeName(currentSourceTee.getTeeName());
            if (currentSourceTee.getCourse() != null && currentSourceTee.getCourse().getName() != null) {
                roundTee.setCourseName(currentSourceTee.getCourse().getName());
            } else if (round.getCourse() != null && round.getCourse().getName() != null) {
                roundTee.setCourseName(round.getCourse().getName());
            }
            String teeGenderContext = currentSourceTee.isEligibleForGender("M") ? "M" : "F";
            roundTee.setCourseRating(currentSourceTee.getRatingForGender(teeGenderContext));
            roundTee.setSlope(currentSourceTee.getSlopeForGender(teeGenderContext));
            roundTee.setParTotal(currentSourceTee.getParForGender(teeGenderContext));
            roundTeeRepository.save(roundTee);
        }
    }

    private Map<Long, RoundTee> loadExistingByCourseTee(Long roundId) {
        List<RoundTee> existingRoundTees = roundTeeRepository.findByRound_IdOrderByTeeNameAsc(roundId);
        Map<Long, RoundTee> byCourseTeeId = new HashMap<>();

        for (RoundTee roundTee : existingRoundTees) {
            if (roundTee.getSourceCourseTee() != null && roundTee.getSourceCourseTee().getId() != null) {
                byCourseTeeId.put(roundTee.getSourceCourseTee().getId(), roundTee);
            }
        }

        return byCourseTeeId;
    }

    private RoundTee chooseFallbackDefault(Map<Long, RoundTee> byCourseTeeId) {
        for (RoundTee tee : byCourseTeeId.values()) {
            if (tee.getTeeRole() == RoundTeeRole.DEFAULT) {
                return tee;
            }
        }
        for (RoundTee tee : byCourseTeeId.values()) {
            return tee;
        }
        return null;
    }

    private List<String> resolveActivePlayerGenders(Round round) {
        List<String> genders = new ArrayList<>();
        if (round == null || round.getTrip() == null || round.getTrip().getId() == null) {
            genders.add("M");
            return genders;
        }

        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(round.getTrip().getId());
        for (TripPlayer tripPlayer : tripPlayers) {
            if (tripPlayer == null || tripPlayer.getPlayer() == null || !tripPlayer.getPlayer().isActive()) {
                continue;
            }

            String gender = normalizeGender(tripPlayer.getPlayer().getGender());
            if (!genders.contains(gender)) {
                genders.add(gender);
            }
        }

        if (genders.isEmpty()) {
            genders.add("M");
        }

        return genders;
    }

    private String resolveTeeGenderContext(CourseTee courseTee, List<String> activePlayerGenders) {
        if (courseTee == null || activePlayerGenders == null || activePlayerGenders.isEmpty()) {
            return null;
        }

        // Prefer the men's rating context when male players are present. The current round_tee
        // schema stores one frozen rating/slope/par set per source tee, not one per gender.
        if (activePlayerGenders.contains("M") && courseTee.isEligibleForGender("M")) {
            return "M";
        }

        if (activePlayerGenders.contains("F") && courseTee.isEligibleForGender("F")) {
            return "F";
        }

        return null;
    }

    private boolean hasCompleteHoleDataForGender(List<CourseHole> sourceHoles, String gender) {
        if (sourceHoles == null || sourceHoles.size() != 18) {
            return false;
        }

        for (CourseHole sourceHole : sourceHoles) {
            if (sourceHole == null || sourceHole.getHoleNumber() == null) {
                return false;
            }
            if (resolveHolePar(sourceHole, gender) == null || resolveHoleHandicap(sourceHole, gender) == null) {
                return false;
            }
        }

        return true;
    }

    private Integer resolveHolePar(CourseHole sourceHole, String gender) {
        if (sourceHole == null) {
            return null;
        }
        if ("F".equals(normalizeGender(gender)) && sourceHole.getWomenPar() != null) {
            return sourceHole.getWomenPar();
        }
        return sourceHole.getPar();
    }

    private Integer resolveHoleHandicap(CourseHole sourceHole, String gender) {
        if (sourceHole == null) {
            return null;
        }
        if ("F".equals(normalizeGender(gender)) && sourceHole.getWomenHandicap() != null) {
            return sourceHole.getWomenHandicap();
        }
        return sourceHole.getHandicap();
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return "M";
        }
        String normalized = gender.trim().toUpperCase();
        if ("F".equals(normalized)) {
            return "F";
        }
        return "M";
    }

    private RoundTee createRoundTee(Round round, CourseTee sourceCourseTee, RoundTeeRole role, List<CourseHole> sourceHoles, String teeGenderContext) {
        RoundTee roundTee = new RoundTee();
        roundTee.setRound(round);
        roundTee.setSourceCourseTee(sourceCourseTee);
        roundTee.setTeeRole(role);
        roundTee.setCourseName(round.getCourse().getName());
        roundTee.setTeeName(sourceCourseTee.getTeeName());
        roundTee.setCourseRating(sourceCourseTee.getRatingForGender(teeGenderContext));
        roundTee.setSlope(sourceCourseTee.getSlopeForGender(teeGenderContext));
        roundTee.setParTotal(sourceCourseTee.getParForGender(teeGenderContext));
        roundTee = roundTeeRepository.save(roundTee);

        for (CourseHole sourceHole : sourceHoles) {
            RoundTeeHole roundTeeHole = new RoundTeeHole();
            roundTeeHole.setRoundTee(roundTee);
            roundTeeHole.setHoleNumber(sourceHole.getHoleNumber());
            roundTeeHole.setPar(resolveHolePar(sourceHole, teeGenderContext));
            roundTeeHole.setHandicap(resolveHoleHandicap(sourceHole, teeGenderContext));
            roundTeeHole.setYardage(sourceHole.getYardage());
            roundTeeHoleRepository.save(roundTeeHole);
        }

        return roundTee;
    }

    private List<CourseHole> resolveSourceHoles(CourseTee courseTee) {
        List<CourseHole> directHoles = courseHoleRepository.findByCourseTee_IdOrderByHoleNumberAsc(courseTee.getId());
        if (directHoles != null && directHoles.size() == 18) {
            return directHoles;
        }

        List<CourseTeeComboHole> comboMappings = courseTeeComboHoleRepository.findByComboTee_IdOrderByHoleNumberAsc(courseTee.getId());
        if (comboMappings == null || comboMappings.size() != 18) {
            return new ArrayList<>();
        }

        List<CourseHole> resolvedHoles = new ArrayList<>();
        for (CourseTeeComboHole mapping : comboMappings) {
            if (mapping == null || mapping.getHoleNumber() == null || mapping.getSourceTee() == null || mapping.getSourceTee().getId() == null) {
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
