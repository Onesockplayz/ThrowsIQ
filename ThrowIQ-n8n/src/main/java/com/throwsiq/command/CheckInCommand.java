package com.throwsiq.command;

import com.throwsiq.formatter.EmbedFormatter;
import com.throwsiq.model.CheckInData;
import com.throwsiq.model.ReflectionResult;
import com.throwsiq.model.SessionType;
import com.throwsiq.model.ThrowEvent;
import com.throwsiq.service.ReflectionService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;

/**
 * Handles the {@code /checkin} slash command.
 *
 * <p>Owns the Discord interaction lifecycle (defer → validate → delegate → reply) but contains
 * no domain logic — all coaching analysis is performed by {@link ReflectionService}.
 */
public class CheckInCommand extends ListenerAdapter {

    private static final String COMMAND_NAME = "checkin";

    private final ReflectionService reflectionService;

    public CheckInCommand(ReflectionService reflectionService) {
        this.reflectionService = reflectionService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals(COMMAND_NAME)) return;

        // Defer immediately — must happen before any async work to satisfy Discord's 3-second limit
        event.deferReply().queue();

        try {
            // ── Extract options ───────────────────────────────────────────────
            String eventValue   = event.getOption("event").getAsString();
            String sessionValue = event.getOption("session_type").getAsString();
            String notes        = event.getOption("notes").getAsString();
            int    fatigue      = event.getOption("fatigue_rating").getAsInt();

            // ── Validate fatigue rating ───────────────────────────────────────
            if (fatigue < 1 || fatigue > 10) {
                event.getHook()
                        .sendMessage("❌ **Invalid fatigue rating.** Please enter a number between 1 and 10.")
                        .queue();
                return;
            }

            // ── Map string values to domain enums ─────────────────────────────
            ThrowEvent throwEvent   = ThrowEvent.valueOf(eventValue);
            SessionType sessionType = SessionType.valueOf(sessionValue);

            CheckInData data = new CheckInData(throwEvent, sessionType, notes, fatigue);

            // ── Delegate to domain service ────────────────────────────────────
            ReflectionResult result = reflectionService.reflect(data);

            // ── Format and send embed ─────────────────────────────────────────
            MessageEmbed embed = EmbedFormatter.build(data, result);
            event.getHook().sendMessageEmbeds(embed).queue();

        } catch (Exception ex) {
            // Send a clean error message — no stack trace exposed to Discord
            MessageEmbed errorEmbed = new EmbedBuilder()
                    .setTitle("⚠️ Something went wrong")
                    .setDescription(
                            "ThrowsIQ couldn't complete your check-in right now.\n\n"
                            + "This is usually a temporary issue. Please try again in a moment.\n"
                            + "If the problem persists, let your server admin know.")
                    .setColor(new Color(0xE74C3C))
                    .setFooter("ThrowsIQ • Powered by AI • Not a medical tool")
                    .build();
            event.getHook().sendMessageEmbeds(errorEmbed).queue();
            // Log to stdout so the host can diagnose
            System.err.println("[ThrowsIQ] Error handling /checkin: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
