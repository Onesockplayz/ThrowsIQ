package com.throwsiq.model;

/**
 * The type of training session the athlete completed.
 */
public enum SessionType {

    LIFTING("Lifting"),
    THROWING("Throwing"),
    TECHNICAL_WORK("Technical Work"),
    HYBRID("Hybrid"),
    RECOVERY("Recovery");

    private final String displayName;

    SessionType(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable label suitable for Discord embed fields. */
    public String displayName() {
        return displayName;
    }
}
