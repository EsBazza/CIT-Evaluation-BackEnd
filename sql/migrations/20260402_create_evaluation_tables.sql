-- Bootstrap tables required by the evaluation flow.
-- Apply this on any fresh or reset database before starting the backend.

CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE IF NOT EXISTS evaluations (
	id BIGSERIAL PRIMARY KEY,
	faculty_email VARCHAR(255) NOT NULL,
	section VARCHAR(100) NOT NULL,
	student_number VARCHAR(100) NOT NULL,
	student_email CITEXT NOT NULL,
	ciphertext TEXT,
	student_public_key TEXT,
	iv VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS evaluation_scores (
	id BIGSERIAL PRIMARY KEY,
	score INTEGER NOT NULL,
	criterion_id BIGINT NOT NULL,
	evaluation_id BIGINT NOT NULL,
	CONSTRAINT fk_evaluation_scores_criterion
		FOREIGN KEY (criterion_id)
		REFERENCES criteria (id)
		ON DELETE CASCADE,
	CONSTRAINT fk_evaluation_scores_evaluation
		FOREIGN KEY (evaluation_id)
		REFERENCES evaluations (id)
		ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_evaluations_student_faculty_section
ON evaluations (student_number, faculty_email, section);