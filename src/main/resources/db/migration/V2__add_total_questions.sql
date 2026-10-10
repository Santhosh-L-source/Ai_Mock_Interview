-- V2__add_total_questions.sql
-- Add total_questions to interview_sessions table
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS total_questions INT NOT NULL DEFAULT 5;
