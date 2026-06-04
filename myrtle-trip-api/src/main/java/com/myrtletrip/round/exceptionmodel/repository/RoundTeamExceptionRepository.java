package com.myrtletrip.round.exceptionmodel.repository;

import com.myrtletrip.round.exceptionmodel.entity.RoundTeamException;
import com.myrtletrip.round.exceptionmodel.entity.RoundTeamExceptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoundTeamExceptionRepository extends JpaRepository<RoundTeamException, Long> {

    @Modifying
    @Query("delete from RoundTeamException e where e.round.id = :roundId")
    void deleteByRoundIdHard(@Param("roundId") Long roundId);

    List<RoundTeamException> findByRound_IdAndActiveTrueOrderByRoundTeam_TeamNumberAscIdAsc(Long roundId);

    List<RoundTeamException> findByRoundTeam_IdAndActiveTrueOrderByIdAsc(Long roundTeamId);

    Optional<RoundTeamException> findFirstByRound_IdAndRoundTeam_IdAndExceptionTypeAndActiveTrueOrderByIdAsc(
            Long roundId,
            Long roundTeamId,
            RoundTeamExceptionType exceptionType
    );

    Optional<RoundTeamException> findFirstByRound_IdAndGhostPlayer_IdAndActiveTrueOrderByIdAsc(Long roundId, Long ghostPlayerId);
}
