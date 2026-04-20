package com.myrtletrip.course.repository;

import com.myrtletrip.course.entity.CourseHole;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseHoleRepository extends JpaRepository<CourseHole, Long> {

    List<CourseHole> findByCourseTee_IdOrderByHoleNumberAsc(Long courseTeeId);

    Optional<CourseHole> findByCourseTee_IdAndHoleNumber(Long courseTeeId, Integer holeNumber);

    @Transactional
    void deleteByCourseTee_Id(Long courseTeeId);
}
