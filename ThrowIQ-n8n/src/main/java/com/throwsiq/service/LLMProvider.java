package com.throwsiq.service;

/**
 * Abstraction over any LLM backend.  The single method accepts a fully-assembled prompt string and
 * returns the model's raw text response.  Callers ({@link ReflectionService}) never import any
 * vendor SDK — all vendor-specific code lives in implementing classes.
 */
public interface LLMProvider {

    /**
     * Sends {@code prompt} to the backing LLM and returns the response text.
     *
     * @param prompt the fully-assembled prompt string
     * @return the model's raw response (expected to be valid JSON per the system contract)
     * @throws Exception on network errors, auth failures, or unexpected API responses
     */
    String generateReflection(String prompt) throws Exception;
}
