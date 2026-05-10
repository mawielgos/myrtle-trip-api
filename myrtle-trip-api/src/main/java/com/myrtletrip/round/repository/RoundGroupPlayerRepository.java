package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.RoundGroupPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundGroupPlayerRepository extends JpaRepository<RoundGroupPlayer, Long> {

    List<RoundGroupPlayer> findByRoundGroup_IdOrderBySeatOrderAsc(Long roundGroupId);

    void deleteByRoundGroup_Round_Id(Long roundId);

    void deleteByRoundGroup_Round_Trip_Id(Long tripId);
}
