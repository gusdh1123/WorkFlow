-- 현재 사용중인 계정 조회
SELECT current_user;
-- 현재 DB조회
SELECT current_database();

-- 테이블 조회
SELECT * FROM department;
SELECT * FROM users;
SELECT * FROM tasks ORDER by id ASC;
SELECT * FROM comments;
SELECT * FROM attachments ORDER by id ASC;
SELECT * FROM audit_logs;
SELECT * FROM refresh_tokens;
SELECT * FROM favorites;
SELECT * FROM notifications;

-- 테이블 삭제
DROP TABLE notifications CASCADE;
DROP TABLE favorites CASCADE;
DROP TABLE refresh_tokens CASCADE;
DROP TABLE audit_logs CASCADE;
DROP TABLE attachments CASCADE;
DROP TABLE comments CASCADE;
DROP TABLE tasks CASCADE;
DROP TABLE users CASCADE;
DROP TABLE department CASCADE;

-- 테이블 및 인덱스, 제약조건 생성
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
	is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
	deleted_at TIMESTAMPTZ NULL,
	created_by BIGINT NOT NULL,
	assignee_id BIGINT NULL,
  	owner_department_id BIGINT NOT NULL, -- 작성자 부서
  	work_department_id BIGINT NOT NULL, -- 처리/ 담당 부서
	version BIGINT NOT NULL DEFAULT 0, -- 낙관적 락 컬럼 추가
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
    task_id BIGINT NULL,
    actor_id BIGINT NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    field_name VARCHAR(50) NULL,
    before_value TEXT NULL,
    after_value TEXT NULL,
    reason TEXT NULL,
    created_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE INDEX idx_audit_task_id ON audit_logs(task_id);

CREATE TABLE refresh_tokens(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at timestamp(6) without time zone NOT NULL,
    revoked_at timestamp(6) without time zone NULL,
    created_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    created_at timestamp(6) without time zone NOT NULL DEFAULT NOW(),
	UNIQUE (user_id, task_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
	FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_task_id ON favorites(task_id);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NULL,
    type VARCHAR(20) NOT NULL,              -- COMMENT, TASK_ASSIGN 등
    reference_id BIGINT NULL,               -- task_id, comment_id 등
    reference_type VARCHAR(20) NOT NULL,    -- TASK, COMMENT 등
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 사용자별 전체 알림 조회
CREATE INDEX idx_notifications_user_id ON notifications(user_id);

-- 사용자별 읽지 않은 알림 조회
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);


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


-- 계정 5개
-- 관리자 1, 개발팀 2, 디자인팀 2
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


-- 더미 데이터 20개
INSERT INTO tasks (
    title,
    description,
    status,
    priority,
    visibility,
    due_date,
    created_by,
    assignee_id,
    owner_department_id,
    work_department_id,
    created_at,
    updated_at
) VALUES

-- ADMIN → DEV
('인증 서버 리팩토링',
 'JWT 구조 개선 및 인증 로직 정리',
 'IN_PROGRESS','HIGH','PRIVATE','2026-06-22',
 1,3,1,2,now(),now()),

('메인 API 응답 속도 개선',
 '쿼리 최적화 및 캐싱 적용',
 'TODO','HIGH','DEPARTMENT','2026-06-28',
 1,2,1,2,now(),now()),

-- ADMIN → DSG
('대시보드 UI 개선',
 '카드 레이아웃 및 색상 개선',
 'IN_PROGRESS','MEDIUM','PUBLIC','2026-06-24',
 1,4,1,3,now(),now()),

('알림 아이콘 디자인 수정',
 '아이콘 가시성 개선 및 통일성 유지',
 'TODO','LOW','PUBLIC','2026-06-29',
 1,5,1,3,now(),now()),

-- DEV 내부 (MANAGER → USER)
('로그인 실패 처리',
 '예외 메시지 및 로깅 강화',
 'REVIEW','HIGH','PRIVATE','2026-06-21',
 2,3,2,2,now(),now()),

('검색 기능 리팩토링',
 '검색 쿼리 개선 및 성능 향상',
 'IN_PROGRESS','MEDIUM','DEPARTMENT','2026-06-26',
 2,3,2,2,now(),now()),

-- DEV 내부 (USER → MANAGER)
('배포 자동화 스크립트 작성',
 'CI/CD 스크립트 구성',
 'TODO','HIGH','DEPARTMENT','2026-06-30',
 3,2,2,2,now(),now()),

('테스트 코드 보강',
 '단위 테스트 및 통합 테스트 추가',
 'IN_PROGRESS','MEDIUM','DEPARTMENT','2026-06-25',
 3,2,2,2,now(),now()),

-- DSG 내부
('프로필 UI 개선',
 '프로필 화면 레이아웃 수정',
 'IN_PROGRESS','MEDIUM','PUBLIC','2026-06-23',
 4,5,3,3,now(),now()),

('버튼 스타일 가이드 정리',
 '공통 버튼 스타일 정의',
 'DONE','LOW','PUBLIC','2026-06-18',
 4,5,3,3,now(),now()),

('모바일 UI 테스트',
 '반응형 깨짐 여부 점검',
 'REVIEW','MEDIUM','PUBLIC','2026-06-22',
 5,4,3,3,now(),now()),

('에러 페이지 디자인',
 '404/500 페이지 디자인 개선',
 'TODO','LOW','PUBLIC','2026-06-27',
 5,4,3,3,now(),now()),

-- ADMIN 혼합
('알림 시스템 설계',
 '실시간 알림 구조 정의',
 'TODO','HIGH','PRIVATE','2026-06-27',
 1,3,1,2,now(),now()),

('파일 업로드 최적화',
 '대용량 파일 처리 개선',
 'IN_PROGRESS','HIGH','DEPARTMENT','2026-06-26',
 1,2,1,2,now(),now()),

('UI 접근성 개선',
 '웹 접근성 기준 반영',
 'TODO','MEDIUM','PUBLIC','2026-06-28',
 1,4,1,3,now(),now()),

('다크모드 지원',
 '다크모드 UI 설계 및 적용',
 'IN_PROGRESS','MEDIUM','PUBLIC','2026-06-25',
 1,5,1,3,now(),now()),

-- DEV 추가
('DB 인덱스 최적화',
 '조회 성능 개선',
 'IN_PROGRESS','HIGH','PRIVATE','2026-06-27',
 2,3,2,2,now(),now()),

('토큰 만료 처리',
 '리프레시 토큰 관리 개선',
 'REVIEW','HIGH','PRIVATE','2026-06-22',
 2,3,2,2,now(),now()),

-- DSG 추가
('컬러 시스템 정리',
 '브랜드 컬러 체계 정의',
 'DONE','LOW','PUBLIC','2026-06-19',
 4,5,3,3,now(),now()),

('아이콘 세트 정리',
 '아이콘 통일 및 리소스 정리',
 'IN_PROGRESS','LOW','PUBLIC','2026-06-26',
 4,5,3,3,now(),now()),

-- 김워크 관리자 업무 (정리된 구조)

('전체 인증 아키텍처 재설계',
 'JWT 기반 인증 구조 및 보안 정책 전면 재정의',
 'IN_PROGRESS','HIGH','PRIVATE','2026-06-29',
 1,1,1,2,now(),now()),

('전사 권한 체계 표준화',
 'ADMIN / MANAGER / USER 권한 정책 통합 설계',
 'REVIEW','HIGH','PRIVATE','2026-06-28',
 1,1,1,2,now(),now()),

('API 공통 응답 구조 설계',
 '전 시스템 Response DTO 표준화 및 에러 코드 체계 정리',
 'IN_PROGRESS','MEDIUM','DEPARTMENT','2026-06-27',
 1,1,1,2,now(),now()),

('전체 시스템 장애 대응 전략 수립',
 '장애 발생 시 자동 복구 및 알림 정책 정의',
 'TODO','HIGH','PRIVATE','2026-06-30',
 1,1,1,2,now(),now()),

('로그/모니터링 아키텍처 설계',
 'ELK 기반 로그 수집 구조 및 모니터링 기준 정의',
 'IN_PROGRESS','HIGH','DEPARTMENT','2026-06-26',
 1,1,1,2,now(),now()),

('데이터베이스 성능 관리 정책 수립',
 '슬로우 쿼리 탐지 및 인덱스 관리 기준 설계',
 'TODO','HIGH','PRIVATE','2026-06-28',
 1,1,1,2,now(),now()),

('파일 업로드 보안 정책 정의',
 'S3 업로드 검증 및 악성 파일 필터링 구조 설계',
 'IN_PROGRESS','HIGH','DEPARTMENT','2026-06-25',
 1,1,1,2,now(),now()),

('실시간 알림 시스템 구조 설계',
 'WebSocket 기반 이벤트 처리 아키텍처 정의',
 'TODO','HIGH','PRIVATE','2026-06-29',
 1,1,1,2,now(),now()),

('시스템 아키텍처 개선 전략 수립',
 '전체 서비스 구조 모듈화 및 확장성 개선 방향 정의',
 'REVIEW','HIGH','PRIVATE','2026-06-30',
 1,1,1,2,now(),now()),

('운영 안정성 및 배포 전략 개선',
 '무중단 배포 및 롤백 전략 표준화',
 'IN_PROGRESS','HIGH','DEPARTMENT','2026-06-27',
 1,1,1,2,now(),now());


-- 생성된 테이블 조회
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

