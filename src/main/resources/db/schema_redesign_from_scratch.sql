-- ====================================================================
-- AI INTERVIEWER PLATFORM: FULL REDESIGNED DATABASE SCHEMA FROM SCRATCH
-- Includes:
-- 1. admin_users (Administrator credentials)
-- 2. candidates (Student records with roll_no eg: 24AM119)
-- 3. scheduled_interviews (Campus placement drives)
-- 4. interview_assignments (Drive assignments with 1-attempt constraint)
-- 5. interview_sessions (Live session state, proctor scores & verdicts)
-- 6. interview_turns (Turn-by-turn Q&A, transcripts, rubric scores)
-- 7. proctor_violation_logs (Tamper audit log: mobile, tab switch, etc.)
-- 8. Seeded Demo Admin & Demo Student Accounts
-- ====================================================================

-- 1. ADMIN USERS TABLE
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_users_username ON admin_users(username);
CREATE INDEX IF NOT EXISTS idx_admin_users_email ON admin_users(email);

-- 2. CANDIDATES (STUDENTS) TABLE
CREATE TABLE IF NOT EXISTS candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    roll_no VARCHAR(50) UNIQUE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    student_year VARCHAR(50) NOT NULL DEFAULT '2nd Year',
    department VARCHAR(100) NOT NULL DEFAULT 'Computer Science & Engineering',
    auth_user_id VARCHAR(255),
    temporary_password_issued BOOLEAN DEFAULT FALSE,
    must_change_password BOOLEAN DEFAULT FALSE,
    resume_text TEXT,
    skills JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_candidates_email ON candidates(email);
CREATE INDEX IF NOT EXISTS idx_candidates_roll_no ON candidates(roll_no);
CREATE INDEX IF NOT EXISTS idx_candidates_student_year ON candidates(student_year);

-- 3. SCHEDULED INTERVIEWS (CAMPUS DRIVES) TABLE
CREATE TABLE IF NOT EXISTS scheduled_interviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    target_role VARCHAR(255) NOT NULL,
    description TEXT,
    total_questions INT NOT NULL DEFAULT 5,
    duration_minutes INT NOT NULL DEFAULT 15,
    scheduled_start TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_end TIMESTAMP WITH TIME ZONE,
    target_year VARCHAR(50) NOT NULL DEFAULT 'ALL',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scheduled_interviews_status ON scheduled_interviews(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_interviews_target_year ON scheduled_interviews(target_year);

-- 4. INTERVIEW ASSIGNMENTS TABLE
CREATE TABLE IF NOT EXISTS interview_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL REFERENCES scheduled_interviews(id) ON DELETE CASCADE,
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    candidate_email VARCHAR(255) NOT NULL,
    attempt_status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_candidate_interview_assignment UNIQUE (interview_id, candidate_id),
    CONSTRAINT uq_email_interview_assignment UNIQUE (interview_id, candidate_email)
);

CREATE INDEX IF NOT EXISTS idx_interview_assignments_candidate_email ON interview_assignments(candidate_email);
CREATE INDEX IF NOT EXISTS idx_interview_assignments_interview_id ON interview_assignments(interview_id);

-- 5. INTERVIEW SESSIONS TABLE
CREATE TABLE IF NOT EXISTS interview_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    assignment_id UUID REFERENCES interview_assignments(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    total_questions INT NOT NULL DEFAULT 5,
    overall_score DOUBLE PRECISION,
    technical_score DOUBLE PRECISION,
    communication_score DOUBLE PRECISION,
    verdict VARCHAR(100),
    summary_feedback TEXT,
    proctor_warnings_count INT NOT NULL DEFAULT 0,
    is_malpractice BOOLEAN NOT NULL DEFAULT FALSE,
    malpractice_reason TEXT,
    session_token VARCHAR(255),
    proctor_events_json JSONB DEFAULT '[]'::jsonb,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_interview_sessions_candidate_id ON interview_sessions(candidate_id);
CREATE INDEX IF NOT EXISTS idx_interview_sessions_assignment_id ON interview_sessions(assignment_id);

-- Enforce at most one completed or disqualified session per interview assignment
CREATE UNIQUE INDEX IF NOT EXISTS uq_session_assignment_attempt 
ON interview_sessions(assignment_id) 
WHERE assignment_id IS NOT NULL AND status IN ('COMPLETED', 'MALPRACTICE');

-- 6. INTERVIEW TURNS TABLE
CREATE TABLE IF NOT EXISTS interview_turns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    question_number INT NOT NULL,
    question_text TEXT NOT NULL,
    candidate_answer_transcript TEXT,
    technical_score DOUBLE PRECISION,
    communication_score DOUBLE PRECISION,
    feedback_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_session_question_number UNIQUE(session_id, question_number)
);

CREATE INDEX IF NOT EXISTS idx_interview_turns_session_id ON interview_turns(session_id);

-- 7. PROCTOR VIOLATION LOGS TABLE
CREATE TABLE IF NOT EXISTS proctor_violation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    violation_type VARCHAR(100) NOT NULL,
    details TEXT,
    warning_number INT NOT NULL,
    logged_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_proctor_violation_logs_session_id ON proctor_violation_logs(session_id);

-- ====================================================================
-- SEED INITIAL DATA: DEMO ADMIN & DEMO STUDENT (ROLL NO: 24AM119)
-- ====================================================================

-- 1. Demo Administrator
-- Username: admin | Email: admin@interview.ai | Password: Admin@2026
INSERT INTO admin_users (id, username, email, password_hash, full_name, role)
VALUES (
    'a0000000-0000-0000-0000-000000000001'::uuid,
    'admin',
    'admin@interview.ai',
    'Admin@2026',
    'Chief Technical Evaluator (Admin)',
    'ADMIN'
)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role;

-- 2. Demo Student Record
-- Roll No: 24AM119 | Email: alex.morgan@testcampus.edu | Password: Candidate@2026
INSERT INTO candidates (
    id,
    roll_no,
    name,
    email,
    password_hash,
    student_year,
    department,
    skills,
    resume_text,
    temporary_password_issued,
    must_change_password
)
VALUES (
    'c0000000-0000-0000-0000-000000000001'::uuid,
    '24AM119',
    'Alex Morgan',
    'alex.morgan@testcampus.edu',
    'Candidate@2026',
    '2nd Year',
    'Artificial Intelligence & Machine Learning',
    '["Python", "Java", "React", "PostgreSQL", "Data Structures", "Git"]'::jsonb,
    '2nd-year AI & Machine Learning student (Roll No: 24AM119). Proficient in Python, Java, React, PostgreSQL, and Git. Coursework: Data Structures & Algorithms, Object-Oriented Programming, and Machine Learning Fundamentals. Built a real-time event booking portal with hash-based indexing and concurrency handling.',
    false,
    false
)
ON CONFLICT (id) DO UPDATE SET
    roll_no = EXCLUDED.roll_no,
    name = EXCLUDED.name,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    student_year = EXCLUDED.student_year,
    department = EXCLUDED.department,
    skills = EXCLUDED.skills,
    resume_text = EXCLUDED.resume_text;

-- 3. Demo Scheduled Interview Drive
INSERT INTO scheduled_interviews (
    id,
    title,
    target_role,
    description,
    total_questions,
    duration_minutes,
    target_year,
    status
)
VALUES (
    'd0000000-0000-0000-0000-000000000001'::uuid,
    'Campus Placement Technical Drive 2026',
    'Software Development Engineer (SDE Intern)',
    'Comprehensive AI-driven technical screening evaluating programming fundamentals, basic data structures & algorithms (Arrays, Strings, HashMaps, Two Pointers), and problem-solving experience.',
    5,
    15,
    'ALL',
    'ACTIVE'
)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title,
    target_role = EXCLUDED.target_role,
    description = EXCLUDED.description,
    total_questions = EXCLUDED.total_questions,
    duration_minutes = EXCLUDED.duration_minutes,
    target_year = EXCLUDED.target_year,
    status = EXCLUDED.status;

-- 4. Demo Student Assignment to Campus Drive
INSERT INTO interview_assignments (
    id,
    interview_id,
    candidate_id,
    candidate_email,
    attempt_status
)
VALUES (
    'e0000000-0000-0000-0000-000000000001'::uuid,
    'd0000000-0000-0000-0000-000000000001'::uuid,
    'c0000000-0000-0000-0000-000000000001'::uuid,
    'alex.morgan@testcampus.edu',
    'NOT_STARTED'
)
ON CONFLICT (interview_id, candidate_email) DO NOTHING;
