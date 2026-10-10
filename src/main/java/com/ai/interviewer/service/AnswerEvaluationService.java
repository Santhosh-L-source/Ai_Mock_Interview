package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.ParsedResume;
import com.ai.interviewer.domain.dto.PlannedTopic;
import com.ai.interviewer.domain.dto.TurnEvaluation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnswerEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AnswerEvaluationService.class);

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public AnswerEvaluationService(ChatLanguageModel chatLanguageModel, ObjectMapper objectMapper) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper;
    }

    @org.springframework.scheduling.annotation.Async
    public java.util.concurrent.CompletableFuture<TurnEvaluation> evaluateAnswerAsync(
            String questionText,
            String expectedKeyPoints,
            String candidateTranscript,
            PlannedTopic topic,
            ParsedResume resume) {
        TurnEvaluation evaluation = evaluateAnswer(questionText, expectedKeyPoints, candidateTranscript, topic, resume);
        return java.util.concurrent.CompletableFuture.completedFuture(evaluation);
    }

    public TurnEvaluation evaluateAnswer(
            String questionText,
            String expectedKeyPoints,
            String candidateTranscript,
            PlannedTopic topic,
            ParsedResume resume) {

        log.info("Evaluating answer for Question #{} ('{}')", topic.questionNumber(), topic.topic());

        if (candidateTranscript == null || candidateTranscript.trim().isBlank()) {
            return new TurnEvaluation(
                    0.0,
                    0.0,
                    List.of(),
                    List.of("No verbal answer recorded"),
                    "Candidate remained silent or provided no transcript.",
                    "Ensure you speak clearly into the microphone and attempt an answer.",
                    "No response was detected for this turn.",
                    "Try to explain your initial thoughts or ask for clarification if stuck."
            );
        }

        String prompt = """
            You are an encouraging, supportive campus mentor and technical interviewer evaluating a 1st or 2nd-year college student.
            Grade their spoken answer generously with strong partial credit.

            Interview Context:
            - Category: %s
            - Topic: %s
            - Focus Area: %s
            - Question Asked: %s
            - Target Concept: %s

            Student Background:
            - Candidate Name: %s
            - Skills: %s

            Student Spoken Answer:
            "%s"

            GRADING PHILOSOPHY FOR 1ST & 2ND-YEAR COLLEGE STUDENTS:
            1. HIGH PARTIAL CREDIT: 1st and 2nd-year students are at the beginning of their engineering journey. Never expect senior precision or textbook definitions.
            2. SCORING BENCHMARKS:
               - RELATED OR INTUITIVE ATTEMPT: If the student mentions ANY related keyword, basic concept, general idea, or simple syntax (even if informal, incomplete, or brief): Award 7.0 to 8.5 / 10.
               - GOOD BASIC EXPLANATION: If the student explains the general purpose or gets the main idea right: Award 8.5 to 9.5 / 10.
               - ROUGH OR MINIMAL ATTEMPT: If the candidate tries to speak about the topic or related concepts: Award 6.0 to 7.0 / 10.
               - NEVER give harsh 1.0 or 2.0 scores if the student made an honest attempt relating to the topic.
            3. COMMUNICATION SCORE: If they spoke audibly, coherently, and made an effort, award 7.5 to 9.0 / 10.
            4. TONE: Warm, encouraging, highlighting what they did well, with 1 constructive tip for future learning.

            Return ONLY a valid JSON object without markdown or code fences:
            {
              "technicalScore": 8.0,
              "communicationScore": 8.5,
              "keyPointsCovered": ["Identified the core idea", "Explained basic purpose"],
              "keyPointsMissed": ["Can mention additional details as you learn more"],
              "strengths": "Great job explaining the main concept with good confidence.",
              "areasToImprove": "Keep practicing with coding examples to solidify the theory.",
              "feedbackSummary": "Solid foundational understanding and great attempt for a 1st/2nd year student.",
              "suggestedImprovement": "Connecting the concept to a mini-project will make your answers even stronger."
            }
            """.formatted(
                topic.category(),
                topic.topic(),
                topic.focusArea(),
                questionText,
                expectedKeyPoints != null ? expectedKeyPoints : "Foundational knowledge and intuitive explanation",
                resume != null ? resume.candidateName() : "Student Candidate",
                resume != null && resume.technicalSkills() != null ? String.join(", ", resume.technicalSkills()) : "General CS",
                candidateTranscript
        );

        try {
            String response = chatLanguageModel.generate(prompt);
            return parseEvaluationJson(response, candidateTranscript);
        } catch (Exception e) {
            log.error("Failed to evaluate answer with LLM, applying heuristic fallback: {}", e.getMessage());
            return fallbackEvaluation(candidateTranscript, topic);
        }
    }

    private TurnEvaluation parseEvaluationJson(String rawResponse, String candidateTranscript) {
        try {
            String cleaned = cleanJsonString(rawResponse);
            JsonNode root = objectMapper.readTree(cleaned);

            double techScore = Math.min(10.0, Math.max(0.0, root.path("technicalScore").asDouble(7.5)));
            double commScore = Math.min(10.0, Math.max(0.0, root.path("communicationScore").asDouble(8.0)));

            List<String> covered = new ArrayList<>();
            if (root.has("keyPointsCovered") && root.get("keyPointsCovered").isArray()) {
                root.get("keyPointsCovered").forEach(n -> covered.add(n.asText()));
            }

            List<String> missed = new ArrayList<>();
            if (root.has("keyPointsMissed") && root.get("keyPointsMissed").isArray()) {
                root.get("keyPointsMissed").forEach(n -> missed.add(n.asText()));
            }

            String strengths = root.path("strengths").asText("Demonstrated relevant conceptual intuition.");
            String areasToImprove = root.path("areasToImprove").asText("Continue practicing with hands-on coding exercises.");
            String feedbackSummary = root.path("feedbackSummary").asText("Good foundational effort with positive potential.");
            String suggestedImprovement = root.path("suggestedImprovement").asText("Keep building small projects to reinforce key concepts.");

            return new TurnEvaluation(
                    techScore,
                    commScore,
                    covered,
                    missed,
                    strengths,
                    areasToImprove,
                    feedbackSummary,
                    suggestedImprovement
            );
        } catch (Exception e) {
            log.warn("Error parsing LLM evaluation JSON: {}, building fallback", rawResponse, e);
            return fallbackEvaluation(candidateTranscript, null);
        }
    }

    private TurnEvaluation fallbackEvaluation(String candidateTranscript, PlannedTopic topic) {
        int wordCount = candidateTranscript != null ? candidateTranscript.split("\\s+").length : 0;
        double techScore = wordCount > 15 ? 8.0 : (wordCount > 5 ? 7.0 : 6.0);
        double commScore = wordCount > 15 ? 8.5 : (wordCount > 5 ? 7.5 : 6.5);

        return new TurnEvaluation(
                techScore,
                commScore,
                List.of("Addressed the core concept", "Attempted the question verbally"),
                List.of("Can elaborate further with continued practice"),
                "Good conceptual attempt and clear willingness to learn.",
                "Keep building small coding projects to gain hands-on confidence.",
                "Solid baseline attempt for an early-career student.",
                "Practice explaining concepts out loud to build interview fluency."
        );
    }

    private String cleanJsonString(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
