package com.myrtletrip.round.model;

public enum RoundTeeRole {
    DEFAULT,
    PLAYER_OPTION,

    /** Legacy values retained so existing historical rows still deserialize. */
    STANDARD,
    ALTERNATE
}
