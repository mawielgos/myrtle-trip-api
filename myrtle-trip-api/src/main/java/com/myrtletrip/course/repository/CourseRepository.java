package com.myrtletrip.course.repository;

import com.myrtletrip.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByNameIgnoreCase(String name);

    Optional<Course> findByLegacyCourseNumber(Integer legacyCourseNumber);
}