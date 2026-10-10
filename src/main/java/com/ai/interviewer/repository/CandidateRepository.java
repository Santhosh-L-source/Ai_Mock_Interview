package com.ai.interviewer.repository;

import com.ai.interviewer.domain.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {
    Optional<Candidate> findByAuthUserId(String authUserId);
    Optional<Candidate> findByEmail(String email);
    Optional<Candidate> findByRollNo(String rollNo);
    java.util.List<Candidate> findByStudentYear(String studentYear);
    java.util.List<Candidate> findAllByOrderByCreatedAtDesc();
}
