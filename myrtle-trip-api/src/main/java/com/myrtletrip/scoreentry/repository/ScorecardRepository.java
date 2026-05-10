package com.myrtletrip.scoreentry.repository;

import com.myrtletrip.scoreentry.entity.Scorecard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScorecardRepository extends JpaRepository<Scorecard, Long> {

    List<Scorecard> findByRound_Id(Long roundId);

    List<Scorecard> findByRound_IdOrderByIdAsc(Long roundId);

    Optional<Scorecard> findByRound_IdAndPlayer_Id(Long roundId, Long playerId);
    
    long countByRoundId(Long roundId);
    
    long countByRoundIdAndTeamIsNull(Long roundId);
    long countByRound_Trip_IdAndGrossScoreIsNotNull(Long tripId);

    long countByRound_Trip_IdAndAdjustedGrossScoreIsNotNull(Long tripId);

    long countByRound_Trip_IdAndNetScoreIsNotNull(Long tripId);

    void deleteByRound_Trip_Id(Long tripId);
}