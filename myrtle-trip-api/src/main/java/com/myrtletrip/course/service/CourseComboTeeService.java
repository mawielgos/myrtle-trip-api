package com.myrtletrip.course.service;

import com.myrtletrip.course.dto.CourseTeeComboHoleResponse;
import com.myrtletrip.course.dto.SaveCourseTeeComboHoleRequest;
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
public class CourseComboTeeService {

    private final CourseHoleRepository courseHoleRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final CourseTeeComboHoleRepository courseTeeComboHoleRepository;
    private final CourseHoleService courseHoleService;

    public CourseComboTeeService(CourseHoleRepository courseHoleRepository,
                                 CourseTeeRepository courseTeeRepository,
                                 CourseTeeComboHoleRepository courseTeeComboHoleRepository,
                                 CourseHoleService courseHoleService) {
        this.courseHoleRepository = courseHoleRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.courseTeeComboHoleRepository = courseTeeComboHoleRepository;
        this.courseHoleService = courseHoleService;
    }

    public List<CourseTeeComboHoleResponse> getComboHolesForTee(Long comboTeeId) {
        CourseTee comboTee = courseTeeRepository.findById(comboTeeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        if (!isComboTee(comboTee)) {
            throw new IllegalArgumentException("Tee is not a combo tee");
        }

        List<CourseTeeComboHoleResponse> responses = new ArrayList<>();
        for (CourseTeeComboHole mapping :
                courseTeeComboHoleRepository.findByComboTee_IdOrderByHoleNumberAsc(comboTeeId)) {
            responses.add(toComboHoleResponse(mapping));
        }
        return responses;
    }

    @Transactional
    public List<CourseTeeComboHoleResponse> saveComboHolesForTee(
            Long comboTeeId,
            List<SaveCourseTeeComboHoleRequest> requests) {
        CourseTee comboTee = courseTeeRepository.findById(comboTeeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        if (!isComboTee(comboTee)) {
            throw new IllegalArgumentException("Tee is not a combo tee");
        }

        validateComboHoleRequests(comboTee, requests);

        courseTeeComboHoleRepository.deleteByComboTee_Id(comboTeeId);
        courseTeeComboHoleRepository.flush();

        List<CourseTeeComboHole> mappingsToSave = new ArrayList<>();
        for (SaveCourseTeeComboHoleRequest request : requests) {
            CourseTee sourceTee = courseTeeRepository.findById(request.getSourceTeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Source tee not found"));

            CourseTeeComboHole mapping = new CourseTeeComboHole();
            mapping.setComboTee(comboTee);
            mapping.setHoleNumber(request.getHoleNumber());
            mapping.setSourceTee(sourceTee);
            mappingsToSave.add(mapping);
        }

        courseTeeComboHoleRepository.saveAll(mappingsToSave);
        courseTeeComboHoleRepository.flush();
        updateComboTeeTotals(comboTee);

        return getComboHolesForTee(comboTeeId);
    }

    private void validateComboHoleRequests(CourseTee comboTee, List<SaveCourseTeeComboHoleRequest> requests) {
        if (requests == null || requests.size() != 18) {
            throw new IllegalArgumentException("Exactly 18 combo tee hole mappings are required");
        }

        Set<Integer> holeNumbers = new HashSet<>();
        for (SaveCourseTeeComboHoleRequest request : requests) {
            if (request.getHoleNumber() == null || request.getHoleNumber() < 1 || request.getHoleNumber() > 18) {
                throw new IllegalArgumentException("Hole number must be between 1 and 18");
            }
            if (!holeNumbers.add(request.getHoleNumber())) {
                throw new IllegalArgumentException("Duplicate hole number: " + request.getHoleNumber());
            }
            if (request.getSourceTeeId() == null) {
                throw new IllegalArgumentException("Source tee is required for hole " + request.getHoleNumber());
            }

            CourseTee sourceTee = courseTeeRepository.findById(request.getSourceTeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Source tee not found"));

            if (sourceTee.getId().equals(comboTee.getId())) {
                throw new IllegalArgumentException("Combo tee cannot source holes from itself");
            }
            if (!sourceTee.getCourse().getId().equals(comboTee.getCourse().getId())) {
                throw new IllegalArgumentException("Source tees must belong to the same course as the combo tee");
            }
            if (isComboTee(sourceTee)) {
                throw new IllegalArgumentException("Combo tees cannot use another combo tee as a source tee");
            }

            courseHoleRepository.findByCourseTee_IdAndHoleNumber(sourceTee.getId(), request.getHoleNumber())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Source tee " + sourceTee.getTeeName() + " does not have hole " + request.getHoleNumber()));
        }
    }

    private void updateComboTeeTotals(CourseTee comboTee) {
        List<CourseHole> resolvedHoles = courseHoleService.getHolesForTee(comboTee.getId());
        int parTotal = 0;
        int womenParTotal = 0;
        int yardageTotal = 0;
        boolean hasAnyMenPar = false;
        boolean hasAnyWomenPar = false;
        boolean hasAnyYardage = false;

        for (CourseHole hole : resolvedHoles) {
            if (hole.getPar() != null) {
                hasAnyMenPar = true;
                parTotal += hole.getPar();
            }
            if (hole.getWomenPar() != null) {
                hasAnyWomenPar = true;
                womenParTotal += hole.getWomenPar();
            }
            if (hole.getYardage() != null) {
                hasAnyYardage = true;
                yardageTotal += hole.getYardage();
            }
        }

        comboTee.setParTotal(hasAnyMenPar ? parTotal : womenParTotal);
        comboTee.setWomenParTotal(hasAnyWomenPar ? womenParTotal : null);
        comboTee.setYardageTotal(hasAnyYardage ? yardageTotal : null);
        courseTeeRepository.save(comboTee);
    }

    private CourseTeeComboHoleResponse toComboHoleResponse(CourseTeeComboHole mapping) {
        Long sourceTeeId = null;
        String sourceTeeName = null;
        if (mapping.getSourceTee() != null) {
            sourceTeeId = mapping.getSourceTee().getId();
            sourceTeeName = mapping.getSourceTee().getTeeName();
        }
        return new CourseTeeComboHoleResponse(
                mapping.getId(),
                mapping.getHoleNumber(),
                sourceTeeId,
                sourceTeeName
        );
    }

    private boolean isComboTee(CourseTee tee) {
        return tee != null && TeeType.COMBO.equals(tee.getTeeType());
    }
}
