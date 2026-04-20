package com.myrtletrip.course.repository;

import com.myrtletrip.course.entity.CourseTee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseTeeRepository extends JpaRepository<CourseTee, Long> {

    Optional<CourseTee> findByCourse_IdAndTeeNameIgnoreCase(Long courseId, String teeName);

    List<CourseTee> findByCourse_IdAndActiveTrueOrderByTeeNameAsc(Long courseId);

    List<CourseTee> findByCourse_IdOrderByTeeNameAsc(Long courseId);

    long countByCourse_Id(Long courseId);

    boolean existsByCourse_IdAndTeeNameIgnoreCase(Long courseId, String teeName);

    boolean existsByCourse_IdAndTeeNameIgnoreCaseAndIdNot(Long courseId, String teeName, Long teeId);
}
