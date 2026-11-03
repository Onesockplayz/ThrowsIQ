package com.throwsiq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.throwsiq.model.CheckInData;
import com.throwsiq.model.ReflectionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Assembles the LLM prompt from a {@link CheckInData}, delegates to an {@link LLMProvider},
 * parses the JSON response into a {@link ReflectionResult}, and runs the safety check.
 *
 * <p>This class imports nothing from any LLM vendor SDK — all vendor coupling lives in the
 * {@link LLMProvider} implementations.
 */
public class ReflectionService {

    /**
     * Keywords that, if found in the practice notes (case-insensitive), set
     * {@link ReflectionResult#safetyFlagged()} to {@code true}.
     */
    private static final Set<String> PAIN_KEYWORDS = Set.of(
            "pain", "hurt", "sharp", "strain", "tweak", "injured", "sprain"
    );

    /** Fatigue rating at or above which the safety flag is always raised. */
    private static final int SAFETY_FATIGUE_THRESHOLD = 7;

    private final LLMProvider provider;
    private final ObjectMapper mapper;

    public ReflectionService(LLMProvider provider) {
        this.provider = provider;
        this.mapper   = new ObjectMapper();
    }

    /**
     * Runs the full reflection pipeline for one check-in.
     *
     * @param data the athlete's check-in data
     * @return the parsed and safety-checked reflection result
     * @throws Exception if the LLM call or JSON parsing fails
     */
    public ReflectionResult reflect(CheckInData data) throws Exception {
        boolean flagged = isSafetyFlagged(data);
        String prompt   = buildPrompt(data);
        String raw      = provider.generateReflection(prompt);
        return parseResponse(raw, flagged);
    }

    // -------------------------------------------------------------------------
    // Prompt assembly
    // -------------------------------------------------------------------------

    private String buildPrompt(CheckInData data) {
        return "Athlete check-in details:\n"
                + "  Event        : " + data.event().displayName() + "\n"
                + "  Session type : " + data.sessionType().displayName() + "\n"
                + "  Fatigue/Soreness (1-10): " + data.fatigueRating() + "\n"
                + "  Practice notes:\n"
                + "    " + data.notes() + "\n\n"
                + "Analyze the notes for technical faults specific to the "
                + data.event().displayName() + " event. "
                + "Return your analysis as a JSON object with keys: "
                + "detectedFaults, technicalCues, drills, coachQuestions (each an array of strings). "
                + "Do NOT suggest medical diagnoses. "
                + "Do NOT encourage the athlete to throw through sharp pain.";
    }

    // -------------------------------------------------------------------------
    // JSON parsing
    // -------------------------------------------------------------------------

    private ReflectionResult parseResponse(String json, boolean safetyFlagged) throws Exception {
        JsonNode root = mapper.readTree(json);

        List<String> detectedFaults  = toStringList(root.path("detectedFaults"));
        List<String> technicalCues   = toStringList(root.path("technicalCues"));
        List<String> drills          = toStringList(root.path("drills"));
        List<String> coachQuestions  = toStringList(root.path("coachQuestions"));

        return new ReflectionResult(detectedFaults, technicalCues, drills, coachQuestions, safetyFlagged);
    }

    private List<String> toStringList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String text = item.asText("").trim();
                if (!text.isEmpty()) {
                    list.add(text);
                }
            }
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // Safety check
    // -------------------------------------------------------------------------

    private boolean isSafetyFlagged(CheckInData data) {
        if (data.fatigueRating() >= SAFETY_FATIGUE_THRESHOLD) {
            return true;
        }
        String notesLower = data.notes().toLowerCase(Locale.ROOT);
        for (String keyword : PAIN_KEYWORDS) {
            if (notesLower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
