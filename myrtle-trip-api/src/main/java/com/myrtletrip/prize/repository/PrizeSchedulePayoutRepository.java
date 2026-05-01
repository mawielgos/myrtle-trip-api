package com.myrtletrip.prize.repository;

import com.myrtletrip.prize.entity.PrizeSchedulePayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface PrizeSchedulePayoutRepository extends JpaRepository<PrizeSchedulePayout, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void deleteByPrizeSchedule_Id(Long prizeScheduleId);

    List<PrizeSchedulePayout> findByPrizeSchedule_IdOrderByFinishingPlaceAsc(Long prizeScheduleId);
}
