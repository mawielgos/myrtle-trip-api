package com.myrtletrip.scoreentry.repository;

import com.myrtletrip.scoreentry.entity.TeamHoleScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamHoleScoreRepository extends JpaRepository<TeamHoleScore, Long> {

    Optional<TeamHoleScore> findByRoundTeam_IdAndHoleNumber(Long roundTeamId, Integer holeNumber);

    List<TeamHoleScore> findByRoundTeam_IdOrderByHoleNumberAsc(Long roundTeamId);

    List<TeamHoleScore> findByRoundTeam_Round_IdOrderByRoundTeam_TeamNumberAscHoleNumberAsc(Long roundId);

    void deleteByRoundTeam_Id(Long roundTeamId);
}
