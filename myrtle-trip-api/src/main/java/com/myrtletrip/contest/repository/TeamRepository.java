package com.myrtletrip.contest.repository;

import com.myrtletrip.contest.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByRound_IdAndTeamTypeOrderByTeamNumber(Long roundId, String teamType);
}