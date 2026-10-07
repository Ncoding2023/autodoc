# AutoDoc 개발 규칙

## 개발 원칙

- 도메인 단위 패키지 구조를 사용한다.
- Controller → Service → Repository 구조를 따른다.
- Controller에 비즈니스 로직을 작성하지 않는다.
- Entity를 API에 직접 반환하지 않는다.
- 중복 로직을 만들지 않는다.
- 필요하지 않은 추상화와 디자인 패턴을 미리 만들지 않는다.

## 구현 순서

Database → Entity → DTO → Repository → Service → Controller → Test 순서로 구현한다.
기능 단위로 구현하고 검증한 뒤 다음 기능으로 진행한다.

## Database

- 외래 키(Foreign Key) 제약조건을 사용하지 않는다.
- PostgreSQL을 사용한다.

현재 승인된 구현 범위는 Database → Entity까지다. DTO, Repository, Service,
Controller, Frontend 및 Seed 기능은 후속 요청이 있을 때 구현한다. DB와 엔티티 설계는
사용자에게 제공받거나 확인된 요구사항을 기준으로 작성한다.

## 기술 스택

- Backend: Java 25, Spring Boot 4.1, Spring Data JPA, Hibernate, Gradle 9
- Frontend: React 19, TypeScript 5, Vite 8, React Router, Axios
- Styling: Tailwind CSS 4, Custom CSS
- 문서 처리: Apache POI 및 DOCX 처리 라이브러리 검토
- 인증: HttpSession, BCrypt
- 테스트: JUnit 5, Postman, Seed Data

## 개발 범위

- 1차 개발: 회원·팀 관리, 문서 CRUD, DOCS/SHEETS 작성, 기본·사용자 템플릿,
  문서 검색, DOCX/XLSX 생성·다운로드, 첨부파일을 구현한다.
- 확장 개발: 사용자 간 문서 공유, 문서 버전, Google Drive 등 외부 저장소 연동은
  1차 개발에 구현하지 않는다.

## DTO

- Request / Response DTO를 분리하고 Java record를 우선 사용한다.
- 생성/수정 입력값 또는 목록/상세 응답 구조가 다르면 DTO를 분리한다.
- Entity를 Request/Response로 직접 사용하지 않는다.

## Validation

- Jakarta Validation을 사용한다. 입력값 형식은 DTO에서 `@NotBlank`, `@NotNull`,
  `@Size`, `@Email` 등으로 검증한다.
- 소유권, 권한, 데이터 존재 여부, 문서 상태 등 비즈니스 규칙은 Service에서 검증한다.

## Repository와 Transaction

- Spring Data JPA와 `JpaRepository`로 기본 CRUD를 처리하고 필요한 조회만 추가한다.
  MyBatis는 사용하지 않는다.
- 데이터 변경은 Service에서 `@Transactional`을 사용하고, 하나의 업무 단위 변경을 하나의
  Transaction으로 처리한다.
- 조회는 필요한 경우 `@Transactional(readOnly = true)`를 사용한다.

## Exception

- `@RestControllerAdvice`로 공통 처리하고 ErrorCode / BusinessException / ErrorResponse
  구조를 사용한다.
- Controller에 반복적인 try-catch를 작성하지 않는다.

## 인증

- HttpSession 기반 인증과 BCrypt 비밀번호 암호화를 사용한다.
- 권한은 ADMIN / MANAGER / USER를 사용한다.
- 문서 접근 시 소유권 및 공유 권한을 검증한다.

## Seed Data

- local Profile에서만 실행하고 운영 환경에서는 실행하지 않는다.
- 중복 데이터 생성을 방지하고, 비밀번호는 BCrypt로 저장한다.
- 실제 개인정보를 사용하지 않는다. Seed는 자동화 테스트를 대체하지 않는다.
- 데이터베이스 접속 정보는 환경 변수로 관리하고 저장소에 비밀번호를 작성하지 않는다.
- 접속 변수는 `AUTODOC_POSTGRES_URL`, `AUTODOC_POSTGRES_USERNAME`,
  `AUTODOC_POSTGRES_PASSWORD`를 사용한다.

## Frontend

- React + TypeScript, designAxios 공통 Instance, Tailwind CSS 4를 사용한다.
- 반복 UI는 공통 Component로 분리하고 PC Web을 기본으로 Responsive Web을 지원한다.
- SHEETS는 좁은 화면에서 가로 스크롤을 허용한다.

## Git과 Test

- `main` / `develop` / `feature/*` Branch를 사용하고 기능 단위로 Commit한다.
- 핵심 비즈니스 로직은 Service Test를 작성하고 Validation과 실패 케이스도 테스트한다.
- 테스트에서 업무 흐름을 남기는 로그는 한글로 작성한다.
