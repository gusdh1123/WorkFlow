<img width="838" height="195" alt="Logo" src="https://github.com/user-attachments/assets/6c2066f5-5c85-4fa9-a9c8-2e986e561430" />

---


## 📋 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **프로젝트명** | **WorkFlow - 사내 업무 관리 시스템** |
| **개발 기간** | 2026.01.25 ~ 진행 중 |
| **개발 인원** | 2명 |
| **주요 기술** | **Java, Spring Security, JWT, JPA, React, PostgreSQL, GitHub Actions CI, Spring Boot, Gradle** |
| **프로젝트 주소** |  |
---

## ℹ️ 주제 선정 이유
이전 프로젝트에서 사용자 중심의 기능 구현을 경험한 이후, 권한 관리와 업무 상태 전이, 변경 이력과 같은 사내 업무 프로세스를 다뤄보고 싶다는 고민을 하게 되었습니다.
이를 가장 잘 경험할 수 있는 주제가 업무 관리 시스템이라고 판단해 주제로 선택했습니다.

---

## 🛠️ 사용 기술 스택

### 💻 Backend
- Java / Spring Boot
- Spring Security / JWT
- JPA
- Gradle

### 🖥️ Frontend
- HTML5 / CSS3
- React
- Quill

### 🗃️ Database
- PostgreSQL

### ⚙️ 협업 도구
- GitHub / Notion / Discord

---

# 📌 업무 관리 시스템 구현 목표

## 1️⃣ 사용자 / 권한

- 직원 계정 로그인  
- 역할(Role) 관리
  - 일반 직원
  - 관리자(sm 계정)
- 본인 정보 조회
- **권한 기반 접근 제어**: Spring Security + JWT  
- 역할별 기능 분기 설명 가능

---

## 2️⃣ 업무(Task) 관리 (핵심)

- 업무 등록, 수정, 삭제(논리 삭제/휴지통 개념)  
- 담당자 지정 및 변경  
- 마감일 설정  
- 담당자 변경 이력 기록  
- **업무 엔티티 중심 설계**

---

## 3️⃣ 업무 상태 관리 (상태 머신)

- **상태 정의**  
  - 대기(TODO), 진행중(IN_PROGRESS), 검토중(REVIEW), 취소/중단(CANCELED), 보류(ON_HOLD), 완료(DONE)
- **기본 흐름**  
  - TODO → IN_PROGRESS → REVIEW → DONE  
  - REVIEW → IN_PROGRESS (수정 요청)
- **예외 흐름**  
  - TODO / IN_PROGRESS → CANCELED  
  - IN_PROGRESS → ON_HOLD → IN_PROGRESS
- **제한**  
  - DONE / CANCELED → 변경 불가
- **정책**  
  - 상태 전이 제한(Service 레벨)  
  - Enum + 정책 클래스  
  - 취소/보류 시 사유 필수  
  - 검토 → 완료는 관리자/검토자만 가능  
  - 모든 상태 변경은 이력 테이블 기록

---

## 4️⃣ 댓글 / 커뮤니케이션

- 업무별 댓글 작성, 수정, 삭제  
- 작성자 및 작성 시간 기록  
- 댓글도 업무 흐름 기록 개념으로 관리

---

## 5️⃣ 첨부 파일

- 파일 업로드 / 다운로드  
- 업무별 첨부파일 관리  
- 업로드 사용자 기록  
- 로컬 스토리지 또는 S3 연동 가능

---

## 6️⃣ 변경 이력 (Audit Log)

- 자동 기록
  - 상태 변경, 담당자 변경, 마감일 변경
- 기록 내용
  - 변경 전/후, 변경자, 변경 시간
- 유지보수 및 추적 가능하도록 설계

---

## 7️⃣ 검색 / 필터

- 상태별 필터, 담당자별 필터  
- 기간(마감일) 검색  
- 키워드 검색(제목/내용)  
- QueryDSL 또는 동적 쿼리 활용, 인덱스 고려

---

## 8️⃣ 알림 (선택)

- 업무 할당 시 알림  
- 상태 변경 시 알림  
- 마감 임박 알림  
- 비동기 처리 및 실패 로그 기록

---

## 9️⃣ 후순위 기능 / 확장

- 전자결재(결재선 / 반려 / 재상신)  
- 실시간 채팅(1:1, 그룹)  
- 통계 대시보드(히스토리 포함)  
- 반응형 웹 / 모바일 대응  
- 서비스 기능 예시  
  - 날씨 및 날짜 기반 회식 안주 추천(AI 연동 가능)  
  - 캘린더, 시계 연동
---

## 📷 시연 이미지

<details>
<summary><h3>Login</h3></summary>

<img width="1121" height="720" alt="Login" src="https://github.com/user-attachments/assets/d6787074-980c-4925-8645-d554256935a9" />

</details>

<details>
<summary><h3>Dashboard</h3></summary>

<img width="2060" height="1226" alt="dashboard" src="https://github.com/user-attachments/assets/b0ceb778-389e-4e2e-a55f-30d250a7504e" />

</details>

<details>
<summary><h3>TASKS</h3></summary>

### TASKS
<details>
<summary><h3>List</h3></summary>

#### List
<img width="2060" height="1227" alt="List" src="https://github.com/user-attachments/assets/6481fe4d-2766-409b-8fe9-824589333c89" />

</details>

<details>
<summary><h3>Create</h3></summary>

#### Create
<img width="987" height="1189" alt="Create" src="https://github.com/user-attachments/assets/20ea7c24-5319-48b7-a3e3-c97eea61c199" />

</details>

<details>
<summary><h3>Read (첨부 파일 삭제는 아직 Update를 하지 않아서 일단 조회에서 동작하도록 함.)</h3></summary>

#### Read
<img width="1509" height="1040" alt="Read" src="https://github.com/user-attachments/assets/6622088d-49d7-402e-99f5-2c6ba00054a6" />

</details>

</details>

---
