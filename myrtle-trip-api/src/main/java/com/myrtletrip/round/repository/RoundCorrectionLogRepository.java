package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.RoundCorrectionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundCorrectionLogRepository extends JpaRepository<RoundCorrectionLog, Long> {

    List<RoundCorrectionLog> findByRound_IdOrderByCreatedAtDescIdDesc(Long roundId);

    long countByRound_Trip_Id(Long tripId);

    void deleteByRound_Trip_Id(Long tripId);
}
