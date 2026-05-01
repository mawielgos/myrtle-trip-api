package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.RoundTee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoundTeeRepository extends JpaRepository<RoundTee, Long> {

    List<RoundTee> findByRound_IdOrderByTeeNameAsc(Long roundId);

    Optional<RoundTee> findByRound_IdAndSourceCourseTee_Id(Long roundId, Long sourceCourseTeeId);
}
