package com.ai.interviewer.repository;

import com.ai.interviewer.domain.entity.ScheduledInterview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledInterviewRepository extends JpaRepository<ScheduledInterview, UUID> {

    @Query("SELECT s FROM ScheduledInterview s ORDER BY s.createdAt DESC")
    List<ScheduledInterview> findAllOrderByCreatedAtDesc();

    List<ScheduledInterview> findByStatusOrderByCreatedAtDesc(String status);
}
