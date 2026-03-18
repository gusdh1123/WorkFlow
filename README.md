<img width="838" height="195" alt="Logo" src="https://github.com/user-attachments/assets/6c2066f5-5c85-4fa9-a9c8-2e986e561430" />

---


## 📋 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **프로젝트명** | **WorkFlow(v1.0 예정) - 사내 업무 관리 시스템** |
| **개발 기간** | 2026.01.25 ~ 진행 중 |
| **개발 인원** | 2명 |
| **주요 기술** | **Java, Spring Security, JWT, JPA, React, PostgreSQL, GitHub Actions CI, Spring Boot, Gradle** |
| **프로젝트 주소** | GitHub Repository |

📌 처음 사용하는 기술 스택을 학습하기 위해 시작한 프로젝트로, 기능을 확장하며 지속적으로 개발을 진행하고 있습니다.

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
- React / VS Code
- HTML5 / CSS3
- Quill

### 🗃️ Database
- PostgreSQL

### ⚙️ 협업 도구
- GitHub / Notion / Discord / GitHub Actions(CI)

---

## 📌 현재 구현된 기능

### 1️⃣ 사용자 / 인증 / 권한

- Spring Security + JWT 기반 인증
  - Access Token: API 호출 인증, 만료 15분
  - Refresh Token: Access Token 갱신, DB 해시값 저장, Rotation 적용, 만료 14일
- Cookie 기반 토큰 전달 (Secure / HttpOnly / SameSite), 만료 14일
- 로그인 / 로그아웃
  - 비밀번호 해시 처리 후 DB 저장 및 검증
- 권한(Role) 기반 접근 제어
  - ADMIN : 시스템 전체 관리
  - MANAGER : 부서 업무 관리
  - USER : 일반 업무 사용자
- Refresh Token 사용 이력 기록 및 Scheduler로 2주 후 자동 정리

### 2️⃣ 업무 관리

- 업무 등록 / 조회 / 수정 / 삭제(Soft Delete)
- 상태, 우선순위, 마감일, 담당자, 공개 범위 설정 및 변경
- 이미지 업로드 및 YouTube 영상 링크 첨부 지원


### 3️⃣ 업무 상태 관리

- 상태 정의
  - 대기(TODO), 진행중(IN_PROGRESS), 검토중(REVIEW), 보류(ON_HOLD), 취소(CANCELED), 완료(DONE)
- 권한 정책
  - 작성자 또는 담당자는 제한된 상태 전이만 가능
  - 관리자(ADMIN) 및 팀장(MANAGER)은 모든 상태 변경 가능
- 상태 관리 정책
  - 상태 전이는 Service 레벨에서 검증
  - Enum 기반 상태 정의 및 상태 전이 규칙 적용
  - 업무 수정 시 변경 사유(reason) 입력 필수
  - 모든 상태 변경은 Activity Log(이력 테이블)에 기록


### 4️⃣ 첨부 파일

- 파일 업로드 / 다운로드 / 삭제(Soft Delete)
- Scheduler로 삭제된지 2주 지난 파일 자동 제거


### 5️⃣ 변경 이력

- 모든 업무(Task) 수정 및 삭제 시 기록
  - 상태, 제목, 담당자, 마감일, 내용, 공개 범위, 중요도
  - 첨부파일 추가/삭제
  - 삭제/수정 사유(reason) 필수
- DB에 저장 후 유지보수 및 추적 가능

### 6️⃣ 필터

- USER / MANAGER
  - 전체 / 전사 / 팀 / 내 업무 / 담당 업무
  - 상태별 필터
  - 정렬: 최신순, 마감순, 우선순위
- ADMIN
  - USER / MANAGER 필터 포함
  - 부서별 필터 가능

---

## 🚧 진행 중 기능

---

## 🗺️ 향후 확장 계획

---

## 📷 시연 이미지

<details>
<summary><b>Login<b></summary>

<img width="1121" height="720" alt="Login" src="https://github.com/user-attachments/assets/d6787074-980c-4925-8645-d554256935a9" />

</details>

<details>
<summary><b>Dashboard</b></summary>

<img width="1924" height="1201" alt="DashBoard" src="https://github.com/user-attachments/assets/ffe9d8c1-fd5a-4b6d-92db-5d988dc779b0" />

</details>

<details>
<summary><b>Tasks</b></summary>
<p></p>

<ul>
<li>
<details>
<summary><b>List</b></summary>

<img width="1816" height="1211" alt="List" src="https://github.com/user-attachments/assets/00d0d52e-51b0-4297-8b3d-0070fc9e4543" />

</details>
</li>

<li>
<details>
<summary><b>Create</b></summary>

<img width="1811" height="1237" alt="Create" src="https://github.com/user-attachments/assets/b8d2984f-1f36-4328-9eb3-c99814bd01ae" />

</details>
</li>

<li>
<details>
<summary><b>Read</b></summary>

<img width="1736" height="1212" alt="Read" src="https://github.com/user-attachments/assets/82d2bb5b-353d-4699-9edc-a8e85e0f2b2c" />

</details>
</li>

<li>
<details>
<summary><b>Update</b></summary>

<img width="1618" height="1226" alt="Update" src="https://github.com/user-attachments/assets/76add4f7-056a-455f-89c2-f6c907c8b196" />

</details>
</li>

<li>
<details>
<summary><b>Delete</b></summary>

<img width="1629" height="1259" alt="Delete" src="https://github.com/user-attachments/assets/2f3a30de-310c-46b3-81ae-b75620bb73fd" />

</details>
</li>

</ul>
</details>

---

## 📦 버전 기록

<details>
<summary><b>v1.0 (예정)</b></summary>

  
- 기본 기능
  - 사용자 인증/권한 관리: JWT, Spring Security 기반
  - 업무(Task) 관리: 등록, 조회, 수정, 삭제 기능
  - 업무 상태 전이 & Activity Log: 상태 변경 시 이력 기록
  - 첨부 파일 관리: 업로드, 다운로드, Soft Delete, 삭제 후 2주 자동 제거
  - 변경 이력 기록: 상태, 담당자, 마감일, 제목, 첨부파일 등
  - 필터 및 정렬 기능: 사용자별, 부서별, 상태별
  - 미디어 첨부: 이미지 업로드 및 YouTube 영상 링크
  - 프론트엔드 화면 기본 구현: 대시보드, 리스트, 작성, 상세 조회, 수정 페이지
 

</details>

---
