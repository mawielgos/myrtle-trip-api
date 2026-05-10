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

    public RoundTeeProvisioningService(CourseTeeRepository courseTeeRepository,
                                       CourseHoleRepository courseHoleRepository,
                                       CourseTeeComboHoleRepository courseTeeComboHoleRepository,
                                       RoundRepository roundRepository,
                                       RoundTeeRepository roundTeeRepository,
                                       RoundTeeHoleRepository roundTeeHoleRepository) {
        this.courseTeeRepository = courseTeeRepository;
        this.courseHoleRepository = courseHoleRepository;
        this.courseTeeComboHoleRepository = courseTeeComboHoleRepository;
        this.roundRepository = roundRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
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

        Long defaultCourseTeeId = null;
        if (round.getDefaultRoundTee() != null && round.getDefaultRoundTee().getSourceCourseTee() != null) {
            defaultCourseTeeId = round.getDefaultRoundTee().getSourceCourseTee().getId();
        }

        for (CourseTee courseTee : courseTees) {
            if (courseTee == null || courseTee.getId() == null) {
                continue;
            }
            if (!courseTee.isEligibleForGender("M") && !courseTee.isEligibleForGender("F")) {
                continue;
            }
            if (byCourseTeeId.containsKey(courseTee.getId())) {
                continue;
            }

            List<CourseHole> sourceHoles = resolveSourceHoles(courseTee);
            if (sourceHoles.size() != 18) {
                // Do not let one partially-configured tee block round setup.
                // It simply will not be offered as a player tee option until its hole detail is complete.
                continue;
            }

            RoundTeeRole role = courseTee.getId().equals(defaultCourseTeeId)
                    ? RoundTeeRole.DEFAULT
                    : RoundTeeRole.PLAYER_OPTION;

            RoundTee created = createRoundTee(round, courseTee, role, sourceHoles);
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

    private RoundTee createRoundTee(Round round, CourseTee sourceCourseTee, RoundTeeRole role, List<CourseHole> sourceHoles) {
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
