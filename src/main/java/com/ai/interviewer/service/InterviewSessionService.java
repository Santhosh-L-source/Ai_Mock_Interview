package com.ai.interviewer.service;

import com.ai.interviewer.domain.dto.*;
import com.ai.interviewer.domain.entity.*;
import com.ai.interviewer.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class InterviewSessionService {

    private static final Logger log = LoggerFactory.getLogger(InterviewSessionService.class);

    private final InterviewSessionRepository sessionRepository;
    private final InterviewTurnRepository turnRepository;
    private final CandidateRepository candidateRepository;
    private final InterviewRoadmapService roadmapService;
    private final QuestionGeneratorService questionGeneratorService;
    private final AnswerEvaluationService evaluationService;
    private final LiveKitTokenService liveKitTokenService;
    private final ObjectMapper objectMapper;

    private final InterviewAssignmentRepository assignmentRepository;
    private final ProctorViolationLogRepository violationLogRepository;

    public InterviewSessionService(
            InterviewSessionRepository sessionRepository,
            InterviewTurnRepository turnRepository,
            CandidateRepository candidateRepository,
            InterviewRoadmapService roadmapService,
            QuestionGeneratorService questionGeneratorService,
            AnswerEvaluationService evaluationService,
            LiveKitTokenService liveKitTokenService,
            ObjectMapper objectMapper,
            InterviewAssignmentRepository assignmentRepository,
            ProctorViolationLogRepository violationLogRepository) {
        this.sessionRepository = sessionRepository;
        this.turnRepository = turnRepository;
        this.candidateRepository = candidateRepository;
        this.roadmapService = roadmapService;
        this.questionGeneratorService = questionGeneratorService;
        this.evaluationService = evaluationService;
        this.liveKitTokenService = liveKitTokenService;
        this.objectMapper = objectMapper;
        this.assignmentRepository = assignmentRepository;
        this.violationLogRepository = violationLogRepository;
    }

    @Transactional
    public SessionResponse startSession(UUID candidateId, Integer totalQuestions) {
        return startSession(candidateId, totalQuestions, null, null);
    }

    @Transactional
    public SessionResponse startSession(UUID candidateId, Integer totalQuestions, UUID assignmentId, String sessionToken) {
        log.info("Starting interview session for candidateId: {}, assignmentId: {}", candidateId, assignmentId);

        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found with id: " + candidateId));

        InterviewAssignment assignment = null;
        if (assignmentId != null) {
            assignment = assignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Interview assignment not found: " + assignmentId));

            // Strict single-attempt verification
            if (assignment.getAttemptStatus() == AssignmentStatus.COMPLETED) {
                throw new IllegalStateException("You have already completed this scheduled interview. Retakes are strictly prohibited.");
            }
            if (assignment.getAttemptStatus() == AssignmentStatus.DISQUALIFIED) {
                throw new IllegalStateException("This interview was terminated due to a proctoring violation. Retakes are not permitted.");
            }

            // Network resilience: if already in progress and session exists, resume
            if (assignment.getAttemptStatus() == AssignmentStatus.IN_PROGRESS && assignment.getSession() != null) {
                InterviewSession existing = assignment.getSession();
                log.info("Network resilience: Reconnecting candidate to existing session: {}", existing.getId());

                List<InterviewTurn> existingTurns = turnRepository.findBySessionIdOrderByQuestionNumberAsc(existing.getId());
                InterviewTurn currentTurn = existingTurns.stream()
                        .filter(t -> t.getCandidateAnswerTranscript() == null || t.getCandidateAnswerTranscript().isBlank())
                        .findFirst()
                        .orElse(existingTurns.isEmpty() ? null : existingTurns.get(existingTurns.size() - 1));

                GeneratedQuestionResponse activeQ = currentTurn != null
                        ? new GeneratedQuestionResponse(currentTurn.getQuestionNumber(), QuestionCategory.RESUME_SKILL, "Current Topic", currentTurn.getQuestionText(), "Evaluation in progress", "Concept application")
                        : null;

                LiveKitTokenResponse livekit = liveKitTokenService.generateToken(existing.getId(), candidate.getId().toString(), candidate.getName());
                return new SessionResponse(
                        existing.getId(),
                        candidate.getId(),
                        candidate.getName(),
                        existing.getStatus(),
                        existing.getStartedAt(),
                        null,
                        activeQ,
                        livekit
                );
            }
        }

        int questions = totalQuestions != null && totalQuestions > 0 ? totalQuestions : 5;
        if (assignment != null && assignment.getScheduledInterview() != null && assignment.getScheduledInterview().getTotalQuestions() != null) {
            questions = assignment.getScheduledInterview().getTotalQuestions();
        }

        // 1. Reconstruct ParsedResume
        ParsedResume resume = buildParsedResume(candidate);

        // 2. Generate Student-focused 50/30/20 Roadmap
        InterviewRoadmap roadmap = roadmapService.generateRoadmap(resume, questions);

        // 3. Create Session Entity
        InterviewSession session = new InterviewSession(candidate, questions);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now());
        session.setSessionToken(sessionToken != null && !sessionToken.isBlank() ? sessionToken : UUID.randomUUID().toString());

        if (assignment != null) {
            session.setAssignment(assignment);
            assignment.setAttemptStatus(AssignmentStatus.IN_PROGRESS);
            assignment.setAttemptedAt(Instant.now());
        }

        InterviewSession savedSession = sessionRepository.save(session);
        if (assignment != null) {
            assignment.setSession(savedSession);
            assignmentRepository.save(assignment);
        }

        // 4. Pre-generate all questions for the roadmap and save into DB
        GeneratedQuestionResponse firstQuestion = null;
        for (PlannedTopic topic : roadmap.plan()) {
            GeneratedQuestionResponse genQ = questionGeneratorService.generateNextQuestion(
                    topic.questionNumber(),
                    topic,
                    resume,
                    List.of()
            );
            if (topic.questionNumber() == 1) {
                firstQuestion = genQ;
            }
            InterviewTurn turn = new InterviewTurn(savedSession, topic.questionNumber(), genQ.questionText());
            turnRepository.save(turn);
        }

        if (firstQuestion == null) {
            firstQuestion = new GeneratedQuestionResponse(
                    1,
                    QuestionCategory.RESUME_SKILL,
                    "Core Programming Skills",
                    "Could you briefly explain how you use your primary programming language in your projects?",
                    "Baseline student assessment",
                    "Core syntax and basic logic"
            );
            InterviewTurn turn1 = new InterviewTurn(savedSession, 1, firstQuestion.questionText());
            turnRepository.save(turn1);
        }

        // 5. LiveKit WebRTC Token
        LiveKitTokenResponse livekit = liveKitTokenService.generateToken(
                savedSession.getId(),
                candidate.getId().toString(),
                candidate.getName()
        );

        return new SessionResponse(
                savedSession.getId(),
                candidate.getId(),
                candidate.getName(),
                savedSession.getStatus(),
                savedSession.getStartedAt(),
                roadmap,
                firstQuestion,
                livekit
        );
    }

    @Transactional
    public TurnResponse submitAnswer(UUID sessionId, int questionNumber, String candidateTranscript) {
        log.info("Submitting answer for Session: {}, Question #{}", sessionId, questionNumber);

        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        Candidate candidate = session.getCandidate();
        ParsedResume resume = buildParsedResume(candidate);

        // 1. Fetch current Turn and save candidate transcript immediately
        InterviewTurn currentTurn = turnRepository.findBySessionIdAndQuestionNumber(sessionId, questionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Turn #" + questionNumber + " not found for session " + sessionId));

        currentTurn.setCandidateAnswerTranscript(candidateTranscript);
        turnRepository.save(currentTurn);

        int totalQuestions = session.getTotalQuestions() != null && session.getTotalQuestions() > 0
                ? session.getTotalQuestions()
                : 5;

        // 2. Resolve topic metadata without slow LLM calls
        PlannedTopic currentTopic = new PlannedTopic(
                questionNumber,
                QuestionCategory.RESUME_SKILL,
                "Question #" + questionNumber,
                currentTurn.getQuestionText(),
                "Evaluation"
        );

        // 3. Dispatch Asynchronous Turn Evaluation in the background
        final UUID turnId = currentTurn.getId();
        final String qText = currentTurn.getQuestionText();
        evaluationService.evaluateAnswerAsync(
                qText,
                "Core algorithmic correctness, clean explanation, and time complexity",
                candidateTranscript,
                currentTopic,
                resume
        ).thenAccept(evaluation -> {
            try {
                turnRepository.findById(turnId).ifPresent(turn -> {
                    turn.setTechnicalScore(evaluation.technicalScore());
                    turn.setCommunicationScore(evaluation.communicationScore());
                    Map<String, Object> feedbackMap = objectMapper.convertValue(evaluation, new TypeReference<>() {});
                    turn.setFeedbackJson(feedbackMap);
                    turnRepository.save(turn);
                    log.info("Background async evaluation saved for Turn #{}", turn.getQuestionNumber());
                });
            } catch (Exception e) {
                log.error("Failed to save async turn evaluation for turn {}: {}", turnId, e.getMessage());
            }
        });

        // 4. Return Next Pre-Generated Question INSTANTLY from the database (< 5ms)
        int nextQuestionNumber = questionNumber + 1;
        if (nextQuestionNumber <= totalQuestions) {
            Optional<InterviewTurn> nextTurnOpt = turnRepository.findBySessionIdAndQuestionNumber(sessionId, nextQuestionNumber);

            GeneratedQuestionResponse nextQuestion;
            if (nextTurnOpt.isPresent()) {
                InterviewTurn nextTurn = nextTurnOpt.get();
                nextQuestion = new GeneratedQuestionResponse(
                        nextQuestionNumber,
                        QuestionCategory.RESUME_SKILL,
                        "Question #" + nextQuestionNumber,
                        nextTurn.getQuestionText(),
                        "Pre-generated question",
                        "Fundamental concepts and clear explanation"
                );
            } else {
                nextQuestion = new GeneratedQuestionResponse(
                        nextQuestionNumber,
                        QuestionCategory.RESUME_SKILL,
                        "Question #" + nextQuestionNumber,
                        "Could you explain your approach and core concepts for this topic?",
                        "Standard question",
                        "Clear explanation"
                );
                InterviewTurn newTurn = new InterviewTurn(session, nextQuestionNumber, nextQuestion.questionText());
                turnRepository.save(newTurn);
            }

            return new TurnResponse(
                    currentTurn.getId(),
                    questionNumber,
                    currentTurn.getQuestionText(),
                    candidateTranscript,
                    null,
                    nextQuestion,
                    false
            );
        } else {
            // Completed all questions in the roadmap
            session.setStatus(SessionStatus.COMPLETED);
            session.setEndedAt(Instant.now());
            sessionRepository.save(session);

            return new TurnResponse(
                    currentTurn.getId(),
                    questionNumber,
                    currentTurn.getQuestionText(),
                    candidateTranscript,
                    null,
                    null,
                    true
            );
        }
    }

    @Transactional
    public FinalScorecardResponse completeSession(UUID sessionId) {
        log.info("Completing interview session: {}", sessionId);

        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        session.setStatus(SessionStatus.COMPLETED);
        if (session.getEndedAt() == null) {
            session.setEndedAt(Instant.now());
        }
        sessionRepository.save(session);

        // Wait up to 6 seconds for any pending background evaluations to complete
        long deadline = System.currentTimeMillis() + 6000;
        while (System.currentTimeMillis() < deadline) {
            List<InterviewTurn> currentTurns = turnRepository.findBySessionIdOrderByQuestionNumberAsc(sessionId);
            boolean hasPending = currentTurns.stream().anyMatch(t ->
                    t.getCandidateAnswerTranscript() != null &&
                    !t.getCandidateAnswerTranscript().isBlank() &&
                    t.getTechnicalScore() == null
            );
            if (!hasPending) {
                break;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {}
        }

        List<InterviewTurn> turns = turnRepository.findBySessionIdOrderByQuestionNumberAsc(sessionId);

        double totalTech = 0;
        double totalComm = 0;
        int evaluatedCount = 0;
        List<TurnSummaryRecord> summaries = new ArrayList<>();

        for (InterviewTurn turn : turns) {
            // If still un-evaluated after deadline, provide baseline fallback
            if (turn.getTechnicalScore() == null && turn.getCandidateAnswerTranscript() != null && !turn.getCandidateAnswerTranscript().isBlank()) {
                int wordCount = turn.getCandidateAnswerTranscript().split("\\s+").length;
                turn.setTechnicalScore(wordCount > 15 ? 7.0 : 5.0);
                turn.setCommunicationScore(wordCount > 15 ? 7.5 : 5.5);
                turnRepository.save(turn);
            }

            if (turn.getTechnicalScore() != null && turn.getCommunicationScore() != null) {
                totalTech += turn.getTechnicalScore();
                totalComm += turn.getCommunicationScore();
                evaluatedCount++;
            }

            String summaryText = "Evaluated";
            if (turn.getFeedbackJson() != null && turn.getFeedbackJson().containsKey("feedbackSummary")) {
                summaryText = String.valueOf(turn.getFeedbackJson().get("feedbackSummary"));
            }

            summaries.add(new TurnSummaryRecord(
                    turn.getQuestionNumber(),
                    turn.getQuestionText(),
                    turn.getCandidateAnswerTranscript(),
                    turn.getTechnicalScore(),
                    turn.getCommunicationScore(),
                    summaryText
            ));
        }

        double techAvg = evaluatedCount > 0 ? Math.round((totalTech / evaluatedCount) * 10.0) / 10.0 : 0.0;
        double commAvg = evaluatedCount > 0 ? Math.round((totalComm / evaluatedCount) * 10.0) / 10.0 : 0.0;
        double overallScore = Math.round(((techAvg * 0.6) + (commAvg * 0.4)) * 10.0) / 10.0;

        String verdict;
        String detailedFeedback;

        if (overallScore >= 8.0) {
            verdict = "STRONG HIRE / ADVANCE TO NEXT ROUND";
            detailedFeedback = "The candidate demonstrated excellent technical mastery and clear, structured communication across resume skills, fundamental algorithms, and project storytelling.";
        } else if (overallScore >= 6.5) {
            verdict = "HIRE / SOLID FOUNDATION";
            detailedFeedback = "The candidate showed good conceptual understanding and steady problem solving. Deepening algorithmic optimization and edge-case handling will elevate their performance.";
        } else {
            verdict = "NEEDS IMPROVEMENT";
            detailedFeedback = "The candidate has basic familiarity but struggled with detailed implementations and algorithmic complexity. More hands-on practice with coding and structured communication is recommended.";
        }

        // Persist computed marks directly on InterviewSession entity for admin reporting
        session.setOverallScore(overallScore);
        session.setTechnicalScore(techAvg);
        session.setCommunicationScore(commAvg);
        session.setVerdict(verdict);
        session.setSummaryFeedback(detailedFeedback);

        if (session.getAssignment() != null) {
            InterviewAssignment assignment = session.getAssignment();
            assignment.setAttemptStatus(AssignmentStatus.COMPLETED);
            assignment.setAttemptedAt(session.getEndedAt() != null ? session.getEndedAt() : Instant.now());
            assignmentRepository.save(assignment);
        }

        sessionRepository.save(session);

        return new FinalScorecardResponse(
                sessionId,
                session.getCandidate().getName(),
                overallScore,
                techAvg,
                commAvg,
                verdict,
                detailedFeedback,
                summaries
        );
    }

    @Transactional(readOnly = true)
    public InterviewSessionRecord getSession(UUID sessionId) {
        InterviewSession session = sessionRepository.findByIdWithTurns(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        return InterviewSessionRecord.fromEntity(session);
    }

    @Transactional
    public ProctorViolationResponse recordProctorViolation(UUID sessionId, ProctorViolationRequest request) {
        log.warn("Recording proctor violation for session {}: type={}, warningNumber={}, isMalpractice={}",
                sessionId, request.violationType(), request.warningNumber(), request.isMalpractice());

        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        int newWarnings = session.getProctorWarningsCount() + 1;
        session.setProctorWarningsCount(newWarnings);

        Map<String, Object> event = new HashMap<>();
        event.put("violationType", request.violationType() != null ? request.violationType() : "UNKNOWN_VIOLATION");
        event.put("details", request.details() != null ? request.details() : "Proctoring anomaly detected");
        event.put("warningNumber", newWarnings);
        event.put("timestamp", request.timestamp() != null ? request.timestamp() : Instant.now().toString());

        session.addProctorEvent(event);

        // Save detailed audit record in proctor_violation_logs table
        try {
            ProctorViolationLog logEntry = new ProctorViolationLog(
                    session,
                    request.violationType() != null ? request.violationType() : "UNKNOWN_VIOLATION",
                    request.details(),
                    newWarnings
            );
            violationLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Could not persist ProctorViolationLog: {}", e.getMessage());
        }

        boolean isDisqualified = Boolean.TRUE.equals(request.isMalpractice()) || newWarnings >= 3;

        if (isDisqualified) {
            session.setStatus(SessionStatus.MALPRACTICE);
            session.setIsMalpractice(true);
            session.setEndedAt(Instant.now());
            String reason = request.details() != null && !request.details().isBlank()
                    ? request.details()
                    : "Multiple proctoring violations (" + request.violationType() + ")";
            session.setMalpracticeReason(reason);
            log.error("Session {} DISQUALIFIED for MALPRACTICE. Reason: {}", sessionId, reason);

            if (session.getAssignment() != null) {
                InterviewAssignment assignment = session.getAssignment();
                assignment.setAttemptStatus(AssignmentStatus.DISQUALIFIED);
                assignment.setAttemptedAt(Instant.now());
                assignmentRepository.save(assignment);
            }
        }

        sessionRepository.save(session);

        String message = isDisqualified
                ? "Session terminated due to proctoring violations and malpractice."
                : "Warning " + newWarnings + " recorded.";

        return new ProctorViolationResponse(
                sessionId,
                newWarnings,
                session.getStatus(),
                isDisqualified,
                message
        );
    }

    private ParsedResume buildParsedResume(Candidate candidate) {
        return new ParsedResume(
                candidate.getName(),
                candidate.getSkills() != null ? candidate.getSkills() : List.of("General Software Engineering"),
                List.of("Software Engineering"),
                1,
                "Candidate profile for " + candidate.getName(),
                List.of("Academic & Coursework Projects")
        );
    }
}
