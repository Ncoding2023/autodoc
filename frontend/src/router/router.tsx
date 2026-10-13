import { Navigate, Outlet, createBrowserRouter } from 'react-router-dom';
import { AuthPage } from '../pages/AuthPage';
import { AppLayout } from '../layouts/AppLayout';
import { WorkspacePage } from '../pages/WorkspacePage';
import { RouteErrorPage } from '../pages/RouteErrorPage';
import { requireUser } from './auth-routing';

const RouteLayout = () => <Outlet />;

export const router = createBrowserRouter([
  {
    element: <RouteLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: '/login', element: <AuthPage key="login" /> },
      { path: '/signup', element: <AuthPage key="signup" signup /> },
      {
        id: 'workspace',
        element: <AppLayout />,
        loader: requireUser,
        shouldRevalidate: () => true,
        errorElement: <RouteErrorPage />,
        children: [
          { path: '/dashboard', element: <WorkspacePage title="대시보드" description="문서 현황과 최근 문서를 확인합니다." dashboard /> },
          { path: '/documents', element: <WorkspacePage title="문서 관리" description="내 문서 목록과 검색 기능이 제공될 예정입니다." /> },
          { path: '/documents/new', element: <WorkspacePage title="새 문서 작성" description="작성 형식과 템플릿을 선택하는 기능이 제공될 예정입니다." /> },
          { path: '/documents/:documentId', element: <WorkspacePage title="문서 상세" description="문서 내용을 조회하는 기능이 제공될 예정입니다." /> },
          { path: '/documents/:documentId/edit', element: <WorkspacePage title="문서 수정" description="문서 내용을 수정하는 기능이 제공될 예정입니다." /> },
          { path: '/templates', element: <WorkspacePage title="템플릿 관리" description="기본 템플릿 조회와 사용자 템플릿 관리 기능이 제공될 예정입니다." /> },
          { path: '/templates/new', element: <WorkspacePage title="템플릿 등록" description="사용자 템플릿을 등록하는 기능이 제공될 예정입니다." /> },
          { path: '/templates/:templateId', element: <WorkspacePage title="템플릿 상세" description="템플릿 구조와 디자인을 조회하는 기능이 제공될 예정입니다." /> },
          { path: '/teams', element: <WorkspacePage title="팀 관리" description="팀 정보 조회와 관리 기능이 제공될 예정입니다." /> },
          { path: '/profile', element: <WorkspacePage title="내 정보" description="회원 정보 조회와 수정 기능이 제공될 예정입니다." /> },
        ],
      },
      { path: '*', element: <Navigate to="/dashboard" replace /> },
    ],
  },
]);
