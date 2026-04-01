-- Prevent duplicate submissions for the same student/faculty/section combination.
-- This complements the service-level 409 guard in EvaluationService.
--
-- Existing duplicates must be cleaned first or index creation fails with 23505.
-- Keep the newest row (highest id) per duplicate key and delete older rows.
-- Delete child score rows first to avoid FK violations.

WITH duplicate_eval_ids AS (
	SELECT id
	FROM (
		SELECT
			id,
			ROW_NUMBER() OVER (
				PARTITION BY student_number, faculty_email, section
				ORDER BY id DESC
			) AS rn
		FROM evaluations
	) ranked
	WHERE rn > 1
)
DELETE FROM evaluation_scores s
USING duplicate_eval_ids d
WHERE s.evaluation_id = d.id;

WITH duplicate_eval_ids AS (
	SELECT id
	FROM (
		SELECT
			id,
			ROW_NUMBER() OVER (
				PARTITION BY student_number, faculty_email, section
				ORDER BY id DESC
			) AS rn
		FROM evaluations
	) ranked
	WHERE rn > 1
)
DELETE FROM evaluations e
USING duplicate_eval_ids d
WHERE e.id = d.id;

CREATE UNIQUE INDEX IF NOT EXISTS ux_evaluations_student_faculty_section
ON evaluations (student_number, faculty_email, section);
