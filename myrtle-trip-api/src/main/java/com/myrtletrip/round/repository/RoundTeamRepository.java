package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.RoundTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundTeamRepository extends JpaRepository<RoundTeam, Long> {

    List<RoundTeam> findByRound_IdOrderByTeamNumberAsc(Long roundId);

    void deleteByRound_Id(Long roundId);
}