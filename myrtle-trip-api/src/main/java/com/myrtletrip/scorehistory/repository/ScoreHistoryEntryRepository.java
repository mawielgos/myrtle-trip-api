package com.myrtletrip.scorehistory.repository;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScoreHistoryEntryRepository extends JpaRepository<ScoreHistoryEntry, Long> {

    boolean existsByPlayerAndScoreDateAndCourseAndGrossScore(
            Player player,
            LocalDate scoreDate,
            Course course,
            Integer grossScore
    );

    boolean existsByPlayerAndScoreDateAndCourseNameAndGrossScore(
            Player player,
            LocalDate scoreDate,
            String courseName,
            Integer grossScore
    );

    boolean existsByPlayerAndHandicapGroupCodeAndSourceType(
            Player player,
            String handicapGroupCode,
            String sourceType
    );

    void deleteByPlayerAndHandicapGroupCodeAndSourceType(
            Player player,
            String handicapGroupCode,
            String sourceType
    );

    List<ScoreHistoryEntry> findByPlayerAndHandicapGroupCodeAndSourceTypeIn(
            Player player,
            String handicapGroupCode,
            Collection<String> sourceTypes
    );

    List<ScoreHistoryEntry> findByPlayerAndSourceTypeIn(
            Player player,
            Collection<String> sourceTypes
    );

    List<ScoreHistoryEntry> findTop6ByPlayerAndDifferentialIsNotNullAndManualDifferentialRequiredFalseAndSourceTypeNotInOrderByScoreDateDescIdDesc(
            Player player,
            Collection<String> sourceTypes
    );

    boolean existsByRound_IdAndPlayer_Id(Long roundId, Long playerId);

    Optional<ScoreHistoryEntry> findByRound_IdAndPlayer_Id(Long roundId, Long playerId);

    long countByHandicapGroupCode(String handicapGroupCode);

    long countByPlayerAndHandicapGroupCodeAndSourceTypeAndDifferentialIsNotNullAndManualDifferentialRequiredFalse(
            Player player,
            String handicapGroupCode,
            String sourceType
    );

    long countByPlayerAndSourceTypeAndDifferentialIsNotNullAndManualDifferentialRequiredFalse(
            Player player,
            String sourceType
    );

    long countByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrue(
            String handicapGroupCode,
            String sourceType
    );

    List<ScoreHistoryEntry> findByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrueOrderByPlayer_DisplayNameAscPostingOrderAscIdAsc(
            String handicapGroupCode,
            String sourceType
    );

    List<ScoreHistoryEntry> findByHandicapGroupCodeAndSourceTypeOrderByPlayer_DisplayNameAscScoreDateDescIdDesc(
            String handicapGroupCode,
            String sourceType
    );

    List<ScoreHistoryEntry> findByHandicapGroupCodeAndSourceTypeOrderByPlayer_DisplayNameAscPostingOrderAscIdAsc(
            String handicapGroupCode,
            String sourceType
    );

    long countByPlayer_IdAndHandicapGroupCodeAndSourceType(
            Long playerId,
            String handicapGroupCode,
            String sourceType
    );

    long countByPlayer_IdAndHandicapGroupCodeAndSourceTypeAndIdNot(
            Long playerId,
            String handicapGroupCode,
            String sourceType,
            Long id
    );

    List<ScoreHistoryEntry> findByPlayer_IdAndHandicapGroupCodeAndSourceTypeOrderByPostingOrderAscIdAsc(
            Long playerId,
            String handicapGroupCode,
            String sourceType
    );

    List<ScoreHistoryEntry> findByPlayer_IdAndHandicapGroupCodeAndSourceTypeAndIdNotOrderByPostingOrderAscIdAsc(
            Long playerId,
            String handicapGroupCode,
            String sourceType,
            Long id
    );

    Optional<ScoreHistoryEntry> findByIdAndHandicapGroupCodeAndSourceType(
            Long id,
            String handicapGroupCode,
            String sourceType
    );

    @Query("""
            select e
            from ScoreHistoryEntry e
            where e.sourceType = :sourceType
              and e.player.id in :playerIds
              and e.round is not null
              and e.round.trip.id <> :tripId
              and e.differential is not null
              and e.manualDifferentialRequired = false
            order by e.player.displayName asc, e.scoreDate desc, e.id desc
            """)
    List<ScoreHistoryEntry> findImportablePriorTripRoundEntries(
            @Param("tripId") Long tripId,
            @Param("playerIds") Collection<Long> playerIds,
            @Param("sourceType") String sourceType
    );
}

