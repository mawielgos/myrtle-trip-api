package com.myrtletrip.scorehistory.repository;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

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

    long countByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrue(
            String handicapGroupCode,
            String sourceType
    );

    List<ScoreHistoryEntry> findByHandicapGroupCodeAndSourceTypeAndManualDifferentialRequiredTrueOrderByPlayer_DisplayNameAscPostingOrderAscIdAsc(
            String handicapGroupCode,
            String sourceType
    );

    Optional<ScoreHistoryEntry> findByIdAndHandicapGroupCodeAndSourceType(
            Long id,
            String handicapGroupCode,
            String sourceType
    );
}
