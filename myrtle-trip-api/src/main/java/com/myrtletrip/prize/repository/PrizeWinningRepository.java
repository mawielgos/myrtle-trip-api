package com.myrtletrip.prize.repository;

import com.myrtletrip.prize.entity.PrizeWinning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrizeWinningRepository extends JpaRepository<PrizeWinning, Long> {

    List<PrizeWinning> findByTrip_IdOrderByGameKeyAscSourceRankAscPlayer_DisplayNameAsc(Long tripId);

    void deleteByTrip_Id(Long tripId);
}
