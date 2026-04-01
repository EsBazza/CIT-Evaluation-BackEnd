-- Add the new columns that the application expects (role + assigned_sections).
-- The legacy department column stays in place for compatibility but is read-only.
BEGIN;

ALTER TABLE professors
  ADD COLUMN IF NOT EXISTS role TEXT,
  ADD COLUMN IF NOT EXISTS assigned_sections TEXT;

-- Copy the old department value into the new role column so existing records stay meaningful.
UPDATE professors
  SET role = department
  WHERE role IS NULL OR role = '';

COMMIT;

-- Truncate evaluation data so you can start clean for testing (including related score rows).
TRUNCATE TABLE evaluation_scores, evaluations, criteria, users, professors
  RESTART IDENTITY CASCADE;

-- NOTE: Truncating the professors table removes all faculty accounts; re-populate via the admin UI after running this script.
