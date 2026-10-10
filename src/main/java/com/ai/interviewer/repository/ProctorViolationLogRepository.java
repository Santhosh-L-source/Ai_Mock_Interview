package com.ai.interviewer.repository;

import com.ai.interviewer.domain.entity.ProctorViolationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProctorViolationLogRepository extends JpaRepository<ProctorViolationLog, UUID> {

    List<ProctorViolationLog> findBySessionIdOrderByLoggedAtAsc(UUID sessionId);
}
