package com.myrtletrip.round.dto;

import java.util.ArrayList;
import java.util.List;

public class SaveRoundScrambleSeedingRequest {

    private List<Long> includedPlannedRoundIds = new ArrayList<>();
    private String seedingMethod;
    private Integer scrambleTeamSize;

    public List<Long> getIncludedPlannedRoundIds() { return includedPlannedRoundIds; }
    public void setIncludedPlannedRoundIds(List<Long> includedPlannedRoundIds) { this.includedPlannedRoundIds = includedPlannedRoundIds; }

    public String getSeedingMethod() { return seedingMethod; }
    public void setSeedingMethod(String seedingMethod) { this.seedingMethod = seedingMethod; }

    public Integer getScrambleTeamSize() { return scrambleTeamSize; }
    public void setScrambleTeamSize(Integer scrambleTeamSize) { this.scrambleTeamSize = scrambleTeamSize; }
}
