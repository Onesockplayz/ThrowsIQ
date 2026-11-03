package com.throwsiq.model;

/**
 * Immutable data carrier representing a single athlete check-in.
 *
 * @param event         the throwing event
 * @param sessionType   the type of training session
 * @param notes         free-text practice notes from the athlete
 * @param fatigueRating self-reported fatigue/soreness on a 1–10 scale
 */
public record CheckInData(
        ThrowEvent event,
        SessionType sessionType,
        String notes,
        int fatigueRating
) {}
