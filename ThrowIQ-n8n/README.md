# ThrowsIQ ⚪🔨🥏

**AI-Powered Practice Reflection & Triage for Track & Field Throwers**

ThrowsIQ is a Discord bot that gives throwing athletes access to event-specific, AI-driven coaching feedback after every practice — right inside their team server. Athletes log a check-in with one slash command; ThrowsIQ returns a structured embed with detected technical faults, corrective cues, recommended drills, coach conversation starters, and a built-in safety guardrail that catches high-fatigue or pain-related entries before they become injuries.

---

## The Problem

### Coaching access is a structural bottleneck

Collegiate and club throwing programs routinely carry 8–15 athletes per coach. After a throwing session, most athletes leave the track without any technical feedback — they rely on memory, video review alone, or a brief hallway conversation. The gap between what happened in practice and what gets addressed compounds over weeks and becomes the ceiling on athlete development.

### Generic fitness apps do not speak the language of throws

Strava, Nike Run Club, and their kin are built for linear sport — pace, distance, heart rate. Rotational throwing is kinetically different: a 400ms full-body effort driven by sequenced hip-to-shoulder separation, high CNS demand, and implement-specific mechanics. No mainstream app tracks *footwork sequencing* or asks whether the *blocking arm stayed back long enough*. Athletes are forced to shoehorn notes about their power position into a run-distance field.

### The physical risk is real

Shot put, hammer, and discus are among the highest-stress movements in all of sport — lower-back shear, shoulder impingement, and knee torque are session-by-session concerns. Throwing through a tweak that was never flagged is how weeks become months on the sideline.

---

## Solution & Core Features

### `/checkin` — One command, full reflection

After any practice session, the athlete runs:

```
/checkin  event: Shot Put  session_type: Technical Work  notes: ...  fatigue_rating: 6
```

ThrowsIQ does the rest.

| Feature | Detail |
|---|---|
| **Sport-specific fault detection** | Analyzes free-text notes against kinetic chain mechanics for Shot Put, Hammer, and Discus specifically — not generic strength or cardio cues |
| **Technical cues & drills** | Returns event-matched corrective cues and next-session drill recommendations |
| **Coach conversation starters** | Generates targeted questions the athlete can bring to their coach, turning the next session into a directed conversation rather than another open floor |
| **Dynamic fatigue display** | Color-coded embed (green → yellow → orange → red) with a visual progress bar communicates load at a glance |
| **Built-in safety guardrails** | Automatically flags entries with `fatigue_rating ≥ 7` or pain keywords (`pain`, `hurt`, `sharp`, `strain`, `tweak`, `injured`, `sprain`) and surfaces a `⚠️ SAFETY ALERT` that directs the athlete to their athletic trainer — the bot never suggests diagnoses or encourages throwing through pain |

---

## Built With IBM Bob

ThrowsIQ was designed and built end-to-end using **[IBM Bob](https://www.ibm.com/bob)** as the primary development engine.

### Plan Mode — Architecture & Data Modeling

Bob's Plan Mode was used to produce the full technical specification before a single line of code was written. This included:

- **Domain decomposition** — defining the six-layer package structure (`config`, `model`, `command`, `service`, `formatter`, and the entry point) with clear ownership boundaries between each layer.
- **Data contract design** — specifying Java 17 `record` types for `CheckInData` and `ReflectionResult`, and enum contracts for `ThrowEvent` and `SessionType`, so every downstream layer compiled against a stable interface.
- **Service architecture** — designing the `LLMProvider` interface to decouple `ReflectionService` from any specific LLM vendor, enabling `OpenAILLMProvider` and `MockLLMProvider` to be swapped transparently.
- **Safety rule specification** — articulating the exact fatigue threshold and pain keyword set before implementation to ensure the guardrail logic was deliberate, not reactive.

The output of Plan Mode was `throwsiq-plan.md` — a living sub-task checklist that drove every subsequent implementation step.

### Agent Mode — Implementation

Bob's Agent Mode executed each sub-task sequentially:

- Scaffolded the Maven project (`pom.xml`, directory tree, `.gitignore`, `.env.example`).
- Implemented all domain models, service layer, embed formatter, and command handler.
- Introspected the JDA 5.x JAR directly (`javap`) to confirm the exact `OptionData`, `Command.Choice`, and `SlashCommandData` API before writing any JDA-specific code — zero speculation.
- Configured the `maven-shade-plugin` with `ServicesResourceTransformer` and signature-file filters for a conflict-free fat JAR.
- Ran `mvn clean package` after each sub-task and reported the compiler output before proceeding.

---

## Architecture & Tech Stack

```
/checkin slash command
        │
        ▼
 CheckInCommand          — JDA ListenerAdapter; owns the Discord interaction lifecycle
        │  builds
        ▼
 CheckInData             — Java 17 record (event, sessionType, notes, fatigueRating)
        │  passed to
        ▼
 ReflectionService       — prompt assembly, safety check, JSON parsing
        │  calls
        ▼
 LLMProvider             — interface (OpenAILLMProvider | MockLLMProvider)
        │  returns raw JSON string
        ▼
 ReflectionResult        — Java 17 record (faults, cues, drills, questions, safetyFlagged)
        │  passed to
        ▼
 EmbedFormatter          — pure formatting; builds JDA MessageEmbed
        │
        ▼
 Discord reply embed
```

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Build | Apache Maven 3.9 |
| Discord API | JDA 5.2.1 (`net.dv8tion:JDA`) |
| HTTP client | `java.net.http.HttpClient` (JDK built-in) |
| JSON | Jackson 2.17 (`jackson-databind`) |
| LLM | OpenAI `gpt-4o-mini` via Chat Completions API (`json_object` response format) |
| Offline / testing | `MockLLMProvider` — realistic hardcoded JSON, no API key required |
| Config | `dotenv-java` 3.0 — loads `.env` or OS environment variables |
| Packaging | `maven-shade-plugin` 3.6 — self-contained executable fat JAR |

---

## Getting Started

### Prerequisites

- **JDK 17+** — [Eclipse Temurin](https://adoptium.net) recommended
- **Apache Maven 3.8+**
- A Discord application with a bot token ([Discord Developer Portal](https://discord.com/developers/applications))
- *(Optional)* An OpenAI API key for live AI responses

### 1. Clone & configure

```bash
git clone https://github.com/your-org/throwsiq.git
cd throwsiq
cp .env.example .env
```

Edit `.env`:

```env
BOT_TOKEN=your_discord_bot_token_here
OPENAI_API_KEY=your_openai_api_key_here   # optional — omit to use MockLLMProvider
```

> **Never commit `.env`** — it is excluded by `.gitignore`.

### 2. Build

```bash
mvn clean package
```

This compiles the project and produces a self-contained fat JAR at `target/throwsiq-bot-1.0-SNAPSHOT.jar`.

### 3. Run

```bash
java -jar target/throwsiq-bot-1.0-SNAPSHOT.jar
```

On startup, ThrowsIQ will:

1. Load and validate environment variables.
2. Select `OpenAILLMProvider` if `OPENAI_API_KEY` is set; otherwise fall back to `MockLLMProvider` with an info log.
3. Connect to Discord and log the bot's username.
4. Register the `/checkin` slash command globally (Discord propagates global commands within ~1 hour; for instant testing, switch to guild-scoped registration in `ThrowsIQBot.java`).

### 4. Invite the bot

Generate an invite URL in the Discord Developer Portal with the **`applications.commands`** and **`bot`** scopes, then add the bot to your server.

---

## Usage Example

```
/checkin
  event:          Shot Put
  session_type:   Technical Work
  notes:          Felt like my left foot was landing too far right during the shift,
                  causing me to lose rotation before I got to the power position.
                  A few throws felt decent but the release height was inconsistent.
  fatigue_rating: 5
```

ThrowsIQ replies with a Discord embed containing:

- 🔍 **Detected Faults** — e.g. "Foot landing too far right disrupting linear momentum into the power position"
- 🎯 **Technical Cues** — e.g. "Focus on a straight-line glide path; left foot should land on the toe-board axis"
- 🏋️ **Recommended Drills** — e.g. "Standing power position throws focusing on consistent left side block"
- 💬 **Coach Conversation Starters** — e.g. "Can we mark the ring and film my glide path from behind to check foot placement?"

---

## Future Roadmap

The Discord bot is the minimum viable product — a fast, zero-friction way to validate the core coaching loop in an environment athletes already use daily.

| Phase | Target |
|---|---|
| **Mobile-responsive PWA** | A web app with the same check-in flow, athlete history view, and coach dashboard — accessible on any device without installing anything |
| **Native Android & iOS app** | Push notifications for post-practice check-in reminders, offline-capable session logging, and biometric-gated access |
| **Longitudinal analytics** | Session history, fatigue trend charts, and fault recurrence tracking — turning one-off reflections into a longitudinal development record |
| **Coach portal** | A dashboard where coaches can review their athletes' check-ins, annotate entries, and push custom drill assignments back to the athlete |
| **Multi-event expansion** | Javelin, triple jump, pole vault — same kinetic chain architecture, event-specific fault libraries |

---

## License

MIT — see `LICENSE` for details.

---

*ThrowsIQ is not a medical tool. It does not diagnose injuries or provide medical advice. Athletes experiencing sharp, worsening, or persistent pain should consult a qualified athletic trainer or medical professional.*
