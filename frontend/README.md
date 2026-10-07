# AutoDoc Frontend

UI 구현 전 단계로 React Router와 구현 완료된 백엔드 API 호출 모듈만 구성한다.

## 실행

```bash
npm install
npm run dev
```

개발 서버는 `/api` 요청을 `http://localhost:8080`으로 프록시한다. 다른 백엔드 주소를 사용하려면 `.env`에 다음 값을 설정한다.

```properties
VITE_API_BASE_URL=/api
```

## 구성

- `src/router`: 화면 URL 라우트 정의. 현재 UI는 포함하지 않는다.
- `src/api`: Axios 공통 인스턴스와 도메인별 API 호출 함수.
- `withCredentials: true`: HttpSession 쿠키를 요청에 포함한다.

## 연동 대상

- 회원, 인증, 팀, 템플릿 메타데이터, 문서, SHEETS 열·행 API

관리자 회원 관리, 템플릿 파일, 첨부파일, 파일 생성·다운로드, 미리보기 API는 백엔드가 구현된 뒤 API 모듈을 추가한다.
