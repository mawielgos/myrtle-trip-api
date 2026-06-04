package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.RoundTeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoundTeamPlayerRepository extends JpaRepository<RoundTeamPlayer, Long> {

    List<RoundTeamPlayer> findByRoundTeam_IdOrderByPlayerOrderAsc(Long roundTeamId);

    @Query("""
        select rtp
        from RoundTeamPlayer rtp
        join fetch rtp.roundTeam rt
        join fetch rtp.player p
        where rt.round.id = :roundId
        order by rt.teamNumber asc, rtp.playerOrder asc, p.displayName asc
        """)
    List<RoundTeamPlayer> findForRoundOrderedByTeamNumberAndPlayerOrder(@Param("roundId") Long roundId);

    void deleteByRoundTeam_Round_Id(Long roundId);

    void deleteByRoundTeam_Round_Trip_Id(Long tripId);
}
