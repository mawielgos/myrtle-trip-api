package com.myrtletrip.course.repository;

import com.myrtletrip.course.entity.CourseTee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseTeeRepository extends JpaRepository<CourseTee, Long> {

    Optional<CourseTee> findByCourse_IdAndTeeNameIgnoreCase(Long courseId, String teeName);
}