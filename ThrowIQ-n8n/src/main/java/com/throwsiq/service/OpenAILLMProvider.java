package com.throwsiq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Production {@link LLMProvider} implementation that calls the OpenAI Chat Completions API
 * directly via {@link java.net.http.HttpClient} and Jackson — no third-party SDK required.
 *
 * <p>Model: {@code gpt-4o-mini} with {@code response_format: { "type": "json_object" }} so the
 * model is constrained to return valid JSON, which {@link ReflectionService} then parses.
 */
public class OpenAILLMProvider implements LLMProvider {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL   = "gpt-4o-mini";

    private final String apiKey;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public OpenAILLMProvider(String apiKey) {
        this.apiKey  = apiKey;
        this.http    = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper  = new ObjectMapper();
    }

    @Override
    public String generateReflection(String prompt) throws Exception {
        // Build the request body: system message (coach persona + JSON schema) + user message (data)
        ObjectNode body = mapper.createObjectNode();
        body.put("model", MODEL);

        // response_format: { "type": "json_object" }
        ObjectNode responseFormat = mapper.createObjectNode();
        responseFormat.put("type", "json_object");
        body.set("response_format", responseFormat);

        // messages array — system sets the persona+contract, user supplies the athlete's data
        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMsg = mapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content",
                "You are an expert Track & Field throwing coach specializing in Shot Put, Hammer, "
                + "and Discus. Analyze the athlete's practice notes and return ONLY a JSON object "
                + "with exactly these four keys (each an array of strings):\n"
                + "  \"detectedFaults\"  — technical faults observed in the notes\n"
                + "  \"technicalCues\"   — specific corrective cues to fix those faults\n"
                + "  \"drills\"          — recommended drills for the next session\n"
                + "  \"coachQuestions\"  — conversation-starter questions for the athlete to ask their coach\n"
                + "Important: do NOT suggest medical diagnoses. Do NOT encourage throwing through sharp pain. "
                + "If no faults are apparent, provide general performance improvement cues. "
                + "Return valid JSON only — no markdown, no extra text.");
        messages.add(systemMsg);

        ObjectNode userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        body.set("messages", messages);

        String requestJson = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error " + response.statusCode()
                    + ": " + response.body());
        }

        // Extract the content string from choices[0].message.content
        return mapper.readTree(response.body())
                .path("choices").path(0)
                .path("message").path("content")
                .asText();
    }
}
