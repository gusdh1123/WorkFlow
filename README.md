[<img width="838" height="195" alt="Logo" src="https://github.com/user-attachments/assets/6c2066f5-5c85-4fa9-a9c8-2e986e561430" />](https://www.workflow.kro.kr/)

---


## 📋 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **프로젝트명** | **WorkFlow - 사내 업무 관리 시스템** |
| **개발 기간** | **v1.0**: 2026.01.25 ~ 2026.03.19<br>**v1.1**: 2026.04.01 ~ 2026.04.03 |
| **개발 인원** | 2명 |
| **주요 기술** | **Java, Spring Security, JWT, JPA, React, PostgreSQL, GitHub Actions CI, Spring Boot, Gradle** |
| **프로젝트 주소** | https://www.workflow.kro.kr/ |

📌 처음 사용하는 기술 스택을 학습하기 위해 시작한 프로젝트로, 기능을 확장하며 지속적으로 개발을 진행하고 있으며, 업무 흐름과 권한, 이력 추적을 고려한 시스템 설계를 목표로 했습니다.

---

## ℹ️ 주제 선정 이유
실제 업무 관리 시스템을 직접 경험해보지는 않았지만, 프로젝트를 통해 권한 관리, 상태 전이, 변경 이력과 같은 실무적인 요소를 설계하고 구현해보고자 했습니다.

특히 협업 환경에서 발생할 수 있는 상황을 가정하고, 다음과 같은 문제를 정의했습니다.

- 역할에 따라 접근 권한이 달라지는 구조
- 업무 상태가 임의로 변경되지 않도록 하는 상태 전이 규칙
- 변경 이력을 기록하여 업무 추적이 가능한 구조

이를 기반으로 사내 업무 관리 시스템을 설계하고 구현했습니다.

---

## 🎯 기획 의도

- 업무 상태로 진행 상황을 쉽게 파악하도록 함 (TODO, IN_PROGRESS, DONE 등)
- 역할별 권한으로 사용자별 작업 범위를 명확히 구분
- 업무 수정, 상태 변경, 삭제 시 변경 이력을 기록해 추적 가능하도록 설계
- 삭제된 데이터는 일정 기간 복구 가능하게 하여 실수에 대응

---

## 🧠 설계 포인트

- 업무 상태 전이를 규칙으로 관리해 임의 변경 방지
- 상태, 담당자, 내용 등 변경 이력을 별도 로그로 기록해 상세 추적
- Soft Delete 기반으로 삭제 데이터 관리, 복구와 유지보수 효율성 확보
- 권한(Role)에 따라 조회 범위와 기능 분리해 사용자별 맞춤 업무 흐름 지원

---

## ⚠️ 트러블 슈팅

- 상태 전이 검증을 Controller에서 Service 레벨로 옮겨 보안 강화
- 변경 이력에 변경 사유(reason)를 필수 입력하도록 해 변경 배경 명확화
- Soft Delete 데이터 누적 문제 해결 위해 삭제 2주 후 자동 물리 삭제 Scheduler 도입

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
- 삭제된 업무 복구 가능
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
  - 모든 상태 변경은 audit_logs(이력 테이블)에 기록


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

### 7️⃣ 즐겨찾기
- 별 아이콘 클릭 시 즐겨찾기 등록/해제 (낙관적 업데이트 적용)
- 즐겨찾기 기준 정렬 및 필터링 지원

---

## 🚧 예정 기능

- **v1.2**
    - **검색 기능**
        - 제목, 키워드, 작성자/담당자 필터 검색
        - 드롭다운(미리보기)
    - **알림 시스템**
        - 글 수정/삭제/복구, 담당자 변경 마감 임박 등 알림
        - 비동기 처리 및 실패 복구 구조 설계
    - **운영 효율성 개선**
        - 스케줄러 안정성 개선
        - 로그 및 모니터링 체계 고도화

---

## 🗺️ 향후 확장 계획
- ~~즐겨찾기 기능: 자주 사용하는 업무를 별도로 저장하여 조회~~(v1.1)
- ~~업무 복구 기능: 삭제된 업무를 복구~~(v1.1)
- ~~검색 및 필터 기능 강화: 업무 상태, 담당자, 기간, 키워드 등 다양한 조건으로 빠르고 정확한 업무 검색 지원~~(v1.2 예정)
- 댓글 기능 추가: 업무별 댓글 작성, 수정, 삭제 기능 및 작성자 표시와 작성 시간 기록으로 원활한 협업 지원
- ~~알림 시스템: 상태 변경, 마감 임박 등 실시간 알림 기능 비동기 처리 및 실패 복구~~(v1.2 예정)
- 고급 통계 및 대시보드 개발: 업무 진행 상황 시각화 및 성과 분석, 의사결정 지원
- 캘린더 기능 추가: 마감일 및 업무 일정을 편리하게 시각화, 일정 추가/수정/삭제 및 반복 일정 관리 가능
- 전자결재 시스템 도입: 업무 승인과 결재 절차 자동화로 비즈니스 프로세스 효율화
- 모바일 반응형 웹: 언제 어디서나 업무 접근성 확대 및 사용자 편의 개선
- 실시간 채팅 기능 추가: 팀 간 즉각 소통 지원, 업무 협업 강화
- AI 업무 지원 기능: 업무 자동화, 일정 추천, 문서 요약, 회식 메뉴 추천 등 사용자 맞춤형 서비스 제공
- 운영 효율성 강화: 자동 삭제 스케줄러 개선, 로그 및 모니터링 체계 고도화 (지속 유지)
- 프로젝트, 팀 기능
    - 팀원 역할 및 협업 현황 관리
    - 업무 분배 및 진행 상태 시각화
    - 협업 이슈 및 성과 리포트 제공 등 프로젝트 관리 지원
- 리포트 기능
    - 업무 진행 현황, 성과, 변경 이력 요약
    - 기간별·상태별 필터, PDF 내보내기 등 보고서 작성 지원
- 설정 페이지 기능 개발
    - 개인 정보 수정 및 보안 관리
    - 권한 확인 및 일부 권한 신청
    - 알림 수신 및 시스템 환경 설정
    - 로그인 기록 조회 및 세션 관리

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

<li>
<details>
<summary><b>Restore</b></summary>

<img width="2025" height="1221" alt="Restore" src="https://github.com/user-attachments/assets/7b9370f3-897e-4c14-b3bd-5b96704c16de" />


</details>
</li>

<li>
<details>
<summary><b>Favorite</b></summary>

<img width="2026" height="1206" alt="Favorite" src="https://github.com/user-attachments/assets/85316cd9-8984-44f5-a180-5ad542a24d48" />

<img width="580" height="315" alt="Favorite1" src="https://github.com/user-attachments/assets/86a1dc16-ffbb-4627-8b91-f8c9aaa44119" />

</details>
</li>

</ul>
</details>

---

## 📦 버전 기록

<details>
<summary><b>v1.0 (2026-03-19)</b></summary>

  
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

<details>
<summary><b>v1.1 (2026-04-03)</b></summary>

  
- 업무 복구 기능 추가
    - 리스트 / 상세 페이지에서 복구 가능
    - 복구 시 사유 입력 필수
    - restore 로그 기록 추가
- 삭제된 업무 조회 기능 개선
    - 삭제 시간 기준 정렬 (최근순 / 오래된순)
    - 삭제 시간 UI 강조 (빨간색)
- 즐겨찾기 기능 추가
    - 전용 테이블 설계
    - 낙관적 업데이트 적용
    - 즐겨찾기 기준 정렬 및 필터 유지
    - 대시보드에서 조회 가능
- 대시보드 기능 개선
    - 업무 제목 클릭 시 상세 페이지 이동


</details>

---
