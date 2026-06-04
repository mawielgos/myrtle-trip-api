package com.myrtletrip.event.model;

import com.myrtletrip.round.model.RoundFormat;

public enum RoundEventType {
    INDIVIDUAL_LOW_NET,
    INDIVIDUAL_LOW_GROSS,
    TEAM_MIDDLE_MAN,
    TEAM_ONE_TWO_THREE,
    TEAM_TWO_MAN_LOW_NET,
    TEAM_TWO_LOW_NET,
    TEAM_THREE_LOW_NET,
    TEAM_SCRAMBLE;

    public boolean isTeamEvent() {
        return switch (this) {
            case TEAM_MIDDLE_MAN, TEAM_ONE_TWO_THREE, TEAM_TWO_MAN_LOW_NET, TEAM_TWO_LOW_NET, TEAM_THREE_LOW_NET, TEAM_SCRAMBLE -> true;
            case INDIVIDUAL_LOW_NET, INDIVIDUAL_LOW_GROSS -> false;
        };
    }

    public boolean isIndividualEvent() {
        return !isTeamEvent();
    }

    public boolean usesGross() {
        return this == INDIVIDUAL_LOW_GROSS || this == TEAM_SCRAMBLE;
    }

    public boolean usesNet() {
        return this != INDIVIDUAL_LOW_GROSS && this != TEAM_SCRAMBLE;
    }

    public boolean usesHandicap() {
        return usesNet();
    }

    public boolean isPayoutEligibleByDefault() {
        return true;
    }

    public boolean isTournamentEligibleByDefault() {
        return this == INDIVIDUAL_LOW_NET;
    }

    public String scoringModeLabel() {
        return switch (this) {
            case INDIVIDUAL_LOW_NET -> "Individual net";
            case INDIVIDUAL_LOW_GROSS -> "Individual gross";
            case TEAM_MIDDLE_MAN, TEAM_ONE_TWO_THREE, TEAM_TWO_MAN_LOW_NET, TEAM_TWO_LOW_NET, TEAM_THREE_LOW_NET -> "Team net";
            case TEAM_SCRAMBLE -> "Team gross";
        };
    }

    public Integer defaultTeamSize(Integer scrambleTeamSize) {
        return switch (this) {
            case TEAM_TWO_MAN_LOW_NET -> 2;
            case TEAM_MIDDLE_MAN, TEAM_ONE_TWO_THREE, TEAM_TWO_LOW_NET, TEAM_THREE_LOW_NET -> 4;
            case TEAM_SCRAMBLE -> scrambleTeamSize == null ? 4 : scrambleTeamSize;
            case INDIVIDUAL_LOW_NET, INDIVIDUAL_LOW_GROSS -> null;
        };
    }

    public String defaultName(Integer scrambleTeamSize) {
        return switch (this) {
            case INDIVIDUAL_LOW_NET -> "Individual Low Net";
            case INDIVIDUAL_LOW_GROSS -> "Individual Low Gross";
            case TEAM_MIDDLE_MAN -> "Middle Man";
            case TEAM_ONE_TWO_THREE -> "One-Two-Three";
            case TEAM_TWO_MAN_LOW_NET -> "2-Man Low Net";
            case TEAM_TWO_LOW_NET -> "4-Man 2-Low Net";
            case TEAM_THREE_LOW_NET -> "Three Low Net";
            case TEAM_SCRAMBLE -> (scrambleTeamSize == null ? 4 : scrambleTeamSize) + "-Man Scramble";
        };
    }

    public RoundFormat legacyRoundFormat() {
        return switch (this) {
            case TEAM_MIDDLE_MAN -> RoundFormat.MIDDLE_MAN;
            case TEAM_ONE_TWO_THREE -> RoundFormat.ONE_TWO_THREE;
            case TEAM_TWO_MAN_LOW_NET -> RoundFormat.TWO_MAN_LOW_NET;
            case TEAM_TWO_LOW_NET -> RoundFormat.THREE_LOW_NET;
            case TEAM_THREE_LOW_NET -> RoundFormat.THREE_LOW_NET;
            case TEAM_SCRAMBLE -> RoundFormat.TEAM_SCRAMBLE;
            case INDIVIDUAL_LOW_NET, INDIVIDUAL_LOW_GROSS -> RoundFormat.STROKE_PLAY;
        };
    }

    public static RoundEventType fromLegacyRoundFormat(RoundFormat format) {
        if (format == null) {
            return INDIVIDUAL_LOW_NET;
        }
        return switch (format) {
            case MIDDLE_MAN -> TEAM_MIDDLE_MAN;
            case ONE_TWO_THREE -> TEAM_ONE_TWO_THREE;
            case TWO_MAN_LOW_NET -> TEAM_TWO_MAN_LOW_NET;
            case THREE_LOW_NET -> TEAM_THREE_LOW_NET;
            case TEAM_SCRAMBLE -> TEAM_SCRAMBLE;
            case STROKE_PLAY -> INDIVIDUAL_LOW_NET;
        };
    }
}
