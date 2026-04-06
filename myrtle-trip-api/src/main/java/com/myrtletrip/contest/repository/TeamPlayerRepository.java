package com.myrtletrip.contest.repository;

import com.myrtletrip.contest.entity.TeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamPlayerRepository extends JpaRepository<TeamPlayer, Long> {

    List<TeamPlayer> findByTeam_Id(Long teamId);
}