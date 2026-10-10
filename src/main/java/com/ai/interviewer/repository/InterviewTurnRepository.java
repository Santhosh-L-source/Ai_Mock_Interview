package com.ai.interviewer.repository;

import com.ai.interviewer.domain.entity.InterviewTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewTurnRepository extends JpaRepository<InterviewTurn, UUID> {

    List<InterviewTurn> findBySessionIdOrderByQuestionNumberAsc(UUID sessionId);

    Optional<InterviewTurn> findBySessionIdAndQuestionNumber(UUID sessionId, Integer questionNumber);
}
