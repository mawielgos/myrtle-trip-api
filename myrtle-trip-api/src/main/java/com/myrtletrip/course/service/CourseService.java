package com.myrtletrip.course.service;

import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.repository.CourseHoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseHoleRepository courseHoleRepository;

    public CourseService(CourseHoleRepository courseHoleRepository) {
        this.courseHoleRepository = courseHoleRepository;
    }

    public List<CourseHole> getHolesForTee(Long courseTeeId) {
        return courseHoleRepository.findByCourseTee_IdOrderByHoleNumberAsc(courseTeeId);
    }

    public Integer getHolePar(Long courseTeeId, int holeNumber) {
        return courseHoleRepository.findByCourseTee_IdAndHoleNumber(courseTeeId, holeNumber)
                .map(CourseHole::getPar)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hole not found for tee " + courseTeeId + ", hole " + holeNumber));
    }

    public Integer getHoleHandicap(Long courseTeeId, int holeNumber) {
        return courseHoleRepository.findByCourseTee_IdAndHoleNumber(courseTeeId, holeNumber)
                .map(CourseHole::getHandicap)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hole not found for tee " + courseTeeId + ", hole " + holeNumber));
    }
}