package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.InterviewRoadmap;
import com.ai.interviewer.domain.dto.ParsedResume;
import com.ai.interviewer.domain.dto.PlannedTopic;
import com.ai.interviewer.domain.dto.QuestionCategory;
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

class InterviewRoadmapServiceTest {

    private ChatLanguageModel chatLanguageModel;
    private ObjectMapper objectMapper;
    private InterviewRoadmapService roadmapService;

    @BeforeEach
    void setUp() {
        chatLanguageModel = Mockito.mock(ChatLanguageModel.class);
        objectMapper = new ObjectMapper();
        roadmapService = new InterviewRoadmapService(chatLanguageModel, objectMapper);
    }

    @Test
    @DisplayName("Should generate a 5-step roadmap adhering to 50% Resume, 30% DSA, 20% Behavioral with LLM")
    void testGenerateRoadmapWithLlm() {
        String mockLlmJson = """
            [
              {
                "questionNumber": 1,
                "category": "RESUME_SKILL",
                "topic": "Python Core & Functions",
                "focusArea": "List comprehensions and file I/O",
                "rationale": "Verify basic Python programming"
              },
              {
                "questionNumber": 2,
                "category": "RESUME_SKILL",
                "topic": "React State Management",
                "focusArea": "useState and props drilling",
                "rationale": "Assess frontend basics"
              },
              {
                "questionNumber": 3,
                "category": "DSA_BASIC",
                "topic": "Two Pointers Array Search",
                "focusArea": "Two-sum sorted array with O(N) time complexity",
                "rationale": "Evaluate basic algorithmic reasoning"
              },
              {
                "questionNumber": 4,
                "category": "DSA_BASIC",
                "topic": "HashMap Frequency Counting",
                "focusArea": "Key-value counting and duplicate detection",
                "rationale": "Check hash table mechanics"
              },
              {
                "questionNumber": 5,
                "category": "BEHAVIORAL",
                "topic": "Coursework Project Teamwork",
                "focusArea": "Resolving a git merge conflict in group project",
                "rationale": "Assess teamwork with STAR method"
              }
            ]
            """;

        when(chatLanguageModel.generate(anyString())).thenReturn(mockLlmJson);

        ParsedResume studentResume = new ParsedResume(
                "Jane Doe",
                List.of("Python", "React", "SQL"),
                List.of("Web Development"),
                1,
                "2nd-year CS student interested in fullstack",
                List.of("College Club Portal")
        );

        InterviewRoadmap roadmap = roadmapService.generateRoadmap(studentResume, 5);

        assertNotNull(roadmap);
        assertEquals(5, roadmap.totalQuestions());
        assertEquals("Jane Doe", roadmap.candidateName());
        assertEquals(5, roadmap.plan().size());

        long resumeCount = roadmap.plan().stream().filter(p -> p.category() == QuestionCategory.RESUME_SKILL).count();
        long dsaCount = roadmap.plan().stream().filter(p -> p.category() == QuestionCategory.DSA_BASIC).count();
        long behavioralCount = roadmap.plan().stream().filter(p -> p.category() == QuestionCategory.BEHAVIORAL).count();

        assertEquals(2, resumeCount);
        assertEquals(2, dsaCount);
        assertEquals(1, behavioralCount);
    }

    @Test
    @DisplayName("Should use fallback student blueprint when LLM is unreachable")
    void testFallbackRoadmap() {
        when(chatLanguageModel.generate(anyString())).thenThrow(new RuntimeException("LLM offline"));

        ParsedResume studentResume = new ParsedResume(
                "John Student",
                List.of("Java", "MySQL"),
                List.of("Backend Basics"),
                1,
                "1st-year computer science student",
                List.of("Student Management System")
        );

        InterviewRoadmap roadmap = roadmapService.generateRoadmap(studentResume, 5);

        assertNotNull(roadmap);
        assertEquals(5, roadmap.totalQuestions());
        assertFalse(roadmap.plan().isEmpty());

        for (PlannedTopic step : roadmap.plan()) {
            assertNotNull(step.category());
            assertNotNull(step.topic());
            assertNotNull(step.focusArea());
            assertNotNull(step.rationale());
        }
    }
}
