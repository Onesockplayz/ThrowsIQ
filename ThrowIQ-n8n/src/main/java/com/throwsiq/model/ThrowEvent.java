package com.throwsiq.model;

/**
 * The throwing event an athlete competes in.
 */
public enum ThrowEvent {

    SHOT_PUT("Shot Put"),
    HAMMER("Hammer"),
    DISCUS("Discus");

    private final String displayName;

    ThrowEvent(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable label suitable for Discord embed titles. */
    public String displayName() {
        return displayName;
    }
}
