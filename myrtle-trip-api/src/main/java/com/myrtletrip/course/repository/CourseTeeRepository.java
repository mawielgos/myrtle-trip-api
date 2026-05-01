package com.myrtletrip.course.repository;

import com.myrtletrip.course.entity.CourseTee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CourseTeeRepository extends JpaRepository<CourseTee, Long> {

    Optional<CourseTee> findByCourse_IdAndTeeNameIgnoreCase(Long courseId, String teeName);

    List<CourseTee> findByCourse_IdAndActiveTrueOrderByTeeNameAscEffectiveDateDesc(Long courseId);

    List<CourseTee> findByCourse_IdOrderByTeeNameAscEffectiveDateDesc(Long courseId);

    long countByCourse_Id(Long courseId);

    boolean existsByCourse_IdAndTeeNameIgnoreCase(Long courseId, String teeName);

    boolean existsByCourse_IdAndTeeNameIgnoreCaseAndIdNot(Long courseId, String teeName, Long teeId);

    boolean existsByCourse_IdAndTeeNameIgnoreCaseAndEffectiveDate(Long courseId, String teeName, LocalDate effectiveDate);

    boolean existsByCourse_IdAndTeeNameIgnoreCaseAndEffectiveDateAndIdNot(Long courseId, String teeName, LocalDate effectiveDate, Long teeId);
}
