package com.ai.interviewer.repository;

import com.ai.interviewer.domain.entity.AssignmentStatus;
import com.ai.interviewer.domain.entity.InterviewAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewAssignmentRepository extends JpaRepository<InterviewAssignment, UUID> {

    @Query("SELECT a FROM InterviewAssignment a JOIN FETCH a.scheduledInterview i LEFT JOIN FETCH a.session s WHERE a.candidateEmail = :email ORDER BY a.assignedAt DESC")
    List<InterviewAssignment> findByCandidateEmailWithDetails(@Param("email") String email);

    Optional<InterviewAssignment> findByScheduledInterviewIdAndCandidateEmail(UUID interviewId, String candidateEmail);

    Optional<InterviewAssignment> findByScheduledInterviewIdAndCandidateId(UUID interviewId, UUID candidateId);

    @Query("SELECT a FROM InterviewAssignment a JOIN FETCH a.candidate c LEFT JOIN FETCH a.session s LEFT JOIN FETCH s.turns WHERE a.scheduledInterview.id = :interviewId ORDER BY a.assignedAt DESC")
    List<InterviewAssignment> findByScheduledInterviewIdWithCandidateAndSession(@Param("interviewId") UUID interviewId);

    boolean existsByScheduledInterviewIdAndCandidateEmail(UUID interviewId, String candidateEmail);

    long countByScheduledInterviewId(UUID interviewId);

    long countByScheduledInterviewIdAndAttemptStatus(UUID interviewId, AssignmentStatus status);
}
