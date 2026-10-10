-- ====================================================================================================
-- AI INTERVIEWER PLATFORM: ALL-IN-ONE MASTER DATABASE SCRIPT
-- ====================================================================================================
-- This single file contains:
--   PART 1: Schema Teardown & Reset (DROP TABLE IF EXISTS ... CASCADE)
--   PART 2: Schema DDL Definitions (All 7 tables with PK, FK, Constraints, Defaults)
--   PART 3: Performance Indexes & Unique Constraints
--   PART 4: Pre-Seeded Demo Data (Admin, Student Roll No: 24AM119, Campus Drive, Assignment)
--   PART 5: All Application Queries (Auth, Student Management, Interview Execution, Proctoring, Reports)
-- ====================================================================================================

-- ----------------------------------------------------------------------------------------------------
-- PART 1: SCHEMA TEARDOWN & RESET (SAFE: Uncomment if you want to wipe & recreate from scratch)
-- ----------------------------------------------------------------------------------------------------
-- DROP TABLE IF EXISTS proctor_violation_logs CASCADE;
-- DROP TABLE IF EXISTS interview_turns CASCADE;
-- DROP TABLE IF EXISTS interview_sessions CASCADE;
-- DROP TABLE IF EXISTS interview_assignments CASCADE;
-- DROP TABLE IF EXISTS scheduled_interviews CASCADE;
-- DROP TABLE IF EXISTS candidates CASCADE;
-- DROP TABLE IF EXISTS admin_users CASCADE;

-- Ensure UUID generator extension is active
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- ----------------------------------------------------------------------------------------------------
-- PART 2: TABLE DEFINITIONS (DDL)
-- ----------------------------------------------------------------------------------------------------

-- 1. ADMIN USERS TABLE
-- Stores portal administrators and evaluators with username and password
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. CANDIDATES (STUDENTS) TABLE
-- Stores student profiles, institutional roll numbers (e.g. 24AM119), year, department, and credentials
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

-- 3. SCHEDULED INTERVIEWS (CAMPUS DRIVES) TABLE
-- Campus recruitment drives configured by administrators (target year, question count, duration)
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

-- 4. INTERVIEW ASSIGNMENTS TABLE
-- Links a candidate to a scheduled interview drive and tracks attempt status
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

-- 5. INTERVIEW SESSIONS TABLE
-- Live test sessions, AI rubric scores, final verdict, proctoring summary, and malpractice flags
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

-- 6. INTERVIEW TURNS TABLE
-- Turn-by-turn question text, candidate speech transcript, AI evaluation score, and rubric feedback
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

-- 7. PROCTOR VIOLATION LOGS TABLE
-- Detailed tamper/violation audit log (e.g., TAB_SWITCH, MOBILE_DEVICE, MULTIPLE_FACES, FACE_ABSENT)
CREATE TABLE IF NOT EXISTS proctor_violation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    violation_type VARCHAR(100) NOT NULL,
    details TEXT,
    warning_number INT NOT NULL,
    logged_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ----------------------------------------------------------------------------------------------------
-- PART 3: INDEXES & PERFORMANCE OPTIMIZATIONS
-- ----------------------------------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_admin_users_username ON admin_users(username);
CREATE INDEX IF NOT EXISTS idx_admin_users_email ON admin_users(email);

CREATE INDEX IF NOT EXISTS idx_candidates_roll_no ON candidates(roll_no);
CREATE INDEX IF NOT EXISTS idx_candidates_email ON candidates(email);
CREATE INDEX IF NOT EXISTS idx_candidates_student_year ON candidates(student_year);

CREATE INDEX IF NOT EXISTS idx_scheduled_interviews_status ON scheduled_interviews(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_interviews_target_year ON scheduled_interviews(target_year);

CREATE INDEX IF NOT EXISTS idx_interview_assignments_candidate_email ON interview_assignments(candidate_email);
CREATE INDEX IF NOT EXISTS idx_interview_assignments_interview_id ON interview_assignments(interview_id);

CREATE INDEX IF NOT EXISTS idx_interview_sessions_candidate_id ON interview_sessions(candidate_id);
CREATE INDEX IF NOT EXISTS idx_interview_sessions_assignment_id ON interview_sessions(assignment_id);

CREATE INDEX IF NOT EXISTS idx_interview_turns_session_id ON interview_turns(session_id);
CREATE INDEX IF NOT EXISTS idx_proctor_violation_logs_session_id ON proctor_violation_logs(session_id);

-- Enforce at most one completed or disqualified session per interview assignment
CREATE UNIQUE INDEX IF NOT EXISTS uq_session_assignment_attempt 
ON interview_sessions(assignment_id) 
WHERE assignment_id IS NOT NULL AND status IN ('COMPLETED', 'MALPRACTICE');


-- ----------------------------------------------------------------------------------------------------
-- PART 3.1: ROW LEVEL SECURITY (RLS) POLICIES
-- Enables Supabase Row Level Security to protect tables from unauthorized PostgREST/anon key exposure.
-- Spring Boot connects directly over JDBC using the database owner/superuser role and bypasses RLS.
-- ----------------------------------------------------------------------------------------------------
ALTER TABLE admin_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE candidates ENABLE ROW LEVEL SECURITY;
ALTER TABLE scheduled_interviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE interview_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE interview_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE interview_turns ENABLE ROW LEVEL SECURITY;
ALTER TABLE proctor_violation_logs ENABLE ROW LEVEL SECURITY;

-- Grants full access to the Supabase service_role key and direct DB owner
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'service_role_admin_users') THEN
        CREATE POLICY "service_role_admin_users" ON admin_users FOR ALL TO service_role USING (true);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'service_role_candidates') THEN
        CREATE POLICY "service_role_candidates" ON candidates FOR ALL TO service_role USING (true);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'service_role_interviews') THEN
        CREATE POLICY "service_role_interviews" ON scheduled_interviews FOR ALL TO service_role USING (true);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'service_role_assignments') THEN
        CREATE POLICY "service_role_assignments" ON interview_assignments FOR ALL TO service_role USING (true);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'service_role_sessions') THEN
        CREATE POLICY "service_role_sessions" ON interview_sessions FOR ALL TO service_role USING (true);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'service_role_turns') THEN
        CREATE POLICY "service_role_turns" ON interview_turns FOR ALL TO service_role USING (true);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'service_role_proctor_logs') THEN
        CREATE POLICY "service_role_proctor_logs" ON proctor_violation_logs FOR ALL TO service_role USING (true);
    END IF;
END $$;


-- ----------------------------------------------------------------------------------------------------
-- PART 4: PRE-SEEDED DEMO ACCOUNTS & INITIAL DATA
-- ----------------------------------------------------------------------------------------------------

-- 1. Demo Administrator Account
-- Username: admin | Email: admin@interview.ai | Password: Admin@2026
INSERT INTO admin_users (
    id,
    username,
    email,
    password_hash,
    full_name,
    role
)
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

-- 2. Demo Student Account
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

-- 4. Assign Demo Student to Demo Campus Drive
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

-- 5. Seed Demo Interview Session
INSERT INTO interview_sessions (
    id,
    candidate_id,
    assignment_id,
    status,
    total_questions,
    overall_score,
    technical_score,
    communication_score,
    verdict,
    summary_feedback,
    started_at
)
VALUES (
    'f0000000-0000-0000-0000-000000000001'::uuid,
    'c0000000-0000-0000-0000-000000000001'::uuid,
    'e0000000-0000-0000-0000-000000000001'::uuid,
    'IN_PROGRESS',
    5,
    8.2,
    8.5,
    8.0,
    'RECOMMENDED',
    'Candidate demonstrated solid core programming understanding, algorithms intuition, and clear verbal communication.',
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    status = EXCLUDED.status;


-- ----------------------------------------------------------------------------------------------------
-- PART 5: ALL APPLICATION QUERIES (CRUD, AUTHENTICATION, INTERVIEWS, REPORTS)
-- ----------------------------------------------------------------------------------------------------

-- ====================================================================
-- 5.1 AUTHENTICATION QUERIES
-- ====================================================================

-- 5.1.1 Admin Login (By username or email)
-- Used when administrator enters username and password
SELECT id, username, email, full_name, role, password_hash
FROM admin_users
WHERE (LOWER(username) = LOWER('admin') OR LOWER(email) = LOWER('admin'))
  AND password_hash = 'Admin@2026';

-- 5.1.2 Student Login (By Roll Number or Email)
-- Used when student enters Roll No (e.g. 24AM119) or email and password
SELECT id, roll_no, name, email, student_year, department, password_hash
FROM candidates
WHERE (LOWER(roll_no) = LOWER('24AM119') OR LOWER(email) = LOWER('24AM119'))
  AND password_hash = 'Candidate@2026';

-- 5.1.3 Change Student Password
UPDATE candidates
SET password_hash = 'Candidate@2026',
    must_change_password = false
WHERE id = 'c0000000-0000-0000-0000-000000000001'::uuid;


-- ====================================================================
-- 5.2 STUDENT MANAGEMENT QUERIES (ADMIN PORTAL)
-- ====================================================================

-- 5.2.1 Create New Student with Roll Number
INSERT INTO candidates (
    id,
    roll_no,
    name,
    email,
    password_hash,
    student_year,
    department,
    temporary_password_issued,
    must_change_password
)
VALUES (
    'c0000000-0000-0000-0000-000000000002'::uuid,
    '24CS042',
    'Jordan Smith',
    'jordan.smith@testcampus.edu',
    'Candidate@2026',
    '3rd Year',
    'Computer Science & Engineering',
    false,
    false
)
ON CONFLICT (id) DO UPDATE SET
    roll_no = EXCLUDED.roll_no,
    name = EXCLUDED.name,
    email = EXCLUDED.email
RETURNING id, roll_no, name, email, student_year, department;

-- 5.2.2 Fetch All Students (Ordered by Roll Number)
SELECT id, roll_no, name, email, student_year, department, created_at
FROM candidates
ORDER BY roll_no ASC NULLS LAST;

-- 5.2.3 Find Student by Roll Number
SELECT id, roll_no, name, email, student_year, department, resume_text, skills
FROM candidates
WHERE roll_no = '24AM119';

-- 5.2.4 Find Students by Year
SELECT id, roll_no, name, email, department
FROM candidates
WHERE student_year = '2nd Year';


-- ====================================================================
-- 5.3 SCHEDULED INTERVIEWS & ASSIGNMENTS QUERIES
-- ====================================================================

-- 5.3.1 Create a Campus Interview Drive
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
    'd0000000-0000-0000-0000-000000000002'::uuid,
    'Final Year TCS/Infosys Mock Drive',
    'Associate Software Engineer',
    'Core Java, SQL, and problem solving assessment',
    5,
    15,
    '4th Year',
    'ACTIVE'
)
ON CONFLICT (id) DO UPDATE SET
    title = EXCLUDED.title
RETURNING id, title, target_role, status;

-- 5.3.2 List All Scheduled Drives
SELECT id, title, target_role, total_questions, duration_minutes, target_year, status, created_at
FROM scheduled_interviews
ORDER BY created_at DESC;

-- 5.3.3 Assign Candidate to an Interview Drive
INSERT INTO interview_assignments (
    interview_id,
    candidate_id,
    candidate_email,
    attempt_status
)
VALUES (
    'd0000000-0000-0000-0000-000000000001'::uuid,
    'c0000000-0000-0000-0000-000000000001'::uuid,
    'alex.morgan@testcampus.edu',
    'NOT_STARTED'
)
ON CONFLICT (interview_id, candidate_email) DO NOTHING;

-- 5.3.4 Get Pending Interviews for a Student
SELECT 
    si.id AS interview_id,
    si.title,
    si.target_role,
    si.duration_minutes,
    si.total_questions,
    ia.id AS assignment_id,
    ia.attempt_status
FROM interview_assignments ia
JOIN scheduled_interviews si ON ia.interview_id = si.id
WHERE ia.candidate_email = 'alex.morgan@testcampus.edu'
  AND ia.attempt_status = 'NOT_STARTED'
  AND si.status = 'ACTIVE';


-- ====================================================================
-- 5.4 LIVE INTERVIEW SESSION & TURN EXECUTION
-- ====================================================================

-- 5.4.1 Start Interview Session
INSERT INTO interview_sessions (
    id,
    candidate_id,
    assignment_id,
    status,
    total_questions,
    started_at
)
VALUES (
    'f0000000-0000-0000-0000-000000000001'::uuid,
    'c0000000-0000-0000-0000-000000000001'::uuid,
    'e0000000-0000-0000-0000-000000000001'::uuid,
    'IN_PROGRESS',
    5,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO UPDATE SET
    status = EXCLUDED.status
RETURNING id, status, started_at;

-- 5.4.2 Mark Assignment as IN_PROGRESS
UPDATE interview_assignments
SET attempt_status = 'IN_PROGRESS',
    attempted_at = CURRENT_TIMESTAMP
WHERE id = 'e0000000-0000-0000-0000-000000000001'::uuid;

-- 5.4.3 Save Interview Turn (Question + Candidate Speech Transcript + AI Rubric Score)
INSERT INTO interview_turns (
    session_id,
    question_number,
    question_text,
    candidate_answer_transcript,
    technical_score,
    communication_score,
    feedback_json
)
VALUES (
    'f0000000-0000-0000-0000-000000000001'::uuid,
    1,
    'Explain how HashMaps handle collisions and the average vs worst-case time complexity.',
    'HashMaps use buckets with linked lists or trees when keys hash to the same bucket index. Average lookup is O(1), and worst case is O(n) or O(log n).',
    8.5,
    9.0,
    '{"strengths": "Accurate explanation of separate chaining and treeify threshold", "improvements": "Could mention load factor resize cost"}'::jsonb
)
ON CONFLICT (session_id, question_number) DO UPDATE SET
    candidate_answer_transcript = EXCLUDED.candidate_answer_transcript,
    technical_score = EXCLUDED.technical_score,
    communication_score = EXCLUDED.communication_score,
    feedback_json = EXCLUDED.feedback_json;

-- 5.4.4 Complete Interview Session & Set Final Marks / Verdict
UPDATE interview_sessions
SET status = 'COMPLETED',
    overall_score = 8.2,
    technical_score = 8.5,
    communication_score = 8.0,
    verdict = 'RECOMMENDED',
    summary_feedback = 'Strong core computer science fundamentals and clear articulation.',
    ended_at = CURRENT_TIMESTAMP
WHERE id = 'f0000000-0000-0000-0000-000000000001'::uuid;

-- 5.4.5 Mark Assignment Completed (Locks student from re-attempting)
UPDATE interview_assignments
SET attempt_status = 'COMPLETED'
WHERE id = 'e0000000-0000-0000-0000-000000000001'::uuid;


-- ====================================================================
-- 5.5 PROCTORING, MALPRACTICE & AUDIT LOGS
-- ====================================================================

-- 5.5.1 Log Proctor Violation Event
INSERT INTO proctor_violation_logs (
    id,
    session_id,
    violation_type,
    details,
    warning_number
)
VALUES (
    '90000000-0000-0000-0000-000000000001'::uuid,
    'f0000000-0000-0000-0000-000000000001'::uuid,
    'TAB_SWITCH',
    'Candidate switched browser tabs for 4 seconds during Question #2',
    1
)
ON CONFLICT (id) DO NOTHING;

-- 5.5.2 Increment Proctor Warnings Count in Session
UPDATE interview_sessions
SET proctor_warnings_count = proctor_warnings_count + 1
WHERE id = 'f0000000-0000-0000-0000-000000000001'::uuid;

-- 5.5.3 Disqualify Candidate for Malpractice (E.g. Mobile phone or > 3 violations)
UPDATE interview_sessions
SET status = 'MALPRACTICE',
    is_malpractice = TRUE,
    malpractice_reason = 'Disqualified: Mobile device detected and multiple window switches.',
    ended_at = CURRENT_TIMESTAMP
WHERE id = 'f0000000-0000-0000-0000-000000000001'::uuid;

-- 5.5.4 Mark Assignment Disqualified
UPDATE interview_assignments
SET attempt_status = 'DISQUALIFIED'
WHERE id = 'e0000000-0000-0000-0000-000000000001'::uuid;


-- ====================================================================
-- 5.6 ATTENDANCE, MARKS & SCORECARD REPORTS (ADMIN DASHBOARD)
-- ====================================================================

-- 5.6.1 Drive Attendance & Marks Report (Includes Student Roll Number)
SELECT 
    ia.id AS assignment_id,
    c.roll_no AS candidate_roll_no,
    c.name AS candidate_name,
    c.email AS candidate_email,
    c.student_year,
    c.department,
    ia.attempt_status,
    ia.assigned_at,
    ia.attempted_at,
    s.id AS session_id,
    s.overall_score,
    s.technical_score,
    s.communication_score,
    s.verdict,
    s.proctor_warnings_count,
    s.is_malpractice,
    s.malpractice_reason
FROM interview_assignments ia
JOIN candidates c ON ia.candidate_id = c.id
LEFT JOIN interview_sessions s ON s.assignment_id = ia.id
WHERE ia.interview_id = 'd0000000-0000-0000-0000-000000000001'::uuid
ORDER BY c.roll_no ASC NULLS LAST, c.name ASC;

-- 5.6.2 Fetch Candidate Scorecard with Turn-by-Turn History
SELECT 
    s.id AS session_id,
    c.roll_no,
    c.name AS candidate_name,
    c.email,
    s.overall_score,
    s.technical_score,
    s.communication_score,
    s.verdict,
    s.summary_feedback,
    t.question_number,
    t.question_text,
    t.candidate_answer_transcript,
    t.technical_score AS turn_tech_score,
    t.communication_score AS turn_comm_score,
    t.feedback_json
FROM interview_sessions s
JOIN candidates c ON s.candidate_id = c.id
LEFT JOIN interview_turns t ON t.session_id = s.id
WHERE s.id = 'f0000000-0000-0000-0000-000000000001'::uuid
ORDER BY t.question_number ASC;

-- 5.6.3 Fetch Proctor Violation Audit for a Session
SELECT 
    id,
    violation_type,
    details,
    warning_number,
    logged_at
FROM proctor_violation_logs
WHERE session_id = 'f0000000-0000-0000-0000-000000000001'::uuid
ORDER BY warning_number ASC;
