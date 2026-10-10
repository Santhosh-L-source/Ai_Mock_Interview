package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.ParsedResume;
import com.ai.interviewer.domain.dto.PlannedTopic;
import com.ai.interviewer.domain.dto.QuestionCategory;
import com.ai.interviewer.domain.dto.TurnEvaluation;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AnswerEvaluationServiceTest {

    private ChatLanguageModel chatLanguageModel;
    private ObjectMapper objectMapper;
    private AnswerEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        objectMapper = new ObjectMapper();
        evaluationService = new AnswerEvaluationService(chatLanguageModel, objectMapper);
    }

    @Test
    @DisplayName("Should evaluate student answer with LLM and extract scores and feedback")
    void testEvaluateAnswerWithLlm() {
        String mockLlmJson = """
            {
              "technicalScore": 8.5,
              "communicationScore": 9.0,
              "keyPointsCovered": ["Identified HashMap data structure", "Explained O(1) average lookup"],
              "keyPointsMissed": ["Did not mention collision resolution chaining"],
              "strengths": "Clear explanation of hash table basics.",
              "areasToImprove": "Mention how collisions are resolved.",
              "feedbackSummary": "Strong understanding with good articulation.",
              "suggestedImprovement": "Add a brief mention of linked lists or trees for bucket collisions."
            }
            """;

        when(chatLanguageModel.generate(anyString())).thenReturn(mockLlmJson);

        PlannedTopic topic = new PlannedTopic(
                1,
                QuestionCategory.DSA_BASIC,
                "HashMaps & Lookup Complexity",
                "Hash table mechanics and Big-O",
                "Assess fundamental data structure mechanics"
        );

        ParsedResume resume = new ParsedResume(
                "Jane Doe",
                List.of("Python", "Java"),
                List.of("Software Engineering"),
                1,
                "Student developer",
                List.of()
        );

        TurnEvaluation evaluation = evaluationService.evaluateAnswer(
                "How does a HashMap achieve O(1) lookups?",
                "Hashing function, index mapping, O(1) average time",
                "A HashMap hashes the key to compute an index in an underlying array, allowing instant O(1) average lookup.",
                topic,
                resume
        );

        assertNotNull(evaluation);
        assertEquals(8.5, evaluation.technicalScore());
        assertEquals(9.0, evaluation.communicationScore());
        assertEquals(2, evaluation.keyPointsCovered().size());
        assertEquals(1, evaluation.keyPointsMissed().size());
        assertEquals("Clear explanation of hash table basics.", evaluation.strengths());
    }

    @Test
    @DisplayName("Should handle empty transcript gracefully with zero score")
    void testEvaluateEmptyTranscript() {
        PlannedTopic topic = new PlannedTopic(
                1,
                QuestionCategory.RESUME_SKILL,
                "Python Basics",
                "Core syntax",
                "Rationale"
        );

        TurnEvaluation evaluation = evaluationService.evaluateAnswer(
                "Explain list comprehensions in Python.",
                "Syntax and example",
                "   ",
                topic,
                null
        );

        assertNotNull(evaluation);
        assertEquals(0.0, evaluation.technicalScore());
        assertEquals(0.0, evaluation.communicationScore());
        assertTrue(evaluation.keyPointsMissed().contains("No verbal answer recorded"));
    }
}
