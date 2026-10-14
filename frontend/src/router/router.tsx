import { Navigate, Outlet, createBrowserRouter } from 'react-router-dom';
import { AuthPage } from '../pages/AuthPage';
import { AppLayout } from '../layouts/AppLayout';
import { WorkspacePage } from '../pages/WorkspacePage';
import { RouteErrorPage } from '../pages/RouteErrorPage';
import { requireUser } from './auth-routing';
import { TemplateSelectionPage } from '../pages/TemplateSelectionPage';
import { DocumentListPage } from '../pages/DocumentListPage';
import { DocumentFormPage } from '../pages/DocumentFormPage';
import { DocumentDetailPage } from '../pages/DocumentDetailPage';
import { createDocument, deleteDocument, loadDocument, loadDocumentForEdit, loadDocuments, loadDocumentTemplate, updateDocument } from '../pages/document-routes';

const RouteLayout = () => <Outlet />;

export const router = createBrowserRouter([
  {
    element: <RouteLayout />,
    hydrateFallbackElement: <main role="status" className="grid min-h-dvh place-items-center p-6 text-sm text-slate-500">로그인 상태와 화면을 확인하고 있습니다.</main>,
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
          { path: '/documents', element: <DocumentListPage />, loader: loadDocuments },
          { path: '/documents/new', element: <TemplateSelectionPage key="document-template-selection" /> },
          { path: '/documents/new/write', element: <DocumentFormPage key="create-document" creating />, loader: loadDocumentTemplate, action: createDocument },
          { path: '/documents/:documentId', element: <DocumentDetailPage />, loader: loadDocument, action: deleteDocument },
          { path: '/documents/:documentId/edit', element: <DocumentFormPage key="update-document" />, loader: loadDocumentForEdit, action: updateDocument },
          { path: '/templates', element: <TemplateSelectionPage key="basic-template-catalog" catalog /> },
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
