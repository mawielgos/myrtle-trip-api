package com.myrtletrip.course.repository;

import com.myrtletrip.course.entity.CourseTeeComboHole;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseTeeComboHoleRepository extends JpaRepository<CourseTeeComboHole, Long> {

    List<CourseTeeComboHole> findByComboTee_IdOrderByHoleNumberAsc(Long comboTeeId);

    Optional<CourseTeeComboHole> findByComboTee_IdAndHoleNumber(Long comboTeeId, Integer holeNumber);

    @Transactional
    void deleteByComboTee_Id(Long comboTeeId);
}
