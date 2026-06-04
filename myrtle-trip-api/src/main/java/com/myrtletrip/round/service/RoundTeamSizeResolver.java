package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import org.springframework.stereotype.Component;

@Component
public class RoundTeamSizeResolver {

    private final RoundEventCapabilityService roundEventCapabilityService;

    public RoundTeamSizeResolver(RoundEventCapabilityService roundEventCapabilityService) {
        this.roundEventCapabilityService = roundEventCapabilityService;
    }

    public int resolveTeamSize(Round round) {
        if (round == null) {
            return 1;
        }
        return roundEventCapabilityService.expectedTeamSize(round);
    }
}
