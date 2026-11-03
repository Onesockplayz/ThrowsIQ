package com.throwsiq.service;

/**
 * Test-only {@link LLMProvider} that returns a realistic, hardcoded JSON payload.
 * No network calls are made — safe to use without an API key.
 */
public class MockLLMProvider implements LLMProvider {

    private static final String MOCK_RESPONSE = """
            {
              "detectedFaults": [
                "Early trunk rotation causing the implement to lag behind the hips",
                "Heel coming off the ground too early during the power position",
                "Right arm sweeping wide instead of staying close to the body through release"
              ],
              "technicalCues": [
                "Stay patient — let the legs drive first before the upper body rotates",
                "Keep your heel planted until your knee has fully extended",
                "Elbow leads the hand; keep the arm tight on the way through"
              ],
              "drills": [
                "Standing throws focusing on leg-to-hip sequence without arm involvement",
                "Slow-motion walk-through of the power position with a partner checking heel contact",
                "Towel drill: wrap a towel around the throwing arm to reinforce a tight elbow path"
              ],
              "coachQuestions": [
                "Can we film my power position from behind to check my heel lift timing?",
                "What specific cue do you use to keep the implement close during the swing?",
                "Should I be thinking about driving my knee up or pushing through my hip first?"
              ]
            }
            """;

    @Override
    public String generateReflection(String prompt) {
        return MOCK_RESPONSE;
    }
}
