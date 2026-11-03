package com.throwsiq.formatter;

import com.throwsiq.model.CheckInData;
import com.throwsiq.model.ReflectionResult;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.util.List;

/**
 * Stateless utility that converts a {@link CheckInData} + {@link ReflectionResult} pair into a
 * fully-formed JDA {@link MessageEmbed}.  Contains zero domain logic — only layout and formatting.
 */
public final class EmbedFormatter {

    /** Discord field-value character limit. */
    private static final int FIELD_MAX = 1024;

    // Event emojis — simple Unicode, no external dependency
    private static final String EMOJI_SHOT_PUT = "⚪";
    private static final String EMOJI_HAMMER   = "🔨";
    private static final String EMOJI_DISCUS   = "🥏";

    private EmbedFormatter() {}

    /**
     * Builds a {@link MessageEmbed} from the supplied domain objects.
     *
     * @param data   the athlete's check-in inputs
     * @param result the AI-generated analysis
     * @return a fully constructed embed ready to send via JDA
     */
    public static MessageEmbed build(CheckInData data, ReflectionResult result) {
        EmbedBuilder eb = new EmbedBuilder();

        // ── Title ──────────────────────────────────────────────────────────────
        String emoji = eventEmoji(data);
        eb.setTitle(emoji + " ThrowsIQ — " + data.event().displayName() + " Check-In");

        // ── Color (by fatigue rating) ──────────────────────────────────────────
        eb.setColor(fatigueColor(data.fatigueRating()));

        // ── Session ───────────────────────────────────────────────────────────
        eb.addField("Session Type", data.sessionType().displayName(), false);

        // ── Fatigue / Soreness ────────────────────────────────────────────────
        eb.addField("Fatigue / Soreness", fatigueBar(data.fatigueRating()), false);

        // ── Detected Faults ───────────────────────────────────────────────────
        eb.addField("🔍 Detected Faults",
                truncate(bulletList(result.detectedFaults(), "No faults detected.")), false);

        // ── Technical Cues & Drills ───────────────────────────────────────────
        eb.addField("🎯 Technical Cues",
                truncate(bulletList(result.technicalCues(), "No cues provided.")), false);

        eb.addField("🏋️ Recommended Drills",
                truncate(bulletList(result.drills(), "No drills provided.")), false);

        // ── Coach Conversation Starters ───────────────────────────────────────
        eb.addField("💬 Coach Conversation Starters",
                truncate(numberedList(result.coachQuestions(), "No questions generated.")), false);

        // ── Safety Alert (conditional) ────────────────────────────────────────
        if (result.safetyFlagged()) {
            eb.addField(
                    "⚠️ SAFETY ALERT",
                    truncate(
                            "Your reported fatigue level or practice notes suggest you may be dealing "
                            + "with elevated soreness or a potential injury.\n\n"
                            + "**Please consult your athletic trainer or coach before your next session.**\n\n"
                            + "• Do not throw through sharp or worsening pain.\n"
                            + "• Prioritise recovery, mobility work, and rest.\n"
                            + "• ThrowsIQ is not a medical tool and cannot diagnose injuries."),
                    false);
        }

        // ── Footer ────────────────────────────────────────────────────────────
        eb.setFooter("ThrowsIQ • Powered by AI • Not a medical tool");

        return eb.build();
    }

    // -------------------------------------------------------------------------
    // Color thresholds
    // -------------------------------------------------------------------------

    /**
     * Fatigue → embed color mapping:
     * <pre>
     *   1–3   → Green
     *   4–6   → Yellow
     *   7–8   → Orange
     *   9–10  → Red
     * </pre>
     */
    private static Color fatigueColor(int rating) {
        if (rating <= 3) return new Color(0x2ECC71);  // green
        if (rating <= 6) return new Color(0xF1C40F);  // yellow
        if (rating <= 8) return new Color(0xE67E22);  // orange
        return new Color(0xE74C3C);                   // red
    }

    // -------------------------------------------------------------------------
    // Event emoji
    // -------------------------------------------------------------------------

    private static String eventEmoji(CheckInData data) {
        return switch (data.event()) {
            case SHOT_PUT -> EMOJI_SHOT_PUT;
            case HAMMER   -> EMOJI_HAMMER;
            case DISCUS   -> EMOJI_DISCUS;
        };
    }

    // -------------------------------------------------------------------------
    // Fatigue display
    // -------------------------------------------------------------------------

    /** Renders e.g. {@code "7 / 10  🟠🟠🟠🟠🟠🟠🟠⬛⬛⬛"} */
    private static String fatigueBar(int rating) {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(rating).append(" / 10**  ");
        String filledDot  = fatigueDot(rating);
        String emptyDot   = "⬛";
        for (int i = 1; i <= 10; i++) {
            sb.append(i <= rating ? filledDot : emptyDot);
        }
        return sb.toString();
    }

    private static String fatigueDot(int rating) {
        if (rating <= 3) return "🟢";
        if (rating <= 6) return "🟡";
        if (rating <= 8) return "🟠";
        return "🔴";
    }

    // -------------------------------------------------------------------------
    // List formatters
    // -------------------------------------------------------------------------

    private static String bulletList(List<String> items, String fallback) {
        if (items == null || items.isEmpty()) return fallback;
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("• ").append(item);
        }
        return sb.toString();
    }

    private static String numberedList(List<String> items, String fallback) {
        if (items == null || items.isEmpty()) return fallback;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(i + 1).append(". ").append(items.get(i));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Truncation
    // -------------------------------------------------------------------------

    /**
     * Ensures a field value never exceeds Discord's 1024-character limit.
     * Truncates at the last full line before the limit and appends {@code "…"}.
     */
    static String truncate(String value) {
        if (value == null) return "";
        if (value.length() <= FIELD_MAX) return value;
        // Back off to the last newline before the cut-off to avoid splitting mid-bullet
        int cutoff = value.lastIndexOf('\n', FIELD_MAX - 1);
        if (cutoff <= 0) cutoff = FIELD_MAX - 1;
        return value.substring(0, cutoff) + "…";
    }
}
