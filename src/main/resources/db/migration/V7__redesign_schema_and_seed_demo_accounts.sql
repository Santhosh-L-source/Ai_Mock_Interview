-- V7__redesign_schema_and_seed_demo_accounts.sql
-- Database Redesign: Adds admin_users, guarantees roll_no, and seeds demo Admin and Student accounts

-- 1. Create admin_users table
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

-- 2. Ensure candidates table has roll_no and credential columns
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS roll_no VARCHAR(50);
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS student_year VARCHAR(50) DEFAULT '2nd Year';
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS department VARCHAR(100) DEFAULT 'Computer Science & Engineering';
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS temporary_password_issued BOOLEAN DEFAULT FALSE;
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_candidates_roll_no ON candidates(roll_no);
CREATE INDEX IF NOT EXISTS idx_candidates_student_year ON candidates(student_year);

-- 3. Seed Demo Administrator Account
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

-- 4. Seed Demo Student Account
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

-- 5. Seed Demo Scheduled Interview Drive
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

-- 6. Assign Demo Student to Demo Interview Drive
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
