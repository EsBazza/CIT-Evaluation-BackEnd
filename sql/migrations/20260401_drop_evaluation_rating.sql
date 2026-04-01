-- Optional cleanup migration: remove legacy standalone rating column.
-- Performance is now derived from criteria metric scores.
ALTER TABLE evaluations
  DROP COLUMN IF EXISTS rating;
