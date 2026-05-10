package com.myrtletrip.handicap.source.frozen;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FrozenMyrtleBeachImportService {

    private static final String HANDICAP_METHOD_DB_SCORE_HISTORY = "DB_SCORE_HISTORY";
    private static final String HANDICAP_METHOD_LEGACY_MYRTLE_BEACH = "MYRTLE_BEACH";
    private static final String SOURCE_DB_HISTORY_FROZEN = "DB_HISTORY_FROZEN";

    private final PlayerRepository playerRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;

    public FrozenMyrtleBeachImportService(PlayerRepository playerRepository,
                                          ScoreHistoryEntryRepository scoreHistoryEntryRepository) {
        this.playerRepository = playerRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
    }

    @Transactional
    public void initializeFrozenMyrtleBeachForGroup(String handicapGroupCode) {
        List<Player> players = new java.util.ArrayList<>();
        players.addAll(playerRepository.findByHandicapMethodIgnoreCase(HANDICAP_METHOD_DB_SCORE_HISTORY));
        players.addAll(playerRepository.findByHandicapMethodIgnoreCase(HANDICAP_METHOD_LEGACY_MYRTLE_BEACH));

        for (Player player : players) {
            initializeFrozenMyrtleBeachForPlayer(player, handicapGroupCode);
        }
    }

    @Transactional
    public void initializeFrozenMyrtleBeachForPlayer(Player player, String handicapGroupCode) {
        if (player == null) {
            throw new IllegalArgumentException("Player is required");
        }
        if (handicapGroupCode == null || handicapGroupCode.isBlank()) {
            throw new IllegalArgumentException("handicapGroupCode is required");
        }

        // Reload behavior: clear existing frozen Myrtle rows for this player/group, then reload.
        scoreHistoryEntryRepository.deleteByPlayerAndHandicapGroupCodeAndSourceType(
                player,
                handicapGroupCode,
                SOURCE_DB_HISTORY_FROZEN
        );

        List<ScoreHistoryEntry> baselineEntries = scoreHistoryEntryRepository
                .findTop6ByPlayerAndDifferentialIsNotNullAndManualDifferentialRequiredFalseAndSourceTypeNotInOrderByScoreDateDescIdDesc(
                        player,
                        List.of("GHIN_FROZEN", "DB_HISTORY_FROZEN")
                );

        int postingOrder = 1;
        for (ScoreHistoryEntry source : baselineEntries) {
            ScoreHistoryEntry frozen = new ScoreHistoryEntry();
            frozen.setPlayer(player);
            frozen.setRound(null);
            frozen.setCourse(source.getCourse());
            frozen.setScoreDate(source.getScoreDate());
            frozen.setCourseName(source.getCourseName());
            frozen.setCourseRating(source.getCourseRating());
            frozen.setSlope(source.getSlope());
            frozen.setGrossScore(source.getGrossScore());
            frozen.setAdjustedGrossScore(source.getAdjustedGrossScore());
            frozen.setDifferential(source.getDifferential());
            frozen.setSourceType(SOURCE_DB_HISTORY_FROZEN);
            frozen.setIncludedInMyrtleCalc(true);
            frozen.setHandicapGroupCode(handicapGroupCode);
            frozen.setPostingOrder(postingOrder++);
            frozen.setScoreType(source.getScoreType());
            frozen.setHolesPlayed(source.getHolesPlayed());
            frozen.setManualDifferentialRequired(false);

            scoreHistoryEntryRepository.save(frozen);
        }
    }
}