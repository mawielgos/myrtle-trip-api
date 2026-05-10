package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.RoundGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundGroupRepository extends JpaRepository<RoundGroup, Long> {

    @EntityGraph(attributePaths = {"players", "players.player"})
    List<RoundGroup> findByRound_IdOrderByGroupNumberAsc(Long roundId);

    void deleteByRound_Id(Long roundId);

    boolean existsByRound_Id(Long roundId);
    
    long countByRoundId(Long roundId);

    void deleteByRound_Trip_Id(Long tripId);
}
