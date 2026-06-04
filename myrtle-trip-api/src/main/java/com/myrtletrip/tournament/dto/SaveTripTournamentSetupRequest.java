package com.myrtletrip.tournament.dto;

import java.util.ArrayList;
import java.util.List;

public class SaveTripTournamentSetupRequest {
    private Boolean enabled;
    private String name;
    private String standingsLabel;
    private Boolean lowNetEnabled;
    private Boolean lowGrossEnabled;
    private String lowNetName;
    private String lowGrossName;
    private List<Long> includedPlannedRoundIds = new ArrayList<Long>();

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStandingsLabel() { return standingsLabel; }
    public void setStandingsLabel(String standingsLabel) { this.standingsLabel = standingsLabel; }
    public Boolean getLowNetEnabled() { return lowNetEnabled; }
    public void setLowNetEnabled(Boolean lowNetEnabled) { this.lowNetEnabled = lowNetEnabled; }
    public Boolean getLowGrossEnabled() { return lowGrossEnabled; }
    public void setLowGrossEnabled(Boolean lowGrossEnabled) { this.lowGrossEnabled = lowGrossEnabled; }
    public String getLowNetName() { return lowNetName; }
    public void setLowNetName(String lowNetName) { this.lowNetName = lowNetName; }
    public String getLowGrossName() { return lowGrossName; }
    public void setLowGrossName(String lowGrossName) { this.lowGrossName = lowGrossName; }
    public List<Long> getIncludedPlannedRoundIds() { return includedPlannedRoundIds; }
    public void setIncludedPlannedRoundIds(List<Long> includedPlannedRoundIds) { this.includedPlannedRoundIds = includedPlannedRoundIds; }
}
