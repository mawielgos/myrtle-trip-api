package com.myrtletrip.scoreentry.repository;

import com.myrtletrip.scoreentry.entity.HoleScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoleScoreRepository extends JpaRepository<HoleScore, Long> {

    List<HoleScore> findByScorecard_IdOrderByHoleNumberAsc(Long scorecardId);

    Optional<HoleScore> findByScorecard_IdAndHoleNumber(Long scorecardId, Integer holeNumber);

    List<HoleScore> findByScorecard_Id(Long scorecardId);

    List<HoleScore> findByScorecard_Round_Id(Long roundId);
}