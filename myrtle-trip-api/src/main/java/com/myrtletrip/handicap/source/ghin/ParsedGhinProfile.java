package com.myrtletrip.handicap.source.ghin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ParsedGhinProfile {

    private BigDecimal handicapIndex;
    private List<ParsedGhinScore> scores = new ArrayList<>();

    public BigDecimal getHandicapIndex() {
        return handicapIndex;
    }

    public void setHandicapIndex(BigDecimal handicapIndex) {
        this.handicapIndex = handicapIndex;
    }

    public List<ParsedGhinScore> getScores() {
        return scores;
    }

    public void setScores(List<ParsedGhinScore> scores) {
        this.scores = scores;
    }
}