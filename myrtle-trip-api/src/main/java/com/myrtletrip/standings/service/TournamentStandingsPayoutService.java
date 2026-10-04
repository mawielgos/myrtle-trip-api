package com.myrtletrip.standings.service;

import com.myrtletrip.prize.entity.PrizeSchedule;
import com.myrtletrip.prize.entity.PrizeSchedulePayout;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.standings.dto.TournamentStandingRowResponse;
import com.myrtletrip.tournament.model.TournamentCompetitionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TournamentStandingsPayoutService {

    private static final String TOURNAMENT_LOW_NET_GAME_KEY = "TOURNAMENT_LOW_NET";
    private static final String TOURNAMENT_LOW_GROSS_GAME_KEY = "TOURNAMENT_LOW_GROSS";
    private static final String LEGACY_TOURNAMENT_GAME_KEY = "FOUR_DAY_INDIVIDUAL";

    private final PrizeScheduleRepository prizeScheduleRepository;

    public TournamentStandingsPayoutService(PrizeScheduleRepository prizeScheduleRepository) {
        this.prizeScheduleRepository = prizeScheduleRepository;
    }

    public void apply(Long tripId,
                      TournamentCompetitionType competitionType,
                      boolean leaderboardFinal,
                      List<TournamentStandingRowResponse> ranked) {
        clearPayouts(ranked);
        if (!leaderboardFinal || ranked.isEmpty()) {
            return;
        }

        PrizeSchedule schedule = prizeScheduleRepository
                .findByTrip_IdAndGameKey(tripId, tournamentGameKey(competitionType))
                .orElse(null);
        if (schedule == null && competitionType == TournamentCompetitionType.LOW_NET) {
            schedule = prizeScheduleRepository
                    .findByTrip_IdAndGameKey(tripId, LEGACY_TOURNAMENT_GAME_KEY)
                    .orElse(null);
        }
        if (schedule == null || schedule.getPayouts() == null || schedule.getPayouts().isEmpty()) {
            return;
        }

        Map<Integer, BigDecimal> payoutByPlace = new HashMap<>();
        for (PrizeSchedulePayout payout : schedule.getPayouts()) {
            if (payout.getFinishingPlace() == null || payout.getAmountPerPlayer() == null
                    || payout.getFinishingPlace() < 1) {
                continue;
            }
            payoutByPlace.put(payout.getFinishingPlace(), payout.getAmountPerPlayer());
        }

        int index = 0;
        while (index < ranked.size()) {
            TournamentStandingRowResponse current = ranked.get(index);
            if (current.getPosition() == null) {
                index++;
                continue;
            }

            int end = index;
            int position = current.getPosition();
            while (end + 1 < ranked.size()
                    && ranked.get(end + 1).getPosition() != null
                    && ranked.get(end + 1).getPosition().intValue() == position) {
                end++;
            }

            BigDecimal tiedPool = BigDecimal.ZERO;
            for (int place = position; place <= position + (end - index); place++) {
                BigDecimal placeAmount = payoutByPlace.get(place);
                if (placeAmount != null) {
                    tiedPool = tiedPool.add(placeAmount);
                }
            }

            if (tiedPool.signum() > 0) {
                BigDecimal split = tiedPool.divide(
                        BigDecimal.valueOf(end - index + 1L), 2, RoundingMode.HALF_UP);
                for (int i = index; i <= end; i++) {
                    ranked.get(i).setMoney(split);
                }
            }
            index = end + 1;
        }
    }

    private String tournamentGameKey(TournamentCompetitionType competitionType) {
        return competitionType == TournamentCompetitionType.LOW_GROSS
                ? TOURNAMENT_LOW_GROSS_GAME_KEY
                : TOURNAMENT_LOW_NET_GAME_KEY;
    }

    private void clearPayouts(List<TournamentStandingRowResponse> ranked) {
        for (TournamentStandingRowResponse row : ranked) {
            row.setMoney(null);
        }
    }
}
