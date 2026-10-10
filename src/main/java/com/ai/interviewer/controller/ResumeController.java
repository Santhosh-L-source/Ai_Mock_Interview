package com.ai.interviewer.controller;

import com.ai.interviewer.domain.dto.*;
import com.ai.interviewer.domain.entity.Candidate;
import com.ai.interviewer.repository.CandidateRepository;
import com.ai.interviewer.service.InterviewRoadmapService;
import com.ai.interviewer.service.QuestionGeneratorService;
import com.ai.interviewer.service.ResumeParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    private final ResumeParserService resumeParserService;
    private final InterviewRoadmapService roadmapService;
    private final QuestionGeneratorService questionGeneratorService;
    private final CandidateRepository candidateRepository;

    public ResumeController(ResumeParserService resumeParserService,
                            InterviewRoadmapService roadmapService,
                            QuestionGeneratorService questionGeneratorService,
                            CandidateRepository candidateRepository) {
        this.resumeParserService = resumeParserService;
        this.roadmapService = roadmapService;
        this.questionGeneratorService = questionGeneratorService;
        this.candidateRepository = candidateRepository;
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ParsedResume> uploadAndParseResume(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        log.info("Received resume file upload: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        try {
            String extractedText;
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

            if (filename.endsWith(".pdf")) {
                try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(file.getBytes())) {
                    org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                    extractedText = stripper.getText(document);
                }
            } else {
                extractedText = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }

            if (extractedText == null || extractedText.isBlank()) {
                extractedText = "Candidate resume file uploaded: " + file.getOriginalFilename();
            }

            ParsedResume parsed = resumeParserService.parseResumeText(extractedText);
            return ResponseEntity.ok(parsed);
        } catch (Exception e) {
            log.error("Failed to parse uploaded resume file: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/parse")
    public ResponseEntity<ParsedResume> parseResume(@RequestBody ParseResumeRequest request) {
        log.info("Received request to parse resume");
        ParsedResume parsed = resumeParserService.parseResumeText(request.resumeText());
        return ResponseEntity.ok(parsed);
    }

    @PostMapping("/candidates")
    public ResponseEntity<CandidateRecord> createCandidateWithResume(@RequestBody ParseResumeRequest request) {
        log.info("Creating candidate and parsing resume for name: {}, email: {}", request.candidateName(), request.email());
        ParsedResume parsed = resumeParserService.parseResumeText(request.resumeText());

        String name = (request.candidateName() != null && !request.candidateName().isBlank())
                ? request.candidateName()
                : (parsed.candidateName() != null ? parsed.candidateName() : "Unnamed Candidate");

        Candidate candidate = null;
        if (request.email() != null && !request.email().isBlank()) {
            candidate = candidateRepository.findByEmail(request.email().trim().toLowerCase()).orElse(null);
        }
        if (candidate == null && request.authUserId() != null && !request.authUserId().isBlank()) {
            candidate = candidateRepository.findByAuthUserId(request.authUserId()).orElse(null);
        }

        if (candidate != null) {
            candidate.setName(name);
            candidate.setResumeText(request.resumeText());
            if (parsed.technicalSkills() != null && !parsed.technicalSkills().isEmpty()) {
                candidate.setSkills(parsed.technicalSkills());
            }
            if (request.authUserId() != null && !request.authUserId().isBlank()) {
                candidate.setAuthUserId(request.authUserId());
            }
            if (request.email() != null && !request.email().isBlank()) {
                candidate.setEmail(request.email().trim().toLowerCase());
            }
        } else {
            candidate = new Candidate(name, request.email(), request.authUserId(), request.resumeText(), parsed.technicalSkills());
        }

        Candidate saved = candidateRepository.save(candidate);
        return ResponseEntity.ok(CandidateRecord.fromEntity(saved));
    }

    @GetMapping("/candidates/{id}")
    public ResponseEntity<CandidateRecord> getCandidate(@PathVariable UUID id) {
        return candidateRepository.findById(id)
                .map(CandidateRecord::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/candidates/by-auth/{authUserId}")
    public ResponseEntity<CandidateRecord> getCandidateByAuth(@PathVariable String authUserId) {
        return candidateRepository.findByAuthUserId(authUserId)
                .map(CandidateRecord::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/roadmap")
    public ResponseEntity<InterviewRoadmap> generateRoadmap(@RequestBody GenerateRoadmapRequest request) {
        ParsedResume resume = request.resume();

        if (resume == null && request.candidateId() != null) {
            Candidate candidate = candidateRepository.findById(request.candidateId())
                    .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + request.candidateId()));
            resume = new ParsedResume(
                    candidate.getName(),
                    candidate.getSkills(),
                    List.of("Backend Engineering", "Distributed Systems"),
                    3,
                    "Candidate profile for " + candidate.getName(),
                    List.of("Production Services")
            );
        }

        if (resume == null) {
            resume = new ParsedResume(
                    "Standard Candidate",
                    List.of("Java", "Spring Boot", "PostgreSQL", "Kafka", "Docker"),
                    List.of("Backend Systems"),
                    4,
                    "Software Engineer with backend focus",
                    List.of("Microservices Platform")
            );
        }

        int total = request.totalQuestions() != null ? request.totalQuestions() : 10;
        InterviewRoadmap roadmap = roadmapService.generateRoadmap(resume, total);
        return ResponseEntity.ok(roadmap);
    }

    @PostMapping("/question/next")
    public ResponseEntity<GeneratedQuestionResponse> generateNextQuestion(@RequestBody NextQuestionTestRequest request) {
        ParsedResume resume = request.resume();
        if (resume == null && request.candidateId() != null) {
            Candidate candidate = candidateRepository.findById(request.candidateId())
                    .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + request.candidateId()));
            resume = new ParsedResume(
                    candidate.getName(),
                    candidate.getSkills(),
                    List.of("Backend Engineering"),
                    3,
                    candidate.getName() + " profile",
                    List.of("Engineering Projects")
            );
        }

        if (resume == null) {
            resume = new ParsedResume(
                    "Candidate",
                    List.of("Java", "Spring Boot", "PostgreSQL"),
                    List.of("Backend"),
                    3,
                    "Backend Developer",
                    List.of("Payment Service")
            );
        }

        PlannedTopic topic = request.plannedTopic();
        if (topic == null) {
            topic = new PlannedTopic(
                    request.questionNumber() > 0 ? request.questionNumber() : 1,
                    QuestionCategory.RESUME_SKILL,
                    "Programming Fundamentals & Problem Solving",
                    "Core syntax, data types, and functions implementation",
                    "Assess student core programming foundational knowledge"
            );
        }

        GeneratedQuestionResponse response = questionGeneratorService.generateNextQuestion(
                topic.questionNumber(),
                topic,
                resume,
                request.previousTurns()
        );

        return ResponseEntity.ok(response);
    }
}
