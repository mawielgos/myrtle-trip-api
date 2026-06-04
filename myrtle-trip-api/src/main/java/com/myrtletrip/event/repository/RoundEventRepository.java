package com.myrtletrip.event.repository;

import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoundEventRepository extends JpaRepository<RoundEvent, Long> {

    List<RoundEvent> findByRound_IdAndActiveTrueOrderByEventOrderAsc(Long roundId);

    List<RoundEvent> findByRound_IdOrderByEventOrderAsc(Long roundId);

    Optional<RoundEvent> findByRound_IdAndEventType(Long roundId, RoundEventType eventType);

    boolean existsByRound_Id(Long roundId);
}
