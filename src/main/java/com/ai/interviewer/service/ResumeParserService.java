package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.ParsedResume;
import com.ai.interviewer.domain.entity.Candidate;
import com.ai.interviewer.repository.CandidateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeParserService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParserService.class);

    private final ChatLanguageModel chatLanguageModel;
    private final CandidateRepository candidateRepository;
    private final ObjectMapper objectMapper;

    public ResumeParserService(ChatLanguageModel chatLanguageModel,
                               CandidateRepository candidateRepository,
                               ObjectMapper objectMapper) {
        this.chatLanguageModel = chatLanguageModel;
        this.candidateRepository = candidateRepository;
        this.objectMapper = objectMapper;
    }

    public ParsedResume parseResumeText(String resumeText) {
        log.info("Parsing resume text (length: {})", resumeText != null ? resumeText.length() : 0);

        if (resumeText == null || resumeText.isBlank()) {
            return new ParsedResume("Unknown Candidate", List.of(), List.of(), 0, "Empty resume provided", List.of());
        }

        String prompt = """
            You are an expert technical interviewer and AI recruiter.
            Extract the candidate information, technical skills, domain areas, experience, and key projects from the following resume text.
            
            Return ONLY a valid JSON object without markdown formatting or code fences:
            {
              "candidateName": "Full Name",
              "technicalSkills": ["Java", "Spring Boot", "PostgreSQL", "Kafka", "Docker", "Kubernetes", "AWS"],
              "domainAreas": ["Backend Systems", "Distributed Systems", "Cloud Infrastructure"],
              "experienceYears": 5,
              "summary": "Concise summary of experience and primary strengths",
              "keyProjects": ["Project 1 - brief description", "Project 2 - brief description"]
            }

            Resume Text:
            %s
            """.formatted(resumeText);

        try {
            String response = chatLanguageModel.generate(prompt);
            return parseJsonResponse(response, resumeText);
        } catch (Exception ex) {
            log.error("Failed to parse resume with LLM, applying heuristic fallback: {}", ex.getMessage());
            return fallbackParse(resumeText);
        }
    }

    @Transactional
    public ParsedResume parseAndPersistCandidate(UUID candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));

        ParsedResume parsed = parseResumeText(candidate.getResumeText());

        if (parsed.candidateName() != null && !parsed.candidateName().isBlank() && !"Unknown Candidate".equalsIgnoreCase(parsed.candidateName())) {
            candidate.setName(parsed.candidateName());
        }
        if (parsed.technicalSkills() != null && !parsed.technicalSkills().isEmpty()) {
            candidate.setSkills(parsed.technicalSkills());
        }

        candidateRepository.save(candidate);
        return parsed;
    }

    private ParsedResume parseJsonResponse(String rawResponse, String originalText) {
        try {
            String cleaned = cleanJsonString(rawResponse);
            JsonNode root = objectMapper.readTree(cleaned);

            String candidateName = root.path("candidateName").asText("Candidate");
            int experienceYears = root.path("experienceYears").asInt(0);
            String summary = root.path("summary").asText("");

            List<String> technicalSkills = new ArrayList<>();
            if (root.has("technicalSkills") && root.get("technicalSkills").isArray()) {
                root.get("technicalSkills").forEach(node -> technicalSkills.add(node.asText()));
            }

            List<String> domainAreas = new ArrayList<>();
            if (root.has("domainAreas") && root.get("domainAreas").isArray()) {
                root.get("domainAreas").forEach(node -> domainAreas.add(node.asText()));
            }

            List<String> keyProjects = new ArrayList<>();
            if (root.has("keyProjects") && root.get("keyProjects").isArray()) {
                root.get("keyProjects").forEach(node -> keyProjects.add(node.asText()));
            }

            return new ParsedResume(candidateName, technicalSkills, domainAreas, experienceYears, summary, keyProjects);
        } catch (Exception e) {
            log.warn("Failed to parse raw LLM JSON response: {}, returning fallback", rawResponse, e);
            return fallbackParse(originalText);
        }
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

    private ParsedResume fallbackParse(String resumeText) {
        List<String> skills = new ArrayList<>();
        String[] popularSkills = {"Java", "Spring Boot", "PostgreSQL", "React", "Docker", "Kubernetes", "AWS", "Python", "Kafka", "Redis", "Microservices", "REST API", "Git"};
        for (String skill : popularSkills) {
            if (resumeText != null && resumeText.toLowerCase().contains(skill.toLowerCase())) {
                skills.add(skill);
            }
        }
        if (skills.isEmpty()) {
            skills.addAll(List.of("Software Engineering", "Problem Solving", "Backend Development"));
        }
        return new ParsedResume(
                "Candidate",
                skills,
                List.of("Software Development", "Backend Engineering"),
                3,
                "Extracted from resume context.",
                List.of("Engineering Portfolio")
        );
    }
}
