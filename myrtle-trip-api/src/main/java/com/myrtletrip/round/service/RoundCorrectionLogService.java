package com.myrtletrip.round.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundCorrectionLogResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundCorrectionLog;
import com.myrtletrip.round.entity.RoundCorrectionType;
import com.myrtletrip.round.repository.RoundCorrectionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoundCorrectionLogService {

    private final RoundCorrectionLogRepository roundCorrectionLogRepository;

    public RoundCorrectionLogService(RoundCorrectionLogRepository roundCorrectionLogRepository) {
        this.roundCorrectionLogRepository = roundCorrectionLogRepository;
    }

    @Transactional
    public void logCorrectionSafely(Round round,
                                    Player player,
                                    RoundCorrectionType correctionType,
                                    String previousValue,
                                    String newValue) {
        try {
            RoundCorrectionLog log = new RoundCorrectionLog();
            log.setRound(round);
            log.setPlayer(player);
            log.setCorrectionType(correctionType);
            log.setPreviousValue(previousValue);
            log.setNewValue(newValue);
            log.setCreatedBy("system");
            roundCorrectionLogRepository.save(log);
        } catch (Exception ex) {
            // Audit logging should never block a score/tee/handicap correction.
        }
    }

    @Transactional(readOnly = true)
    public List<RoundCorrectionLogResponse> getCorrectionsForRound(Long roundId) {
        List<RoundCorrectionLog> logs = roundCorrectionLogRepository.findByRound_IdOrderByCreatedAtDescIdDesc(roundId);
        List<RoundCorrectionLogResponse> responses = new ArrayList<RoundCorrectionLogResponse>();

        for (RoundCorrectionLog log : logs) {
            RoundCorrectionLogResponse response = new RoundCorrectionLogResponse();
            response.setId(log.getId());
            response.setRoundId(log.getRound() != null ? log.getRound().getId() : null);
            response.setCorrectionType(log.getCorrectionType() != null ? log.getCorrectionType().name() : null);
            response.setPreviousValue(log.getPreviousValue());
            response.setNewValue(log.getNewValue());
            response.setCreatedAt(log.getCreatedAt());
            response.setCreatedBy(log.getCreatedBy());

            if (log.getPlayer() != null) {
                response.setPlayerId(log.getPlayer().getId());
                response.setPlayerName(log.getPlayer().getDisplayName());
            }

            responses.add(response);
        }

        return responses;
    }
}
