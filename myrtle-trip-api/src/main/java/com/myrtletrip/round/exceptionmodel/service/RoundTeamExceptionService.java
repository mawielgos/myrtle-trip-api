package com.myrtletrip.round.exceptionmodel.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.exceptionmodel.dto.RoundTeamExceptionPageResponse;
import com.myrtletrip.round.exceptionmodel.dto.RoundTeamExceptionRequest;
import com.myrtletrip.round.exceptionmodel.dto.RoundTeamExceptionResponse;
import com.myrtletrip.round.exceptionmodel.entity.RoundTeamException;
import com.myrtletrip.round.exceptionmodel.entity.RoundTeamExceptionSelectionMethod;
import com.myrtletrip.round.exceptionmodel.entity.RoundTeamExceptionType;
import com.myrtletrip.round.exceptionmodel.repository.RoundTeamExceptionRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RoundTeamExceptionService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final PlayerRepository playerRepository;
    private final RoundTeamExceptionRepository exceptionRepository;

    public RoundTeamExceptionService(
            RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            PlayerRepository playerRepository,
            RoundTeamExceptionRepository exceptionRepository
    ) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.playerRepository = playerRepository;
        this.exceptionRepository = exceptionRepository;
    }

    @Transactional(readOnly = true)
    public RoundTeamExceptionPageResponse getExceptions(Long roundId) {
        requireRound(roundId);
        RoundTeamExceptionPageResponse page = new RoundTeamExceptionPageResponse();
        page.setRoundId(roundId);
        page.setExceptions(exceptionRepository.findByRound_IdAndActiveTrueOrderByRoundTeam_TeamNumberAscIdAsc(roundId)
                .stream()
                .map(this::toResponse)
                .toList());
        return page;
    }

    @Transactional
    public RoundTeamExceptionResponse saveException(Long roundId, RoundTeamExceptionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exception request is required.");
        }

        Round round = requireRound(roundId);
        RoundTeam targetTeam = requireTeamForRound(roundId, request.getRoundTeamId());
        RoundTeamExceptionType exceptionType = request.getExceptionType();
        if (exceptionType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exception type is required.");
        }

        RoundTeamException exception = loadOrCreate(round, targetTeam, exceptionType, request.getId());
        exception.setRound(round);
        exception.setRoundTeam(targetTeam);
        exception.setExceptionType(exceptionType);
        exception.setActive(request.getActive() == null || Boolean.TRUE.equals(request.getActive()));
        exception.setNotes(clean(request.getNotes()));

        if (exceptionType == RoundTeamExceptionType.GHOST_PLAYER) {
            configureGhost(roundId, targetTeam, exception, request);
        } else if (exceptionType == RoundTeamExceptionType.EXTRA_SHOT_ROTATION) {
            configureExtraShotRotation(exception, request);
        }

        return toResponse(exceptionRepository.save(exception));
    }

    @Transactional
    public void deactivateException(Long roundId, Long exceptionId) {
        RoundTeamException exception = exceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team exception not found: " + exceptionId));
        if (exception.getRound() == null || exception.getRound().getId() == null || !exception.getRound().getId().equals(roundId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Team exception not found for round: " + exceptionId);
        }
        exception.setActive(false);
        exceptionRepository.save(exception);
    }

    @Transactional(readOnly = true)
    public Optional<RoundTeamException> findActiveGhostException(Long roundId, Long teamId) {
        return exceptionRepository.findFirstByRound_IdAndRoundTeam_IdAndExceptionTypeAndActiveTrueOrderByIdAsc(
                roundId,
                teamId,
                RoundTeamExceptionType.GHOST_PLAYER
        );
    }

    @Transactional(readOnly = true)
    public Optional<RoundTeamException> findActiveExtraShotRotation(Long roundId, Long teamId) {
        return exceptionRepository.findFirstByRound_IdAndRoundTeam_IdAndExceptionTypeAndActiveTrueOrderByIdAsc(
                roundId,
                teamId,
                RoundTeamExceptionType.EXTRA_SHOT_ROTATION
        );
    }

    private Round requireRound(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found: " + roundId));
    }

    private RoundTeam requireTeamForRound(Long roundId, Long teamId) {
        if (teamId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Round team id is required.");
        }
        RoundTeam team = roundTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Round team not found: " + teamId));
        if (team.getRound() == null || team.getRound().getId() == null || !team.getRound().getId().equals(roundId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Round team does not belong to round " + roundId + ".");
        }
        return team;
    }

    private RoundTeamException loadOrCreate(Round round, RoundTeam targetTeam, RoundTeamExceptionType exceptionType, Long id) {
        if (id != null) {
            RoundTeamException existing = exceptionRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team exception not found: " + id));
            if (existing.getRound() == null || existing.getRound().getId() == null || !existing.getRound().getId().equals(round.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team exception does not belong to round " + round.getId() + ".");
            }
            return existing;
        }

        return exceptionRepository
                .findFirstByRound_IdAndRoundTeam_IdAndExceptionTypeAndActiveTrueOrderByIdAsc(round.getId(), targetTeam.getId(), exceptionType)
                .orElseGet(RoundTeamException::new);
    }

    private void configureGhost(Long roundId, RoundTeam targetTeam, RoundTeamException exception, RoundTeamExceptionRequest request) {
        Long ghostPlayerId = request.getGhostPlayerId();
        if (ghostPlayerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ghost player id is required.");
        }

        Player ghostPlayer = playerRepository.findById(ghostPlayerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ghost player not found: " + ghostPlayerId));

        RoundTeam sourceTeam = findPlayerTeam(roundId, ghostPlayerId);
        if (sourceTeam == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ghost player must be assigned to another team in this round.");
        }
        if (sourceTeam.getId().equals(targetTeam.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ghost player cannot come from the short team.");
        }

        Optional<RoundTeamException> existingGhostUse = exceptionRepository.findFirstByRound_IdAndGhostPlayer_IdAndActiveTrueOrderByIdAsc(roundId, ghostPlayerId);
        if (existingGhostUse.isPresent()) {
            RoundTeamException existing = existingGhostUse.get();
            if (exception.getId() == null || !exception.getId().equals(existing.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This player is already assigned as a ghost player for another team in this round.");
            }
        }

        exception.setGhostPlayer(ghostPlayer);
        exception.setGhostSourceTeam(sourceTeam);
        exception.setIndexMin(request.getIndexMin());
        exception.setIndexMax(request.getIndexMax());
        exception.setSelectionMethod(request.getSelectionMethod() == null ? RoundTeamExceptionSelectionMethod.MANUAL : request.getSelectionMethod());
        exception.setRotationPattern(null);
    }

    private void configureExtraShotRotation(RoundTeamException exception, RoundTeamExceptionRequest request) {
        exception.setGhostPlayer(null);
        exception.setGhostSourceTeam(null);
        exception.setIndexMin(null);
        exception.setIndexMax(null);
        exception.setSelectionMethod(null);
        String rotationPattern = clean(request.getRotationPattern());
        if (rotationPattern == null) {
            rotationPattern = "1-4-7, 2-5-8, 3-6-9 repeating by player order";
        }
        exception.setRotationPattern(rotationPattern);
    }

    private RoundTeam findPlayerTeam(Long roundId, Long playerId) {
        List<RoundTeamPlayer> teamPlayers = roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(roundId);
        for (RoundTeamPlayer teamPlayer : teamPlayers) {
            if (teamPlayer == null || teamPlayer.getPlayer() == null || teamPlayer.getRoundTeam() == null) {
                continue;
            }
            if (playerId.equals(teamPlayer.getPlayer().getId())) {
                return teamPlayer.getRoundTeam();
            }
        }
        return null;
    }

    public boolean teamHasExactlyThesePlayers(Long roundId, Long teamId, int expectedSize) {
        List<RoundTeamPlayer> players = roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(teamId);
        return players != null && players.size() == expectedSize;
    }

    public int countTeamPlayers(Long teamId) {
        List<RoundTeamPlayer> players = roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(teamId);
        return players == null ? 0 : players.size();
    }

    public boolean hasDuplicateGhostAssignments(Long roundId) {
        List<RoundTeamException> exceptions = exceptionRepository.findByRound_IdAndActiveTrueOrderByRoundTeam_TeamNumberAscIdAsc(roundId);
        Set<Long> ghostPlayerIds = new HashSet<>();
        for (RoundTeamException exception : exceptions) {
            if (exception == null || exception.getExceptionType() != RoundTeamExceptionType.GHOST_PLAYER || exception.getGhostPlayer() == null) {
                continue;
            }
            Long ghostPlayerId = exception.getGhostPlayer().getId();
            if (ghostPlayerId != null && !ghostPlayerIds.add(ghostPlayerId)) {
                return true;
            }
        }
        return false;
    }

    private RoundTeamExceptionResponse toResponse(RoundTeamException exception) {
        RoundTeamExceptionResponse response = new RoundTeamExceptionResponse();
        response.setId(exception.getId());
        response.setRoundId(exception.getRound() == null ? null : exception.getRound().getId());
        response.setRoundTeamId(exception.getRoundTeam() == null ? null : exception.getRoundTeam().getId());
        response.setTeamNumber(exception.getRoundTeam() == null ? null : exception.getRoundTeam().getTeamNumber());
        response.setTeamName(exception.getRoundTeam() == null ? null : exception.getRoundTeam().getTeamName());
        response.setExceptionType(exception.getExceptionType());
        response.setGhostPlayerId(exception.getGhostPlayer() == null ? null : exception.getGhostPlayer().getId());
        response.setGhostPlayerName(exception.getGhostPlayer() == null ? null : exception.getGhostPlayer().getDisplayName());
        response.setGhostSourceTeamId(exception.getGhostSourceTeam() == null ? null : exception.getGhostSourceTeam().getId());
        response.setGhostSourceTeamNumber(exception.getGhostSourceTeam() == null ? null : exception.getGhostSourceTeam().getTeamNumber());
        response.setGhostSourceTeamName(exception.getGhostSourceTeam() == null ? null : exception.getGhostSourceTeam().getTeamName());
        response.setIndexMin(exception.getIndexMin());
        response.setIndexMax(exception.getIndexMax());
        response.setSelectionMethod(exception.getSelectionMethod());
        response.setRotationPattern(exception.getRotationPattern());
        response.setActive(exception.getActive());
        response.setNotes(exception.getNotes());
        return response;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
