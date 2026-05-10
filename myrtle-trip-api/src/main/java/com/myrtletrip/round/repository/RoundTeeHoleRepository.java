package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.RoundTeeHole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoundTeeHoleRepository extends JpaRepository<RoundTeeHole, Long> {

    List<RoundTeeHole> findByRoundTee_IdOrderByHoleNumberAsc(Long roundTeeId);

    Optional<RoundTeeHole> findByRoundTee_IdAndHoleNumber(Long roundTeeId, Integer holeNumber);

    void deleteByRoundTee_Round_Trip_Id(Long tripId);
}
