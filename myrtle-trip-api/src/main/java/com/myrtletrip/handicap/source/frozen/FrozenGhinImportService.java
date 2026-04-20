package com.myrtletrip.handicap.source.frozen;

import com.myrtletrip.handicap.source.ghin.GhinPageClient;
import com.myrtletrip.handicap.source.ghin.GhinPageParser;
import com.myrtletrip.handicap.source.ghin.ParsedGhinProfile;
import com.myrtletrip.handicap.source.ghin.ParsedGhinScore;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class FrozenGhinImportService {

    private static final String SOURCE_GHIN_FROZEN = "GHIN_FROZEN";
    private static final String HANDICAP_METHOD_GHIN = "GHIN";

    private final PlayerRepository playerRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    private final GhinPageClient ghinPageClient;
    private final GhinPageParser ghinPageParser;

    public FrozenGhinImportService(PlayerRepository playerRepository,
                                   ScoreHistoryEntryRepository scoreHistoryEntryRepository,
                                   GhinPageClient ghinPageClient,
                                   GhinPageParser ghinPageParser) {
        this.playerRepository = playerRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
        this.ghinPageClient = ghinPageClient;
        this.ghinPageParser = ghinPageParser;
    }

    @Transactional
    public void initializeFrozenGhinForGroup(String handicapGroupCode) throws Exception {
        List<Player> players = playerRepository.findByHandicapMethodIgnoreCase(HANDICAP_METHOD_GHIN);
        initializeFrozenGhinForPlayers(players, handicapGroupCode);
    }

    @Transactional
    public void initializeFrozenGhinForPlayers(List<Player> players, String handicapGroupCode) throws Exception {
        if (handicapGroupCode == null || handicapGroupCode.isBlank()) {
            throw new IllegalArgumentException("handicapGroupCode is required");
        }
        if (players == null || players.isEmpty()) {
            return;
        }

        Set<Long> seenPlayerIds = new LinkedHashSet<Long>();

        for (Player player : players) {
            if (player == null || player.getId() == null) {
                continue;
            }

            if (!seenPlayerIds.add(player.getId())) {
                continue;
            }

            if (!HANDICAP_METHOD_GHIN.equalsIgnoreCase(player.getHandicapMethod())) {
                continue;
            }

            initializeFrozenGhinForPlayer(player, handicapGroupCode);
        }
    }

    @Transactional
    public void initializeFrozenGhinForPlayer(Player player, String handicapGroupCode) throws Exception {
        if (player == null) {
            throw new IllegalArgumentException("Player is required");
        }
        if (handicapGroupCode == null || handicapGroupCode.isBlank()) {
            throw new IllegalArgumentException("handicapGroupCode is required");
        }
        if (player.getGhinNumber() == null || player.getGhinNumber().isBlank()) {
            return;
        }

        scoreHistoryEntryRepository.deleteByPlayerAndHandicapGroupCodeAndSourceType(
                player,
                handicapGroupCode,
                SOURCE_GHIN_FROZEN
        );

        String html = ghinPageClient.fetchPeerPage(player.getGhinNumber());
        ParsedGhinProfile profile = ghinPageParser.parse(html);

        for (ParsedGhinScore parsedScore : profile.getScores()) {
            ScoreHistoryEntry entry = new ScoreHistoryEntry();
            entry.setPlayer(player);
            entry.setRound(null);
            entry.setCourse(null);

            // GHIN table gives MM/YY only; ordering matters, not exact date.
            entry.setScoreDate(LocalDate.of(1900, 1, 1));

            entry.setCourseName("GHIN Frozen");
            entry.setCourseRating(parsedScore.getCourseRating());
            entry.setSlope(parsedScore.getSlope());
            entry.setGrossScore(parsedScore.getGrossScore());
            entry.setAdjustedGrossScore(null);
            entry.setDifferential(parsedScore.getDifferential());
            entry.setSourceType(SOURCE_GHIN_FROZEN);
            entry.setIncludedInMyrtleCalc(true);
            entry.setHandicapGroupCode(handicapGroupCode);
            entry.setPostingOrder(parsedScore.getDisplayOrder());
            entry.setScoreType(parsedScore.getScoreType());
            entry.setHolesPlayed(parsedScore.getHolesPlayed());
            entry.setManualDifferentialRequired(Boolean.TRUE.equals(parsedScore.getManualDifferentialRequired()));

            scoreHistoryEntryRepository.save(entry);
        }
    }
}
