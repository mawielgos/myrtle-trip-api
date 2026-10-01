package com.myrtletrip.course.service;

import com.myrtletrip.course.dto.CourseHoleResponse;
import com.myrtletrip.course.dto.SaveCourseHoleRequest;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.entity.CourseTeeComboHole;
import com.myrtletrip.course.model.TeeType;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CourseHoleService {

    private final CourseHoleRepository courseHoleRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final CourseTeeComboHoleRepository courseTeeComboHoleRepository;

    public CourseHoleService(CourseHoleRepository courseHoleRepository,
                             CourseTeeRepository courseTeeRepository,
                             CourseTeeComboHoleRepository courseTeeComboHoleRepository) {
        this.courseHoleRepository = courseHoleRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.courseTeeComboHoleRepository = courseTeeComboHoleRepository;
    }

    public List<CourseHoleResponse> getHolesForTeeResponse(Long courseTeeId) {
        List<CourseHoleResponse> responses = new ArrayList<>();
        for (CourseHole hole : getHolesForTee(courseTeeId)) {
            responses.add(toCourseHoleResponse(hole));
        }
        return responses;
    }

    @Transactional
    public List<CourseHoleResponse> saveHolesForTee(Long courseTeeId, List<SaveCourseHoleRequest> requests) {
        CourseTee tee = courseTeeRepository.findById(courseTeeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        if (isComboTee(tee)) {
            throw new IllegalArgumentException("Combo tee hole data is controlled by source tee mappings.");
        }

        validateHoleRequests(tee, requests);

        courseHoleRepository.deleteByCourseTee_Id(courseTeeId);
        courseHoleRepository.flush();

        List<CourseHole> holesToSave = new ArrayList<>();
        int yardageTotal = 0;
        boolean hasAnyYardage = false;

        for (SaveCourseHoleRequest request : requests) {
            CourseHole hole = new CourseHole();
            hole.setCourseTee(tee);
            hole.setHoleNumber(request.getHoleNumber());
            hole.setPar(request.getPar());
            hole.setHandicap(request.getHandicap());
            hole.setYardage(request.getYardage());
            hole.setWomenPar(request.getWomenPar());
            hole.setWomenHandicap(request.getWomenHandicap());
            holesToSave.add(hole);

            if (request.getYardage() != null) {
                hasAnyYardage = true;
                yardageTotal += request.getYardage();
            }
        }

        courseHoleRepository.saveAll(holesToSave);
        if (hasAnyYardage) {
            tee.setYardageTotal(yardageTotal);
            courseTeeRepository.save(tee);
        }
        courseHoleRepository.flush();

        return getHolesForTeeResponse(courseTeeId);
    }

    public List<CourseHole> getHolesForTee(Long courseTeeId) {
        CourseTee tee = courseTeeRepository.findById(courseTeeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        if (!isComboTee(tee)) {
            return courseHoleRepository.findByCourseTee_IdOrderByHoleNumberAsc(courseTeeId);
        }

        return resolveComboHoles(tee);
    }

    public Integer getHolePar(Long courseTeeId, int holeNumber) {
        CourseHole hole = getResolvedHole(courseTeeId, holeNumber);
        return hole.getPar() != null ? hole.getPar() : hole.getWomenPar();
    }

    public Integer getHoleHandicap(Long courseTeeId, int holeNumber) {
        CourseHole hole = getResolvedHole(courseTeeId, holeNumber);
        return hole.getHandicap() != null ? hole.getHandicap() : hole.getWomenHandicap();
    }

    private void validateHoleRequests(CourseTee tee, List<SaveCourseHoleRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one hole is required");
        }
        if (requests.size() != 18) {
            throw new IllegalArgumentException("Exactly 18 holes are required");
        }

        Set<Integer> holeNumbers = new HashSet<>();
        Set<Integer> menHandicaps = new HashSet<>();
        Set<Integer> womenHandicaps = new HashSet<>();
        int menParTotal = 0;
        int womenParTotal = 0;
        boolean hasAnyMenHoleData = false;
        boolean hasAnyWomenHoleData = false;
        boolean hasCompleteMenHoleData = true;
        boolean hasCompleteWomenHoleData = true;

        for (SaveCourseHoleRequest request : requests) {
            if (request.getHoleNumber() == null || request.getHoleNumber() < 1 || request.getHoleNumber() > 18) {
                throw new IllegalArgumentException("Hole number must be between 1 and 18");
            }
            if (!holeNumbers.add(request.getHoleNumber())) {
                throw new IllegalArgumentException("Duplicate hole number: " + request.getHoleNumber());
            }

            boolean menHoleHasAny = request.getPar() != null || request.getHandicap() != null;
            boolean womenHoleHasAny = request.getWomenPar() != null || request.getWomenHandicap() != null;
            hasAnyMenHoleData = hasAnyMenHoleData || menHoleHasAny;
            hasAnyWomenHoleData = hasAnyWomenHoleData || womenHoleHasAny;

            if (menHoleHasAny) {
                if (request.getPar() == null || request.getPar() < 3 || request.getPar() > 6) {
                    throw new IllegalArgumentException("Invalid men's par for hole " + request.getHoleNumber());
                }
                if (request.getHandicap() == null || request.getHandicap() < 1 || request.getHandicap() > 18) {
                    throw new IllegalArgumentException("Men's handicap must be between 1 and 18 for hole " + request.getHoleNumber());
                }
                if (!menHandicaps.add(request.getHandicap())) {
                    throw new IllegalArgumentException("Duplicate men's handicap: " + request.getHandicap());
                }
                menParTotal += request.getPar();
            } else {
                hasCompleteMenHoleData = false;
            }

            if (womenHoleHasAny) {
                if (request.getWomenPar() == null || request.getWomenPar() < 3 || request.getWomenPar() > 6) {
                    throw new IllegalArgumentException("Invalid women's par for hole " + request.getHoleNumber());
                }
                if (request.getWomenHandicap() == null || request.getWomenHandicap() < 1 || request.getWomenHandicap() > 18) {
                    throw new IllegalArgumentException("Women's handicap must be between 1 and 18 for hole " + request.getHoleNumber());
                }
                if (!womenHandicaps.add(request.getWomenHandicap())) {
                    throw new IllegalArgumentException("Duplicate women's handicap: " + request.getWomenHandicap());
                }
                womenParTotal += request.getWomenPar();
            } else {
                hasCompleteWomenHoleData = false;
            }

            if (request.getYardage() != null && request.getYardage() <= 0) {
                throw new IllegalArgumentException("Yardage must be positive for hole " + request.getHoleNumber());
            }
        }

        if (!hasAnyMenHoleData && !hasAnyWomenHoleData) {
            throw new IllegalArgumentException("At least one complete men's or women's scorecard is required");
        }
        if (hasAnyMenHoleData && !hasCompleteMenHoleData) {
            throw new IllegalArgumentException("Men's par and handicap must be complete for all 18 holes, or blank for all 18 holes.");
        }
        if (hasAnyWomenHoleData && !hasCompleteWomenHoleData) {
            throw new IllegalArgumentException("Women's par and handicap must be complete for all 18 holes, or blank for all 18 holes.");
        }
        if (hasCompleteMenHoleData && tee.getParTotal() != null && menParTotal != tee.getParTotal()) {
            throw new IllegalArgumentException(
                    "Men's hole par total " + menParTotal + " does not match tee par total " + tee.getParTotal());
        }
        if (hasCompleteWomenHoleData && tee.getWomenParTotal() != null && womenParTotal != tee.getWomenParTotal()) {
            throw new IllegalArgumentException(
                    "Women's hole par total " + womenParTotal + " does not match women's tee par total " + tee.getWomenParTotal());
        }
    }

    private List<CourseHole> resolveComboHoles(CourseTee comboTee) {
        List<CourseTeeComboHole> mappings =
                courseTeeComboHoleRepository.findByComboTee_IdOrderByHoleNumberAsc(comboTee.getId());

        if (mappings.isEmpty()) {
            return new ArrayList<>();
        }
        if (mappings.size() != 18) {
            throw new IllegalArgumentException("Combo tee must have mappings for exactly 18 holes");
        }

        List<CourseHole> resolvedHoles = new ArrayList<>();
        for (CourseTeeComboHole mapping : mappings) {
            CourseHole sourceHole = courseHoleRepository
                    .findByCourseTee_IdAndHoleNumber(mapping.getSourceTee().getId(), mapping.getHoleNumber())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Missing source hole " + mapping.getHoleNumber() + " for tee "
                                    + mapping.getSourceTee().getTeeName()));

            CourseHole resolvedHole = new CourseHole();
            resolvedHole.setCourseTee(comboTee);
            resolvedHole.setHoleNumber(mapping.getHoleNumber());
            resolvedHole.setPar(sourceHole.getPar());
            resolvedHole.setHandicap(sourceHole.getHandicap());
            resolvedHole.setYardage(sourceHole.getYardage());
            resolvedHole.setWomenPar(sourceHole.getWomenPar());
            resolvedHole.setWomenHandicap(sourceHole.getWomenHandicap());
            resolvedHoles.add(resolvedHole);
        }
        return resolvedHoles;
    }

    private CourseHole getResolvedHole(Long courseTeeId, int holeNumber) {
        if (holeNumber < 1 || holeNumber > 18) {
            throw new IllegalArgumentException("Hole number must be between 1 and 18");
        }

        CourseTee tee = courseTeeRepository.findById(courseTeeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        if (!isComboTee(tee)) {
            return courseHoleRepository.findByCourseTee_IdAndHoleNumber(courseTeeId, holeNumber)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Hole not found for tee " + courseTeeId + ", hole " + holeNumber));
        }

        CourseTeeComboHole mapping =
                courseTeeComboHoleRepository.findByComboTee_IdAndHoleNumber(courseTeeId, holeNumber)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Combo tee mapping not found for tee " + courseTeeId + ", hole " + holeNumber));

        return courseHoleRepository
                .findByCourseTee_IdAndHoleNumber(mapping.getSourceTee().getId(), holeNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source hole not found for tee " + mapping.getSourceTee().getId()
                                + ", hole " + holeNumber));
    }

    private CourseHoleResponse toCourseHoleResponse(CourseHole hole) {
        return new CourseHoleResponse(
                hole.getId(),
                hole.getHoleNumber(),
                hole.getPar(),
                hole.getHandicap(),
                hole.getYardage(),
                hole.getWomenPar(),
                hole.getWomenHandicap()
        );
    }

    private boolean isComboTee(CourseTee tee) {
        return tee != null && TeeType.COMBO.equals(tee.getTeeType());
    }
}
