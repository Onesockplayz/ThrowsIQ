package com.throwsiq.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Loads required environment variables from a {@code .env} file (if present) or the process
 * environment. Fails fast with a clear message if a required variable is missing.
 */
public final class BotConfig {

    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private BotConfig() {}

    /**
     * Returns the Discord bot token ({@code BOT_TOKEN}).
     *
     * @throws IllegalStateException if the variable is absent or blank
     */
    public static String getBotToken() {
        return require("BOT_TOKEN");
    }

    /**
     * Returns the OpenAI API key ({@code OPENAI_API_KEY}).
     *
     * @throws IllegalStateException if the variable is absent or blank
     */
    public static String getOpenAiKey() {
        return require("OPENAI_API_KEY");
    }

    private static String require(String key) {
        String value = DOTENV.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required environment variable '" + key + "' is not set. "
                    + "Copy .env.example to .env and fill in the value.");
        }
        return value;
    }
}
