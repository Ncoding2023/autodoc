# AutoDoc Enum 목록

각 키값은 데이터베이스와 API에서 사용하는 Enum 이름이다. 목록명은 화면과 문서에서 사용하는 한글 표기다.

## 회원 권한 (`UserRole`)

| 키값 | 목록명 |
| --- | --- |
| `ADMIN` | 관리자 |
| `MANAGER` | 매니저 |
| `USER` | 일반 사용자 |

## 회원 상태 (`UserStatus`)

| 키값 | 목록명 |
| --- | --- |
| `ACTIVE` | 활성 |
| `INACTIVE` | 비활성 |

## 문서 작성 형식 (`WritingFormat`)

| 키값 | 목록명 |
| --- | --- |
| `DOCS` | DOCS형 |
| `SHEETS` | SHEETS형 |

## 문서 상태 (`DocumentStatus`)

| 키값 | 목록명 |
| --- | --- |
| `DRAFT` | 작성 중 |
| `COMPLETED` | 작성 완료 |

## 문서 파일 용도 (`DocumentFileRole`)

| 키값 | 목록명 |
| --- | --- |
| `ATTACHMENT` | 첨부파일 |
| `OUTPUT` | 생성 파일 |

## SHEETS 열 형식 (`SheetColumnType`)

| 키값 | 목록명 |
| --- | --- |
| `TEXT` | 텍스트 |
| `NUMBER` | 숫자 |
| `DATE` | 날짜 |
| `BOOLEAN` | 참/거짓 |

## 템플릿 범위 (`TemplateScope`)

| 키값 | 목록명 |
| --- | --- |
| `BASIC` | 기본 템플릿 |
| `USER` | 사용자 템플릿 |

## 템플릿 분석 상태 (`AnalysisStatus`)

| 키값 | 목록명 |
| --- | --- |
| `PENDING` | 분석 대기 |
| `READY` | 분석 완료 |
| `FAILED` | 분석 실패 |

## 템플릿 파일 확장자 (`TemplateFileExtension`)

| 키값 | 목록명 |
| --- | --- |
| `DOCX` | DOCX |
| `XLSX` | XLSX |

## 오류 코드 (`ErrorCode`)

| 키값 | 목록명 |
| --- | --- |
| `USER_NOT_FOUND` | 회원을 찾을 수 없음 |
| `DUPLICATE_EMAIL` | 이미 사용 중인 이메일 |
| `TEAM_NOT_FOUND` | 팀을 찾을 수 없음 |
| `INVALID_CREDENTIALS` | 이메일 또는 비밀번호가 일치하지 않음 |
| `INACTIVE_USER` | 비활성 회원 |
| `UNAUTHORIZED` | 로그인 인증 필요 |
