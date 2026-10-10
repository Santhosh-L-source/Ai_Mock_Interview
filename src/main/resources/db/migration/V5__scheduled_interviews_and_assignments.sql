-- V5__scheduled_interviews_and_assignments.sql
-- Schema migration for Admin Scheduled Interviews, Candidate Assignments, Attendance & Malpractice Audit Logs

-- 1. Scheduled Interviews table (Admin creates these)
CREATE TABLE IF NOT EXISTS scheduled_interviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    target_role VARCHAR(255) NOT NULL,
    description TEXT,
    total_questions INT NOT NULL DEFAULT 7,
    duration_minutes INT NOT NULL DEFAULT 15,
    scheduled_start TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_end TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ARCHIVED, CLOSED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Interview Assignments table (Assigns candidate to a specific scheduled interview)
CREATE TABLE IF NOT EXISTS interview_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL REFERENCES scheduled_interviews(id) ON DELETE CASCADE,
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    candidate_email VARCHAR(255) NOT NULL,
    attempt_status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED', -- NOT_STARTED, IN_PROGRESS, COMPLETED, DISQUALIFIED
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_candidate_interview_assignment UNIQUE (interview_id, candidate_id),
    CONSTRAINT uq_email_interview_assignment UNIQUE (interview_id, candidate_email)
);

-- 3. Enhance candidates table with password management flags
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS temporary_password_issued BOOLEAN DEFAULT FALSE;
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT FALSE;

-- 4. Enhance interview_sessions table with assignment and persisted scores
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS assignment_id UUID REFERENCES interview_assignments(id) ON DELETE SET NULL;
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS overall_score DOUBLE PRECISION;
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS technical_score DOUBLE PRECISION;
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS communication_score DOUBLE PRECISION;
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS verdict VARCHAR(100);
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS summary_feedback TEXT;
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS is_malpractice BOOLEAN DEFAULT FALSE;
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS session_token VARCHAR(255);

-- 5. Proctor Violation Audit Table (For granular red-flag reporting)
CREATE TABLE IF NOT EXISTS proctor_violation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    violation_type VARCHAR(100) NOT NULL,
    details TEXT,
    warning_number INT NOT NULL,
    logged_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Indexes for query performance and single-attempt lookups
CREATE INDEX IF NOT EXISTS idx_scheduled_interviews_status ON scheduled_interviews(status);
CREATE INDEX IF NOT EXISTS idx_interview_assignments_candidate_email ON interview_assignments(candidate_email);
CREATE INDEX IF NOT EXISTS idx_interview_assignments_interview_id ON interview_assignments(interview_id);
CREATE INDEX IF NOT EXISTS idx_interview_sessions_assignment_id ON interview_sessions(assignment_id);
CREATE INDEX IF NOT EXISTS idx_proctor_violation_logs_session_id ON proctor_violation_logs(session_id);

-- 7. Ensure at most one completed or malpractice session per assignment at the database level
CREATE UNIQUE INDEX IF NOT EXISTS uq_session_assignment_attempt 
ON interview_sessions(assignment_id) 
WHERE assignment_id IS NOT NULL AND status IN ('COMPLETED', 'MALPRACTICE');
