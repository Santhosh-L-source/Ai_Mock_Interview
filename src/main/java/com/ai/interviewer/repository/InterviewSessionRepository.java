package com.ai.interviewer.repository;

import com.ai.interviewer.domain.entity.InterviewSession;
import com.ai.interviewer.domain.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    List<InterviewSession> findByCandidateId(UUID candidateId);

    List<InterviewSession> findByStatus(SessionStatus status);

    @Query("SELECT s FROM InterviewSession s LEFT JOIN FETCH s.turns WHERE s.id = :id")
    Optional<InterviewSession> findByIdWithTurns(@Param("id") UUID id);

    @Query("SELECT s FROM InterviewSession s JOIN FETCH s.candidate c LEFT JOIN FETCH s.turns WHERE c.authUserId = :authUserId ORDER BY s.createdAt DESC")
    List<InterviewSession> findByCandidateAuthUserId(@Param("authUserId") String authUserId);

    Optional<InterviewSession> findBySessionToken(String sessionToken);

    Optional<InterviewSession> findByAssignmentId(UUID assignmentId);
}
