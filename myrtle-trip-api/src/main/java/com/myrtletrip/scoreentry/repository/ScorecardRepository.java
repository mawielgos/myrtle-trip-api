package com.myrtletrip.scoreentry.repository;

import com.myrtletrip.scoreentry.entity.Scorecard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScorecardRepository extends JpaRepository<Scorecard, Long> {
    
    List<Scorecard> findByRound_Id(Long roundId);
    
    Optional<Scorecard> findByRound_IdAndPlayer_Id(Long roundId, Long playerId);
}