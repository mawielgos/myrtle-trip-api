package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.RoundTeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundTeamPlayerRepository extends JpaRepository<RoundTeamPlayer, Long> {

    List<RoundTeamPlayer> findByRoundTeam_IdOrderByPlayerOrderAsc(Long roundTeamId);

    List<RoundTeamPlayer> findByRoundTeam_Round_IdOrderByRoundTeam_TeamNumberAscPlayerOrderAsc(Long roundId);

    void deleteByRoundTeam_Round_Id(Long roundId);

    void deleteByRoundTeam_Round_Trip_Id(Long tripId);
}