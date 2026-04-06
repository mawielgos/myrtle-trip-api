package com.myrtletrip.round.model;

public enum RoundFormat {
    MIDDLE_MAN,
    ONE_TWO_THREE,
    TWO_MAN_LOW_NET,
    THREE_LOW_NET,
    TEAM_SCRAMBLE,
    STROKE_PLAY;

    public boolean requiresTeams() {
        return switch (this) {
            case MIDDLE_MAN, ONE_TWO_THREE, TWO_MAN_LOW_NET, THREE_LOW_NET, TEAM_SCRAMBLE -> true;
            case STROKE_PLAY -> false;
        };
    }

    public int expectedTeamSize() {
        return switch (this) {
            case TWO_MAN_LOW_NET -> 2;
            case MIDDLE_MAN, ONE_TWO_THREE, THREE_LOW_NET, TEAM_SCRAMBLE -> 4;
            case STROKE_PLAY -> 1;
        };
    }
}