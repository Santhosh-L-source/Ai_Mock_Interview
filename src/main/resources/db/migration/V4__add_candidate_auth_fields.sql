-- V4__add_candidate_auth_fields.sql
-- Add email and auth_user_id columns to candidates table for Supabase Auth integration
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS auth_user_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_candidates_auth_user_id ON candidates(auth_user_id);
CREATE INDEX IF NOT EXISTS idx_candidates_email ON candidates(email);
