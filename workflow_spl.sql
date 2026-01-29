-- 현재 사용중인 계정 조회
SELECT current_user;
-- 현재 DB조회
SELECT current_database();

-- 테스트 테이블
CREATE TABLE test (
	name TEXT,
	age BIGSERIAL
);

SELECT * FROM test;


DROP TABLE test;


CREATE TABLE users (
	id BIGSERIAL PRIMARY KEY,
	email VARCHAR(255) UNIQUE NOT NULL,
	password_hash VARCHAR(255) NOT NULL,
	name VARCHAR(100) NOT NULL,
	department VARCHAR(100) NOT NULL,
	position VARCHAR(100) NOT NULL,
	role VARCHAR(20) NOT NULL,
	status VARCHAR(20) NOT NULL,
	last_login_at TIMESTAMPTZ NULL,
	created_at TIMESTAMPTZ NULL,
	updated_at TIMESTAMPTZ NULL
);

CREATE TABLE tasks (
	id BIGSERIAL PRIMARY KEY,
	title VARCHAR(200) NOT NULL,
	description TEXT NULL,
	status VARCHAR(20) NOT NULL,
	priority VARCHAR(20) NULL,
	due_date DATE NULL,
	hold_reason TEXT NULL,
	cancel_reason TEXT NULL,
	is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
	deleted_at TIMESTAMPTZ NULL,
	created_by BIGINT NOT NULL ,
	assignee_id BIGINT NULL,
	created_at TIMESTAMPTZ NOT NULL,
	updated_at TIMESTAMPTZ NOT NULL,
	FOREIGN KEY (created_by) REFERENCES users(id),
	FOREIGN KEY (assignee_id) REFERENCES users(id)
);

CREATE TABLE comments(
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    content TEXT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE attachments(
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NULL,
    size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (uploader_id) REFERENCES users(id)
);

CREATE TABLE audit_logs(
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    field_name VARCHAR(50) NULL,
    before_value TEXT NULL,
    after_value TEXT NULL,
    reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE TABLE refresh_tokens(
    id BIGSERIAL,
    user_id BIGINT NOT NULL PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (id) REFERENCES users (id)
);

-- 생성된 테이블 조회
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

