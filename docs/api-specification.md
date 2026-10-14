# AutoDoc API 명세

## 1차 개발 API

| 구분 | Method | API | 기능 | 인증 |
| --- | --- | --- | --- | --- |
| 회원 | POST | `/api/users` | 회원가입, 팀 소속 선택 가능 | X |
| 회원 | GET | `/api/users/signup-teams` | 회원가입용 기존 팀 목록 (ID, 이름) | X |
| 회원 | GET | `/api/users/{userId}` | 회원 정보 조회 | O |
| 회원 | PATCH | `/api/users/{userId}` | 회원 이름/소속 팀 수정 | O |
| 인증 | POST | `/api/auth/login` | 로그인 | X |
| 인증 | POST | `/api/auth/logout` | 로그아웃 | O |
| 인증 | GET | `/api/auth/me` | 로그인 사용자 조회 | O |
| 팀 | POST | `/api/teams` | 팀 등록 | O |
| 팀 | GET | `/api/teams` | 팀 목록 조회 | O |
| 팀 | GET | `/api/teams/{teamId}` | 팀 상세 조회 | O |
| 팀 | PATCH | `/api/teams/{teamId}` | 팀 정보 수정 | O |
| 팀 | DELETE | `/api/teams/{teamId}` | 팀 삭제 | O |
| 회원 관리 | GET | `/api/admin/users` | 전체 회원 목록/검색 | O |
| 회원 관리 | PATCH | `/api/admin/users/{userId}/role` | 회원 권한 변경 | O |
| 회원 관리 | PATCH | `/api/admin/users/{userId}/status` | 회원 상태 변경 | O |
| 회원 관리 | PATCH | `/api/admin/users/{userId}/team` | 회원 소속 팀 변경/해제 | O |
| 템플릿 | GET | `/api/templates` | 사용 가능한 템플릿 목록 조회 | O |
| 템플릿 | GET | `/api/templates/{templateId}` | 템플릿 상세 조회 | O |
| 템플릿 | POST | `/api/templates` | 사용자 템플릿 등록 | O |
| 템플릿 | PATCH | `/api/templates/{templateId}` | 사용자 템플릿 정보 수정 | O |
| 템플릿 | DELETE | `/api/templates/{templateId}` | 사용자 템플릿 삭제 | O |
| 템플릿 파일 | POST | `/api/templates/{templateId}/file` | DOCX/XLSX 원본 업로드 | O |
| 문서 | POST | `/api/documents` | 문서 생성 | O |
| 문서 | GET | `/api/documents` | 내 문서 목록/검색 | O |
| 문서 | GET | `/api/documents/{documentId}` | 문서 상세 조회 | O |
| 문서 | PATCH | `/api/documents/{documentId}` | 문서 기본정보/DOCS 내용 수정 | O |
| 문서 | DELETE | `/api/documents/{documentId}` | 문서 삭제 | O |
| SHEETS 열 | POST | `/api/documents/{documentId}/columns` | 열 추가 | O |
| SHEETS 열 | GET | `/api/documents/{documentId}/columns` | 열 목록 조회 | O |
| SHEETS 열 | PATCH | `/api/documents/{documentId}/columns/{columnId}` | 열 이름/타입 수정 | O |
| SHEETS 열 | PATCH | `/api/documents/{documentId}/columns/order` | 열 순서 변경 | O |
| SHEETS 열 | DELETE | `/api/documents/{documentId}/columns/{columnId}` | 열 삭제 및 행 데이터의 해당 키 제거 | O |
| SHEETS 행 | POST | `/api/documents/{documentId}/rows` | 행 추가 | O |
| SHEETS 행 | GET | `/api/documents/{documentId}/rows` | 행 목록 조회 | O |
| SHEETS 행 | PATCH | `/api/documents/{documentId}/rows/{rowId}` | 행 데이터 수정 | O |
| SHEETS 행 | DELETE | `/api/documents/{documentId}/rows/{rowId}` | 행 삭제 | O |
| 첨부파일 | POST | `/api/documents/{documentId}/files` | 첨부파일 업로드 | O |
| 첨부파일 | GET | `/api/documents/{documentId}/files` | 첨부파일 목록 조회 | O |
| 첨부파일 | GET | `/api/documents/{documentId}/files/{fileId}` | 파일 다운로드 | O |
| 첨부파일 | DELETE | `/api/documents/{documentId}/files/{fileId}` | 첨부파일 삭제 | O |
| 출력 | POST | `/api/documents/{documentId}/exports` | DOCX/XLSX 생성 | O |
| 출력 | GET | `/api/documents/{documentId}/exports/{fileId}` | 생성 파일 다운로드 | O |
| 미리보기 | GET | `/api/documents/{documentId}/preview` | 문서 미리보기 조회 | O |

## 확장 개발 API

| 구분 | Method | API | 기능 | 인증 |
| --- | --- | --- | --- | --- |
| 버전 | GET | `/api/documents/{documentId}/versions` | 문서 버전 목록 조회 | O |
| 버전 | GET | `/api/documents/{documentId}/versions/{versionId}` | 특정 버전 조회 | O |
| 버전 | POST | `/api/documents/{documentId}/versions` | 현재 문서 버전 저장 | O |
| Drive | POST | `/api/documents/{documentId}/drive` | Google Drive 파일 생성/연동 | O |
| Drive | GET | `/api/documents/{documentId}/drive` | Drive 연동 정보 조회 | O |
| Drive | POST | `/api/documents/{documentId}/drive/sync` | Drive 파일 동기화 | O |
| Drive | DELETE | `/api/documents/{documentId}/drive` | Drive 연동 해제 | O |
| 공유 | POST | `/api/documents/{documentId}/shares` | AutoDoc 사용자에게 문서 공유 | O |
| 공유 | GET | `/api/documents/{documentId}/shares` | 문서 공유 목록 조회 | O |
| 공유 | PATCH | `/api/documents/{documentId}/shares/{shareId}` | 공유 권한/만료일 수정 | O |
| 공유 | DELETE | `/api/documents/{documentId}/shares/{shareId}` | 문서 공유 해제 | O |
| 공유 | GET | `/api/shared-documents` | 나에게 공유된 문서 목록 조회 | O |

## 회원가입 팀 정보

회원가입(`POST /api/users`)은 `email`, `password`, `name`과 선택적인 팀 정보를 받는다.
개인 가입은 `teamId`와 `team`을 생략한다. 기존 팀 가입은 `teamId`를 전달하고,
새 팀 등록은 `team: { "name": "팀 이름", "description": "선택 설명" }`을 전달한다.
`teamId`와 `team`은 동시에 전달할 수 없다. 새 팀과 회원은 같은 트랜잭션으로 저장한다.
회원가입용 팀 목록은 로그인 없이 ID와 이름만 제공한다.
비밀번호 찾기는 현재 프론트엔드 안내만 제공하며 재설정 API는 구현하지 않았다.

## 시트 문서 생성

SHEETS 문서 생성(`POST /api/documents`)에는 선택적으로 `sheet`를 전달한다.
`sheet.columns`는 표시 순서대로 `columnKey`, `columnName`, `columnType`을 받으며,
`sheet.rows`는 각 행의 `rowData` 객체를 받는다. 행 데이터의 키는 열의 `columnKey`와 일치해야 한다.
문서·열·행은 한 트랜잭션으로 저장하며 일부 입력이 잘못되면 전체를 롤백한다.
`sheet`를 생략하면 템플릿의 기본 열을 사용한다. DOCS 생성 요청에는 `sheet`를 포함할 수 없다.

## 시트 문서 일괄 수정

`PATCH /api/documents/{documentId}`는 문서 정보와 함께 생성 요청과 동일한 구조의 `sheet`를 받는다.
SHEETS에 `sheet`를 전달하면 열·행 전체를 전달된 순서와 내용으로 교체한다. 빈 배열은 전체 삭제를 의미한다.
문서 정보와 열·행은 하나의 트랜잭션으로 반영하며 검증 또는 저장 실패 시 기존 데이터를 유지한다.
`sheet` 생략 시 기존 열·행을 유지한다. DOCS 수정에는 `sheet`를 전달할 수 없다.
교체 시 열·행 ID는 새로 발급되며 문서 ID는 유지된다.

## 표기

- `O`: HttpSession 인증이 필요한 API
- `X`: 인증 없이 호출할 수 있는 API
- `{...}`: 경로 변수
