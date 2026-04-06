package com.myrtletrip.contest.repository;

import com.myrtletrip.contest.entity.ContestResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContestResultRepository extends JpaRepository<ContestResult, Long> {

    List<ContestResult> findByRound_IdAndContestTypeOrderByRankAsc(Long roundId, String contestType);

    void deleteByRound_IdAndContestType(Long roundId, String contestType);
}