package dev.r3faced.minecurse.wasteland.api;

/**
 * Describes why stored Wasteland player state changed.
 */
public enum WastelandChangeReason {
    GAMEPLAY,
    COMMAND,
    API,
    REWARD,
    RESET,
    CUSTOM
}
