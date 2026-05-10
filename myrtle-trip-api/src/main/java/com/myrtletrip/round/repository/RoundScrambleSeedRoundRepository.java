package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.RoundScrambleSeedRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundScrambleSeedRoundRepository extends JpaRepository<RoundScrambleSeedRound, Long> {

    List<RoundScrambleSeedRound> findByScrambleRound_Id(Long scrambleRoundId);

    void deleteByScrambleRound_Id(Long scrambleRoundId);
}
