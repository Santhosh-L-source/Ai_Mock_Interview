package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGeneratorService.class);

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public QuestionGeneratorService(ChatLanguageModel chatLanguageModel, ObjectMapper objectMapper) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper;
    }

    public GeneratedQuestionResponse generateNextQuestion(
            int targetQuestionNumber,
            PlannedTopic topic,
            ParsedResume resume,
            List<TurnContext> previousTurns) {

        log.info("Generating Question {} on topic '{}' ({})",
                targetQuestionNumber, topic.topic(), topic.category());

        StringBuilder historyBuilder = new StringBuilder();
        if (previousTurns != null && !previousTurns.isEmpty()) {
            historyBuilder.append("Recent Interview Context:\n");
            for (TurnContext turn : previousTurns) {
                historyBuilder.append("Q").append(turn.questionNumber()).append(": ").append(turn.questionText()).append("\n");
                if (turn.candidateTranscript() != null && !turn.candidateTranscript().isBlank()) {
                    historyBuilder.append("Candidate Answer: ").append(turn.candidateTranscript()).append("\n");
                }
            }
        } else {
            historyBuilder.append("This is the first question of the interview.\n");
        }

        String prompt = """
            You are a friendly technical interviewer conducting a live voice interview for a 1st or 2nd-year college student.
            Generate ONE SHORT, DIRECT question (Question #%d) for candidate '%s'.

            Interview Focus for this Turn:
            - Category: %s
            - Topic: %s
            - Focus Area: %s

            Candidate Profile:
            - Declared Skills: %s
            - Summary: %s

            %s

            STRICT RULES FOR 1ST/2ND-YEAR COLLEGE STUDENTS:
            1. SHORT & CONCISE: 1 to 2 short sentences ONLY (maximum 30-35 words). Must be easy to read and understand aloud.
            2. SINGLE TOPIC ONLY: Ask about ONE single fundamental concept. DO NOT ask multi-part compound questions (do NOT combine multiple questions with 'and also explain... and how did you...').
            3. APPROPRIATE DIFFICULTY:
               - RESUME_SKILL: Ask about 1 basic concept, definition, or syntax feature (e.g., "In Java, what is the difference between an Interface and an Abstract Class?", "In React, how do you pass data from a parent component to a child?", "In Python, what is the difference between a list and a tuple?").
               - DSA_BASIC: Ask about 1 foundational data structure or search concept (e.g., "How does binary search find a target in a sorted array, and what is its time complexity?", "What is the main difference between an Array and a LinkedList?").
               - BEHAVIORAL: Ask 1 simple project/teamwork question (e.g., "Can you tell me about a coding bug you encountered in a class project and how you fixed it?").
            4. ABSOLUTELY NO COMPLEX TOPICS: Never ask about enterprise distributed systems, microservices, Kafka, Kubernetes, or large-scale caching.

            Return ONLY a valid JSON object without markdown or code fences:
            {
              "questionNumber": %d,
              "category": "%s",
              "topic": "%s",
              "questionText": "Short 1-2 sentence spoken question under 35 words",
              "contextRationale": "Why this specific question was chosen",
              "expectedKeyPoints": "Key basic concepts the student should explain"
            }
            """.formatted(
                targetQuestionNumber,
                resume.candidateName(),
                topic.category(),
                topic.topic(),
                topic.focusArea(),
                String.join(", ", resume.technicalSkills()),
                resume.summary(),
                historyBuilder.toString(),
                targetQuestionNumber,
                topic.category().name(),
                topic.topic()
        );

        try {
            String response = chatLanguageModel.generate(prompt);
            return parseQuestionJson(response, targetQuestionNumber, topic);
        } catch (Exception e) {
            log.error("Failed to generate question with LLM, using contextual fallback: {}", e.getMessage());
            return fallbackQuestion(targetQuestionNumber, topic);
        }
    }

    private GeneratedQuestionResponse parseQuestionJson(String rawResponse, int questionNumber, PlannedTopic topic) {
        try {
            String cleaned = cleanJsonString(rawResponse);
            JsonNode root = objectMapper.readTree(cleaned);

            String text = root.path("questionText").asText();
            if (text == null || text.isBlank()) {
                return fallbackQuestion(questionNumber, topic);
            }
            String rationale = root.path("contextRationale").asText("Targeted roadmap progression.");
            String expected = root.path("expectedKeyPoints").asText("Fundamental concepts and clear explanation.");

            return new GeneratedQuestionResponse(
                    questionNumber,
                    topic.category(),
                    topic.topic(),
                    text,
                    rationale,
                    expected
            );
        } catch (Exception e) {
            log.warn("Error parsing LLM question JSON: {}, using fallback", rawResponse, e);
            return fallbackQuestion(questionNumber, topic);
        }
    }

    private GeneratedQuestionResponse fallbackQuestion(int questionNumber, PlannedTopic topic) {
        String questionText = switch (topic.category()) {
            case RESUME_SKILL ->
                String.format("Could you explain how %s works and how you used it in your coursework or projects?",
                        topic.topic());
            case DSA_BASIC ->
                String.format("How does %s work, and what is its time complexity?",
                        topic.topic());
            case BEHAVIORAL ->
                String.format("Can you share a time you worked with teammates or fixed a bug on a project involving %s?",
                        topic.topic());
        };

        return new GeneratedQuestionResponse(
                questionNumber,
                topic.category(),
                topic.topic(),
                questionText,
                "Student fallback question for " + topic.topic(),
                "Candidate should explain core concepts and practical understanding."
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
