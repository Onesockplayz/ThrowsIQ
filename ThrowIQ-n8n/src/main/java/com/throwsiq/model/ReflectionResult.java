package com.throwsiq.model;

import java.util.List;

/**
 * Immutable data carrier for the AI-generated analysis of a check-in.
 *
 * @param detectedFaults  technical faults identified in the athlete's notes
 * @param technicalCues   corrective cues to address the detected faults
 * @param drills          recommended drills for the session
 * @param coachQuestions  conversation-starter questions the athlete can bring to their coach
 * @param safetyFlagged   {@code true} when fatigue >= 7 or pain/injury keywords are detected
 */
public record ReflectionResult(
        List<String> detectedFaults,
        List<String> technicalCues,
        List<String> drills,
        List<String> coachQuestions,
        boolean safetyFlagged
) {}
