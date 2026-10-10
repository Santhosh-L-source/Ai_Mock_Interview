-- V3__add_proctoring_and_malpractice.sql
-- Add proctoring columns to interview_sessions table
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS proctor_warnings_count INT NOT NULL DEFAULT 0;
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS malpractice_reason TEXT;
ALTER TABLE interview_sessions ADD COLUMN IF NOT EXISTS proctor_events_json JSONB DEFAULT '[]'::jsonb;
