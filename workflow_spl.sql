-- 현재 사용중인 계정 조회
SELECT current_user;
-- 현재 DB조회
SELECT current_database();

SELECT * FROM department;
SELECT * FROM users;
SELECT * FROM tasks;
SELECT * FROM comments;
SELECT * FROM attachments;
SELECT * FROM audit_logs;
SELECT * FROM refresh_tokens;


DROP TABLE refresh_tokens;
DROP TABLE audit_logs;
DROP TABLE attachments;
DROP TABLE comments;
DROP TABLE tasks;
DROP TABLE users;
DROP TABLE department;


CREATE TABLE department (
	id BIGSERIAL PRIMARY KEY,
	name VARCHAR(100) UNIQUE NOT NULL,
	code VARCHAR(20) UNIQUE NOT NULL,
	created_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
	updated_at timestamp(6) without time zone NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
	id BIGSERIAL PRIMARY KEY,
	email VARCHAR(255) UNIQUE NOT NULL,
	password_hash VARCHAR(255) NOT NULL,
	name VARCHAR(100) NOT NULL,
	department_id BIGINT NOT NULL,
	position VARCHAR(100) NOT NULL,
	role VARCHAR(20) NOT NULL,
	status VARCHAR(20) NOT NULL,
	last_login_at timestamp(6) without time zone NULL,
	created_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
	updated_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
	FOREIGN KEY (department_id) REFERENCES department(id)
);

CREATE INDEX idx_users_department_id ON users(department_id);

CREATE TABLE tasks (
	id BIGSERIAL PRIMARY KEY,
	title VARCHAR(200) NOT NULL,
	description TEXT NULL,
	status VARCHAR(20) NOT NULL,
	priority VARCHAR(20) NOT NULL,
	visibility VARCHAR(20) NOT NULL DEFAULT 'DEPARTMENT',
	due_date DATE NULL,
	hold_reason TEXT NULL,
	cancel_reason TEXT NULL,
	is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
	deleted_at TIMESTAMPTZ NULL,
	created_by BIGINT NOT NULL,
	assignee_id BIGINT NULL,
  	owner_department_id BIGINT NOT NULL, -- 작성자 부서
  	work_department_id BIGINT NOT NULL, -- 처리/ 담당 부서
	created_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
	updated_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
	FOREIGN KEY (created_by) REFERENCES users(id),
	FOREIGN KEY (assignee_id) REFERENCES users(id),
	FOREIGN KEY (owner_department_id) REFERENCES department(id),
	FOREIGN KEY (work_department_id) REFERENCES department(id),
	CHECK (visibility IN (
            'PUBLIC',
            'DEPARTMENT',
            'PRIVATE'
        )),
	CHECK (status IN (
            'TODO',
            'IN_PROGRESS',
            'REVIEW',
            'DONE',
            'ON_HOLD',
            'CANCELED'
        ))	
);

CREATE INDEX idx_tasks_created_by ON tasks(created_by);
CREATE INDEX idx_tasks_assignee_id ON tasks(assignee_id);
CREATE INDEX idx_tasks_owner_dept ON tasks(owner_department_id);
CREATE INDEX idx_tasks_work_dept ON tasks(work_department_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_visibility ON tasks(visibility);

CREATE TABLE comments(
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    content TEXT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at timestamp(6) without time zone NULL,
    created_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
    updated_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
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
    deleted_at timestamp(6) without time zone NULL,
    created_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
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
    created_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE TABLE refresh_tokens(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at timestamp(6) without time zone NOT NULL,
    revoked_at timestamp(6) without time zone NULL,
    created_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users (id)
);


--DB 값 추가
-- 부서 3개
-- (개발)OPS: 1, (개발)DEV: 2, (디자인)DSG: 3
INSERT INTO department (
    name,
	code,
    created_at,
    updated_at
)
VALUES (
    'Operations',
	'OPS',
    now(),
    now()
);

INSERT INTO department (
    name,
	code,
    created_at,
    updated_at
)
VALUES (
    'Development',
	'DEV',
    now(),
    now()
);

INSERT INTO department (
    name,
	code,
    created_at,
    updated_at
)
VALUES (
    'Design',
	'DSG',
    now(),
    now()
);


-- 계정 3개
INSERT INTO users (
    email,
    password_hash,
    name,
    department_id,
    position,
    role,
    status,
    last_login_at,
    created_at,
    updated_at
)
VALUES (
    'admin@workflow.com',
    '$2a$10$t8zVyp9gVqPhTdT0XG4RIOdg9PbLRuHvj8GMjcwUqt7jyvdO.D4HK',
    '김워크',
    1,
    '관리자',
    'ADMIN',
    'OFFLINE',
    now(),
    now(),
    now()
);

INSERT INTO users (
    email,
    password_hash,
    name,
    department_id,
    position,
    role,
    status,
    last_login_at,
    created_at,
    updated_at
)
VALUES (
    'song@workflow.com',
    '$2a$10$t8zVyp9gVqPhTdT0XG4RIOdg9PbLRuHvj8GMjcwUqt7jyvdO.D4HK',
    '송현오',
    2,
    '부장',
    'MANAGER',
    'OFFLINE',
    now(),
    now(),
    now()
);

INSERT INTO users (
    email,
    password_hash,
    name,
    department_id,
    position,
    role,
    status,
    last_login_at,
    created_at,
    updated_at
)
VALUES (
    'lim@workflow.com',
    '$2a$10$t8zVyp9gVqPhTdT0XG4RIOdg9PbLRuHvj8GMjcwUqt7jyvdO.D4HK',
    '임현규',
    2, 
    '주임',
    'USER',
    'OFFLINE',
    now(),
    now(),
    now()
);

INSERT INTO users (
    email,
    password_hash,
    name,
    department_id,
    position,
    role,
    status,
    last_login_at,
    created_at,
    updated_at
)
VALUES (
    'dlim@workflow.com',
    '$2a$10$t8zVyp9gVqPhTdT0XG4RIOdg9PbLRuHvj8GMjcwUqt7jyvdO.D4HK',
    '임현오',
    3,  
    '부장',
    'MANAGER',
    'OFFLINE',
    now(),
    now(),
    now()
);

INSERT INTO users (
    email,
    password_hash,
    name,
    department_id,
    position,
    role,
    status,
    last_login_at,
    created_at,
    updated_at
)
VALUES (
    'dsong@workflow.com',
    '$2a$10$t8zVyp9gVqPhTdT0XG4RIOdg9PbLRuHvj8GMjcwUqt7jyvdO.D4HK',
    '송현규',
    3, 
    '주임',
    'USER',
    'OFFLINE',
    now(),
    now(),
    now()
);


-- 생성된 테이블 조회
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

