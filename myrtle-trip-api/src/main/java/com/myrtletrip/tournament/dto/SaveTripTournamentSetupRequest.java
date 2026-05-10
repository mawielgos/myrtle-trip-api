package com.myrtletrip.tournament.dto;

import java.util.ArrayList;
import java.util.List;

public class SaveTripTournamentSetupRequest {
    private Boolean enabled;
    private String name;
    private String standingsLabel;
    private List<Long> includedPlannedRoundIds = new ArrayList<Long>();

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStandingsLabel() { return standingsLabel; }
    public void setStandingsLabel(String standingsLabel) { this.standingsLabel = standingsLabel; }
    public List<Long> getIncludedPlannedRoundIds() { return includedPlannedRoundIds; }
    public void setIncludedPlannedRoundIds(List<Long> includedPlannedRoundIds) { this.includedPlannedRoundIds = includedPlannedRoundIds; }
}
