package com.throwsiq;

import com.throwsiq.command.CheckInCommand;
import com.throwsiq.config.BotConfig;
import com.throwsiq.model.SessionType;
import com.throwsiq.model.ThrowEvent;
import com.throwsiq.service.LLMProvider;
import com.throwsiq.service.MockLLMProvider;
import com.throwsiq.service.OpenAILLMProvider;
import com.throwsiq.service.ReflectionService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;

/**
 * Application entry point.  Reads config, wires all components, registers the
 * {@code /checkin} slash command globally, and starts the JDA gateway connection.
 */
public class ThrowsIQBot {

    public static void main(String[] args) throws InterruptedException {
        // ── Load config ───────────────────────────────────────────────────────
        String token = BotConfig.getBotToken();

        // ── Smart provider selection ──────────────────────────────────────────
        LLMProvider provider;
        try {
            String openAiKey = BotConfig.getOpenAiKey();
            provider = new OpenAILLMProvider(openAiKey);
            System.out.println("[ThrowsIQ] OpenAI API key found — using OpenAILLMProvider (gpt-4o-mini).");
        } catch (IllegalStateException e) {
            System.out.println("[ThrowsIQ] No OPENAI_API_KEY found — falling back to MockLLMProvider for offline testing.");
            provider = new MockLLMProvider();
        }

        // ── Wire services ─────────────────────────────────────────────────────
        ReflectionService reflectionService = new ReflectionService(provider);
        CheckInCommand    checkInCommand    = new CheckInCommand(reflectionService);

        // ── Build JDA ─────────────────────────────────────────────────────────
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(checkInCommand)
                .build()
                .awaitReady();

        System.out.println("[ThrowsIQ] Connected as: " + jda.getSelfUser().getAsTag());

        // ── Register /checkin slash command globally ───────────────────────────
        //
        // event — string with choices mapped from ThrowEvent enum
        OptionData eventOption = new OptionData(OptionType.STRING, "event",
                "Your throwing event", true)
                .addChoices(
                        new Command.Choice(ThrowEvent.SHOT_PUT.displayName(), ThrowEvent.SHOT_PUT.name()),
                        new Command.Choice(ThrowEvent.HAMMER.displayName(),   ThrowEvent.HAMMER.name()),
                        new Command.Choice(ThrowEvent.DISCUS.displayName(),   ThrowEvent.DISCUS.name())
                );

        // session_type — string with choices mapped from SessionType enum
        OptionData sessionOption = new OptionData(OptionType.STRING, "session_type",
                "Type of training session", true)
                .addChoices(
                        new Command.Choice(SessionType.LIFTING.displayName(),       SessionType.LIFTING.name()),
                        new Command.Choice(SessionType.THROWING.displayName(),      SessionType.THROWING.name()),
                        new Command.Choice(SessionType.TECHNICAL_WORK.displayName(), SessionType.TECHNICAL_WORK.name()),
                        new Command.Choice(SessionType.HYBRID.displayName(),        SessionType.HYBRID.name()),
                        new Command.Choice(SessionType.RECOVERY.displayName(),      SessionType.RECOVERY.name())
                );

        // notes — free-text practice notes (required)
        OptionData notesOption = new OptionData(OptionType.STRING, "notes",
                "Describe your practice session and any technical issues", true);

        // fatigue_rating — integer 1–10 (required)
        OptionData fatigueOption = new OptionData(OptionType.INTEGER, "fatigue_rating",
                "Fatigue / soreness level (1 = fresh, 10 = exhausted)", true)
                .setMinValue(1)
                .setMaxValue(10);

        jda.updateCommands()
                .addCommands(
                        Commands.slash("checkin", "Log your practice session and get AI coaching feedback")
                                .addOptions(eventOption, sessionOption, notesOption, fatigueOption)
                )
                .queue(cmds -> System.out.println("[ThrowsIQ] Slash commands registered: "
                        + cmds.stream().map(c -> "/" + c.getName()).toList()));
    }
}
