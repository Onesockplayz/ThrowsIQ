# ThrowsIQ — Plan

## Top-Level Overview

ThrowsIQ is a greenfield, Discord-based practice reflection and coaching assistant for Track & Field throwers.
It is a stateless Java application built with **Maven + JDA** (Java Discord API).

Athletes interact with the bot via a single slash command (`/checkin`). The bot collects structured inputs
(event, session type, practice notes, fatigue/soreness rating), sends the notes to the **LLM API** for
fault analysis and coach question generation, and returns a rich Discord embed with:
- a session summary
- AI-generated technical cues and drills
- AI-generated conversation-starter questions for their coach
- a conditional safety callout when soreness >= 7 OR pain/injury keywords are detected in the notes

No data is persisted — every interaction is fully self-contained.

---

## Architecture Diagram (text description)

```
/checkin slash command
        │
        ▼
 CheckInCommand  (JDA SlashCommandInteractionEvent handler)
        │  builds
        ▼
 CheckInData  (Java Record — pure data carrier)
        │  passed to
        ▼
 ReflectionService  (domain logic — calls OpenAI, returns ReflectionResult)
        │  returns
        ▼
 ReflectionResult  (Java Record — AI output + safety flag)
        │  passed to
        ▼
 EmbedFormatter  (formats ReflectionResult + CheckInData → MessageEmbed)
        │
        ▼
 Discord reply (ephemeral or public, per config)
```

---

## Package Layout

```
com.throwsiq
├── ThrowsIQBot.java              — entry point, JDA setup, command registration
├── command/
│   └── CheckInCommand.java       — slash command handler
├── model/
│   ├── CheckInData.java          — record: event, sessionType, notes, fatigueRating
│   ├── ReflectionResult.java     — record: faults, cues, coachQuestions, safetyFlag
│   ├── ThrowEvent.java           — enum: SHOT_PUT, HAMMER, DISCUS
│   └── SessionType.java          — enum: LIFTING, THROWING, TECHNICAL, HYBRID, RECOVERY
├── service/
│   ├── LLMProvider.java          — interface: String generateReflection(String prompt)
│   ├── OpenAILLMProvider.java    — implements LLMProvider via OpenAI Java SDK
│   └── ReflectionService.java    — prompt assembly, safety check, JSON parsing; depends on LLMProvider
├── formatter/
│   └── EmbedFormatter.java       — builds JDA MessageEmbed from domain objects
└── config/
    └── BotConfig.java            — reads env vars: BOT_TOKEN, OPENAI_API_KEY
```

---

## Sub-Tasks

---

### Sub-Task 1 — Project Scaffolding

**Status:** `[ ] pending`

**Intent**
Create the Maven project structure, `pom.xml` with all required dependencies (JDA, OpenAI Java client,
dotenv for env var loading), and the entry-point class. Establish the package layout before any logic
is written so all subsequent sub-tasks have a consistent home.

**Expected Outcomes**
- `pom.xml` present with JDA, OpenAI client, and dotenv dependencies at pinned versions
- `ThrowsIQBot.java` entry point compiles and can start JDA (token read from env)
- `BotConfig.java` reads `BOT_TOKEN` and `OPENAI_API_KEY` from environment variables / `.env` file
- `mvn compile` succeeds with no errors

**Todo List**
1. Create `pom.xml` — Java 17, JDA (`net.dv8tion:JDA`), OpenAI Java SDK (`com.openai:openai-java`),
   dotenv-java (`io.github.cdimascio:dotenv-java`), and `maven-compiler-plugin` for Java 17
2. Create the full package directory tree under `src/main/java/com/throwsiq/`
3. Write `BotConfig.java` — static helper that reads env vars via dotenv, exposes `getBotToken()` and `getOpenAiKey()`
4. Write `ThrowsIQBot.java` — `main()` that builds a JDA instance with `JDABuilder`, registers the
   slash command, and adds the event listener; no logic yet, just the wiring skeleton
5. Add a `.env.example` file documenting required environment variables

**Relevant Context**
- JDA latest stable: `5.x` (use `JDABuilder.createDefault(token)`)
- OpenAI Java SDK: `com.openai:openai-java` (official SDK)
- dotenv-java: `io.github.cdimascio:dotenv-java`
- Slash commands in JDA 5 use `Commands.slash(...)` with `addOption(...)` and are registered via
  `jda.updateCommands().addCommands(...).queue()`

---

### Sub-Task 2 — Domain Model (Records & Enums)

**Status:** `[ ] pending`

**Intent**
Define all pure data structures as Java records and enums. These carry no logic — they are the
language of the system. Getting them right first ensures every other layer compiles cleanly against
a stable contract.

**Expected Outcomes**
- `ThrowEvent` enum with `SHOT_PUT`, `HAMMER`, `DISCUS` and a `displayName()` method
- `SessionType` enum with `LIFTING`, `THROWING`, `TECHNICAL_WORK`, `HYBRID`, `RECOVERY` and a `displayName()` method
- `CheckInData` record with fields: `ThrowEvent event`, `SessionType sessionType`, `String notes`, `int fatigueRating`
- `ReflectionResult` record with fields: `List<String> detectedFaults`, `List<String> technicalCues`,
  `List<String> drills`, `List<String> coachQuestions`, `boolean safetyFlagged`
- All classes compile with no warnings

**Todo List**
1. Write `ThrowEvent.java` enum — three values, each storing a display name string
2. Write `SessionType.java` enum — five values, each storing a display name string
3. Write `CheckInData.java` as a Java record (Java 17 `record` keyword)
4. Write `ReflectionResult.java` as a Java record

**Relevant Context**
- Java 17 records are immutable by default; use them for all data carriers
- `displayName()` is a regular instance method on the enum that returns a human-readable label
  (e.g. `SHOT_PUT → "Shot Put"`) for use in Discord embed titles

---

### Sub-Task 3 — `/checkin` Slash Command Handler

**Status:** `[ ] pending`

**Intent**
Implement the JDA event listener that handles the `/checkin` slash command. It is responsible for
reading the five Discord slash command options, validating them, constructing a `CheckInData` record,
and delegating to `ReflectionService`. It owns the Discord interaction lifecycle (deferring,
replying, error handling) but contains no domain logic itself.

**Expected Outcomes**
- `/checkin` registered with five options: `event` (string choices), `session_type` (string choices),
  `notes` (string), `fatigue_rating` (integer 1–10)
- `CheckInCommand` implements `ListenerAdapter` and overrides `onSlashCommandInteraction`
- On a valid `/checkin` call, the command defers the reply, calls `ReflectionService`, formats
  via `EmbedFormatter`, and sends the embed reply
- Invalid fatigue input (outside 1–10) returns a user-facing error message without a stack trace

**Todo List**
1. Write `CheckInCommand.java` extending `ListenerAdapter`
2. Register the slash command definition in `ThrowsIQBot.java` — `event` and `session_type` as
   string options with Discord `Choice` entries matching the enums; `notes` as a required string;
   `fatigue_rating` as a required integer
3. In `onSlashCommandInteraction`: call `event.deferReply()`, extract all options, validate
   `fatigueRating` is 1–10, build `CheckInData`, call `ReflectionService.reflect(checkInData)`,
   pass both to `EmbedFormatter.build(...)`, send result via `hook.sendMessageEmbeds(...).queue()`
4. Handle exceptions from the OpenAI call gracefully — send a user-friendly error embed

**Relevant Context**
- JDA 5: use `event.deferReply()` before any async work; reply via `event.getHook().sendMessageEmbeds(...)`
- `OptionMapping.getAsString()` / `getAsInt()` for reading option values
- `CheckInCommand` is registered as a listener in `ThrowsIQBot.java` via `jda.addEventListener(...)`

---

### Sub-Task 4 — LLMProvider Interface + ReflectionService

**Status:** `[ ] pending`

**Intent**
Introduce an `LLMProvider` interface to decouple `ReflectionService` from any specific LLM vendor.
`ReflectionService` owns all domain logic (prompt assembly, JSON parsing, safety check) and depends
only on `LLMProvider` — it never imports the OpenAI SDK directly. `OpenAILLMProvider` is the
production implementation that wraps the OpenAI Java SDK. This separation lets any LLM be swapped in
via constructor injection and enables unit testing with a mock provider.

**Expected Outcomes**
- `LLMProvider` interface in `com.throwsiq.service` with a single method: `String generateReflection(String prompt)`
- `OpenAILLMProvider` implements `LLMProvider`; it holds the `OpenAI` client, builds
  `ChatCompletionCreateParams`, calls the API, and returns the raw response string
- `ReflectionService` constructor accepts `LLMProvider` (not `OpenAI`); it assembles the prompt,
  calls `provider.generateReflection(prompt)`, parses the returned JSON, and runs the safety check
- The OpenAI prompt instructs the model to act as a Track & Field throwing coach, analyze the notes
  for technical faults specific to the athlete's event, and return a JSON object with keys:
  `detectedFaults` (array), `technicalCues` (array), `drills` (array), `coachQuestions` (array)
- The safety flag is set to `true` if `fatigueRating >= 6` OR the notes text contains any of a
  defined set of pain/injury keywords (e.g. "pain", "hurt", "sore", "ache", "strain", "injured")
- The prompt explicitly instructs the model: do NOT suggest medical diagnoses and do NOT encourage
  throwing through pain
- Response JSON is parsed into `ReflectionResult` using a lightweight approach (e.g. `org.json` or
  Jackson)
- A `MockLLMProvider` can be written in tests by implementing `LLMProvider` and returning a
  hardcoded JSON string — no real HTTP calls needed

**Todo List**
1. Add `org.json` or Jackson dependency to `pom.xml` if needed for JSON parsing
2. Write `LLMProvider.java` — a single-method interface: `String generateReflection(String prompt)`
3. Write `OpenAILLMProvider.java` implementing `LLMProvider` — constructor accepts `OpenAI` client;
   `generateReflection` builds `ChatCompletionCreateParams` with `gpt-4o-mini`, JSON response format,
   calls `client.chat().completions().create(...)`, and returns the content string
4. Write `ReflectionService.java` with constructor accepting `LLMProvider` (not `OpenAI` directly)
5. Write the prompt-assembly private method in `ReflectionService` — include event name, session type,
   notes, fatigue rating; specify the exact JSON response schema; include the safety disclaimer
6. Write the JSON response parser in `ReflectionService` — extract the four arrays into lists
7. Write the safety-check private method in `ReflectionService` — `fatigueRating >= 6` OR keyword scan of `notes`
8. Construct and return `ReflectionResult`

**Relevant Context**
- `LLMProvider` is the only type `ReflectionService` should import from this layer — keep the OpenAI
  SDK imports inside `OpenAILLMProvider` only
- OpenAI Java SDK method chain: `client.chat().completions().create(ChatCompletionCreateParams...)`
- Use `ChatCompletionCreateParams.builder()` with `.model(...)`, `.messages(...)`, `.responseFormat(...)`
- The prompt should be a `system` message defining the coach persona + JSON contract, and a `user`
  message containing the athlete's actual check-in data
- Safety keywords list is defined as a private static constant in `ReflectionService`
- In Sub-Task 6, `ThrowsIQBot` constructs `new OpenAILLMProvider(openAiClient)` and passes it into
  `new ReflectionService(provider)`

---

### Sub-Task 5 — EmbedFormatter

**Status:** `[ ] pending`

**Intent**
Implement `EmbedFormatter` — a pure formatting class that takes `CheckInData` and `ReflectionResult`
and returns a fully constructed JDA `MessageEmbed`. It contains zero domain logic; its only job is
layout, color, and field structure.

**Expected Outcomes**
- `EmbedFormatter.build(CheckInData, ReflectionResult)` returns a `MessageEmbed`
- Embed sections:
  - **Title**: "ThrowsIQ — [Event] Check-In" with a relevant emoji
  - **Color**: Green (normal), Yellow (fatigue 4–5), Orange (fatigue 6–7), Red (fatigue 8–10)
  - **Field — Session**: session type display name
  - **Field — Fatigue/Soreness**: `X / 10` with an emoji scale
  - **Field — Detected Faults**: bulleted list from `ReflectionResult.detectedFaults()`
  - **Field — Technical Cues & Drills**: paired cues and drills
  - **Field — Coach Conversation Starters**: numbered list of questions
  - **Safety Callout** (conditional): shown in red text when `safetyFlagged == true`; text advises
    rest, seeing a trainer, and explicitly states the bot does not provide medical advice and
    athletes should never throw through pain
  - **Footer**: "ThrowsIQ • Powered by AI • Not a medical tool"
- Embed respects Discord's 1024-char field limit (truncate with "…" if needed)

**Todo List**
1. Write `EmbedFormatter.java` as a stateless utility class with one public static method `build(...)`
2. Implement embed color logic based on fatigue rating thresholds
3. Build each embed field from domain data, formatting lists as newline-separated bullet strings
4. Implement the conditional safety callout field — use `⚠️` prefix and clear disclaimer language
5. Add footer with "ThrowsIQ • Powered by AI • Not a medical tool"
6. Add a truncation helper to ensure no field value exceeds 1024 characters

**Relevant Context**
- JDA `EmbedBuilder` API: `.setTitle(...)`, `.setColor(...)`, `.addField(name, value, inline)`,
  `.setFooter(...)`, `.build()`
- Discord embed field value max: 1024 characters; total embed max: 6000 characters
- Keep fields `inline: false` for readability on mobile clients

---

### Sub-Task 6 — Integration & Smoke Test

**Status:** `[ ] pending`

**Intent**
Wire all components together in `ThrowsIQBot.java`, verify the full command flow compiles and the
project is runnable. Add a `README.md` covering setup, environment variables, and how to run.

**Expected Outcomes**
- `mvn package` produces a runnable fat JAR (via `maven-shade-plugin` or `maven-assembly-plugin`)
- `README.md` documents: prerequisites (Java 17+, Maven), env vars (`BOT_TOKEN`, `OPENAI_API_KEY`),
  how to create the Discord bot application, how to run, and example embed output description
- All components are wired: `BotConfig` → `ThrowsIQBot` → `CheckInCommand` → `ReflectionService` + `EmbedFormatter`
- `.gitignore` excludes `.env` and target/

**Todo List**
1. Update `ThrowsIQBot.java` to construct `OpenAILLMProvider` (with OpenAI client from `BotConfig`),
   then construct `ReflectionService(provider)`, and pass it into `CheckInCommand`
2. Confirm slash command registration fires on startup
3. Configure `maven-shade-plugin` in `pom.xml` to produce a self-contained executable JAR with a
   `Main-Class` manifest entry pointing to `ThrowsIQBot`
4. Write `README.md`
5. Write `.gitignore`
6. Run `mvn package` and confirm it builds successfully

**Relevant Context**
- `maven-shade-plugin` goal `shade` bound to `package` phase
- Manifest entry: `<mainClass>com.throwsiq.ThrowsIQBot</mainClass>`
- JDA requires a `META-INF/services` merge strategy in the shade plugin config to avoid runtime errors
