package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.InterviewRoadmap;
import com.ai.interviewer.domain.dto.ParsedResume;
import com.ai.interviewer.domain.dto.PlannedTopic;
import com.ai.interviewer.domain.dto.QuestionCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InterviewRoadmapService {

    private static final Logger log = LoggerFactory.getLogger(InterviewRoadmapService.class);

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public InterviewRoadmapService(ChatLanguageModel chatLanguageModel, ObjectMapper objectMapper) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper;
    }

    public InterviewRoadmap generateRoadmap(ParsedResume resume, int totalQuestions) {
        int questions = totalQuestions > 0 ? totalQuestions : 5;
        log.info("Generating {}-question student interview roadmap for candidate '{}'", questions, resume.candidateName());

        int resumeSkillCount = (int) Math.round(questions * 0.50);
        int dsaCount = (int) Math.round(questions * 0.30);
        int behavioralCount = questions - resumeSkillCount - dsaCount;
        if (behavioralCount < 1) {
            behavioralCount = 1;
            if (resumeSkillCount > 1) {
                resumeSkillCount--;
            } else if (dsaCount > 1) {
                dsaCount--;
            }
        }

        String prompt = """
            You are an expert Technical Interviewer and Campus Hiring Mentor specializing in evaluating 1st and 2nd-year college students.
            Create a custom, student-focused, structured %d-question interview roadmap for candidate '%s' based on their resume profile.

            Mandatory Student Question Distribution:
            1. RESUME_SKILL (50%% - %d questions): Focus strictly on core technical skills, programming languages, and frameworks listed directly on the candidate's resume (e.g., Python, Java, C++, React, HTML/CSS, SQL). Adapt whether they are Frontend, Backend, Mobile, Data Science, etc. Test syntax, core mechanisms, functions, and hands-on project usage.
            2. DSA_BASIC (30%% - %d questions): Fundamental Data Structures & Algorithms appropriate for 1st/2nd-year students (Arrays, Strings, HashMaps, Two Pointers, Linked Lists, Basic Recursion, Time & Space Complexity Big-O).
            3. BEHAVIORAL (20%% - %d questions): College coursework/hackathon project experience, debugging approach, problem-solving journey, or teamwork scenarios using the STAR method (Situation, Task, Action, Result).

            CRITICAL STUDENT GUIDELINES:
            - Tailor the questions to be accessible and appropriate for 1st and 2nd-year college students.
            - Do NOT ask complex enterprise system design, microservices, distributed caching, Kubernetes, or high-load resilience/fault-tolerance questions (such as Kafka, Cassandra, Circuit Breakers) unless explicitly present on the student's resume.
            - Focus on fundamental understanding, code logic, foundational data structures, and learning mindset.

            Candidate Profile:
            - Declared Skills: %s
            - Domain Areas: %s
            - Summary: %s
            - Key Projects: %s

            Return ONLY a valid JSON array of planned questions without markdown or code fences:
            [
              {
                "questionNumber": 1,
                "category": "RESUME_SKILL",
                "topic": "Python & Data Structures",
                "focusArea": "List comprehensions, dictionaries, and string manipulation in coursework projects",
                "rationale": "Verify foundational programming fluency in the student's primary declared language"
              }
            ]
            """.formatted(
                questions,
                resume.candidateName(),
                resumeSkillCount,
                dsaCount,
                behavioralCount,
                String.join(", ", resume.technicalSkills()),
                String.join(", ", resume.domainAreas()),
                resume.summary(),
                String.join("; ", resume.keyProjects())
        );

        try {
            String response = chatLanguageModel.generate(prompt);
            return parseRoadmapJson(response, resume, questions, resumeSkillCount, dsaCount, behavioralCount);
        } catch (Exception e) {
            log.error("Failed to generate roadmap with LLM, falling back to structured blueprint: {}", e.getMessage());
            return fallbackRoadmap(resume, questions, resumeSkillCount, dsaCount, behavioralCount);
        }
    }

    private InterviewRoadmap parseRoadmapJson(String rawResponse, ParsedResume resume, int totalQuestions, int resumeSkillCount, int dsaCount, int behavioralCount) {
        try {
            String cleaned = cleanJsonString(rawResponse);
            JsonNode root = objectMapper.readTree(cleaned);

            List<PlannedTopic> topics = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    int qNum = node.path("questionNumber").asInt(topics.size() + 1);
                    String catStr = node.path("category").asText("RESUME_SKILL");
                    QuestionCategory category = parseCategory(catStr, qNum, resumeSkillCount, dsaCount);
                    String topic = node.path("topic").asText("Programming Fundamentals");
                    String focusArea = node.path("focusArea").asText("Core understanding and problem solving");
                    String rationale = node.path("rationale").asText("Assess foundational engineering knowledge");

                    topics.add(new PlannedTopic(qNum, category, topic, focusArea, rationale));
                }
            }

            if (topics.isEmpty()) {
                return fallbackRoadmap(resume, totalQuestions, resumeSkillCount, dsaCount, behavioralCount);
            }

            String summary = String.format("50%% Resume Skills (%d questions), 30%% Basic DSA (%d questions), 20%% Behavioral (%d questions)",
                    resumeSkillCount, dsaCount, behavioralCount);

            return new InterviewRoadmap(resume.candidateName(), topics.size(), topics, summary);
        } catch (Exception e) {
            log.warn("Error parsing LLM roadmap response: {}, building fallback", rawResponse, e);
            return fallbackRoadmap(resume, totalQuestions, resumeSkillCount, dsaCount, behavioralCount);
        }
    }

    private QuestionCategory parseCategory(String rawCategory, int qNum, int resumeSkillCount, int dsaCount) {
        if (rawCategory != null) {
            String upper = rawCategory.toUpperCase();
            if (upper.contains("RESUME") || upper.contains("SKILL") || upper.contains("CORE")) return QuestionCategory.RESUME_SKILL;
            if (upper.contains("DSA") || upper.contains("ALGO") || upper.contains("DATA_STRUCT") || upper.contains("SYSTEM") || upper.contains("FAIL")) return QuestionCategory.DSA_BASIC;
            if (upper.contains("BEHAV") || upper.contains("STAR") || upper.contains("LEAD") || upper.contains("PROJECT")) return QuestionCategory.BEHAVIORAL;
        }
        if (qNum <= resumeSkillCount) return QuestionCategory.RESUME_SKILL;
        if (qNum <= resumeSkillCount + dsaCount) return QuestionCategory.DSA_BASIC;
        return QuestionCategory.BEHAVIORAL;
    }

    private InterviewRoadmap fallbackRoadmap(ParsedResume resume, int totalQuestions, int resumeSkillCount, int dsaCount, int behavioralCount) {
        List<PlannedTopic> list = new ArrayList<>();
        List<String> skills = resume.technicalSkills().isEmpty() ? List.of("Java", "Python", "SQL", "Git") : resume.technicalSkills();

        int qIndex = 1;
        // 50% Resume Skills
        for (int i = 0; i < resumeSkillCount; i++) {
            String skill = skills.get(i % skills.size());
            list.add(new PlannedTopic(
                    qIndex++,
                    QuestionCategory.RESUME_SKILL,
                    skill + " Fundamentals & Project Implementation",
                    "Core syntax, functions, and practical usage of " + skill,
                    "Verify hands-on foundation in declared resume skill " + skill
            ));
        }

        // 30% DSA Basic
        String[][] dsaTopics = {
                {"Arrays & String Manipulation (Two Pointers / HashMaps)", "Searching, palindrome checks, frequency counting, and Big-O complexity", "Assess basic algorithmic thinking and data structure choice"},
                {"HashMap Lookups & Key-Value Logic", "Lookup efficiency, key uniqueness, and avoiding nested loops", "Test understanding of hashing and time complexity"},
                {"Basic Recursion & Linear Data Structures", "Base conditions, recursion call stack, and list traversal", "Evaluate foundational computer science problem solving"}
        };
        for (int i = 0; i < dsaCount; i++) {
            String[] topicInfo = dsaTopics[i % dsaTopics.length];
            list.add(new PlannedTopic(
                    qIndex++,
                    QuestionCategory.DSA_BASIC,
                    topicInfo[0],
                    topicInfo[1],
                    topicInfo[2]
            ));
        }

        // 20% Behavioral STAR
        String[][] behavioralTopics = {
                {"Coursework / Hackathon Project Collaboration", "Teamwork, task allocation, resolving merge conflicts or technical disagreements", "Evaluate teamwork, communication, and adaptability using STAR"},
                {"Debugging a Challenging Problem in a Project", "Root-cause analysis, debugging tools used, and lessons learned", "Assess learning agility and systematic problem solving using STAR"}
        };
        for (int i = 0; i < behavioralCount; i++) {
            String[] topicInfo = behavioralTopics[i % behavioralTopics.length];
            list.add(new PlannedTopic(
                    qIndex++,
                    QuestionCategory.BEHAVIORAL,
                    topicInfo[0],
                    topicInfo[1],
                    topicInfo[2]
            ));
        }

        String summary = String.format("Student Roadmap (50/30/20): %d Resume Skills, %d Basic DSA, %d Behavioral questions",
                resumeSkillCount, dsaCount, behavioralCount);

        return new InterviewRoadmap(resume.candidateName(), list.size(), list, summary);
    }

    private String cleanJsonString(String raw) {
        if (raw == null) return "[]";
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
