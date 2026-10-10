-- V6__student_year_and_permanent_credentials.sql
-- Schema migration for student year, department, permanent credentials and scheduled interview target year

ALTER TABLE candidates ADD COLUMN IF NOT EXISTS student_year VARCHAR(50) DEFAULT '3rd Year';
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS department VARCHAR(100) DEFAULT 'Computer Science & Engineering';
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

ALTER TABLE scheduled_interviews ADD COLUMN IF NOT EXISTS target_year VARCHAR(50) DEFAULT 'ALL';

CREATE INDEX IF NOT EXISTS idx_candidates_student_year ON candidates(student_year);
CREATE INDEX IF NOT EXISTS idx_scheduled_interviews_target_year ON scheduled_interviews(target_year);
