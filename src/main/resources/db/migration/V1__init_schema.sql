CREATE EXTENSION IF NOT EXISTS citext;

DO $$
BEGIN
	CREATE TYPE app_role AS ENUM ('ADMIN', 'STUDENT', 'FACULTY', 'GUEST');
EXCEPTION
	WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS users (
	id BIGSERIAL PRIMARY KEY,
	email CITEXT NOT NULL UNIQUE,
	name VARCHAR(255),
	oauth_provider VARCHAR(30),
	oauth_subject VARCHAR(128),
	role app_role NOT NULL,
	enabled BOOLEAN NOT NULL DEFAULT TRUE,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS professors (
	id BIGSERIAL PRIMARY KEY,
	name VARCHAR(255) NOT NULL,
	email CITEXT NOT NULL UNIQUE,
	role VARCHAR(255),
	department VARCHAR(255),
	assigned_sections TEXT,
	is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS criteria (
	id BIGSERIAL PRIMARY KEY,
	title VARCHAR(255) NOT NULL
);

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