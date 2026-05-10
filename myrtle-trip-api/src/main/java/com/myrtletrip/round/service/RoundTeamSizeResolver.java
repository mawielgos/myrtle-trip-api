package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.model.RoundFormat;
import org.springframework.stereotype.Component;

@Component
public class RoundTeamSizeResolver {

    public int resolveTeamSize(Round round) {
        if (round == null || round.getFormat() == null) {
            return 1;
        }

        RoundFormat format = round.getFormat();

        if (RoundFormat.TEAM_SCRAMBLE.equals(format)) {
            return 4;
        }

        return format.expectedTeamSize();
    }
}