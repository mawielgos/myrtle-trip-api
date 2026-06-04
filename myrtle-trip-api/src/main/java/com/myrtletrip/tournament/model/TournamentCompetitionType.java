package com.myrtletrip.tournament.model;

public enum TournamentCompetitionType {
    LOW_NET("Low Net", true),
    LOW_GROSS("Low Gross", false);

    private final String displayName;
    private final boolean net;

    TournamentCompetitionType(String displayName, boolean net) {
        this.displayName = displayName;
        this.net = net;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isNet() {
        return net;
    }

    public static TournamentCompetitionType parse(String value) {
        if (value == null || value.isBlank()) {
            return LOW_NET;
        }
        try {
            return TournamentCompetitionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return LOW_NET;
        }
    }
}
