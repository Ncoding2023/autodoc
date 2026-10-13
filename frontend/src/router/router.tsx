import { Navigate, Outlet, createBrowserRouter } from 'react-router-dom';
import { AuthPage } from '../pages/AuthPage';

const RouteLayout = () => <Outlet />;
const EmptyRoute = () => null;

export const router = createBrowserRouter([
  {
    element: <RouteLayout />,
    children: [
      { index: true, element: <Navigate to="/login" replace /> },
      { path: '/login', element: <AuthPage key="login" /> },
      { path: '/signup', element: <AuthPage key="signup" signup /> },
      { path: '/documents', element: <EmptyRoute /> },
      { path: '/documents/new', element: <EmptyRoute /> },
      { path: '/documents/:documentId', element: <EmptyRoute /> },
      { path: '/documents/:documentId/edit', element: <EmptyRoute /> },
      { path: '/templates', element: <EmptyRoute /> },
      { path: '/templates/new', element: <EmptyRoute /> },
      { path: '/templates/:templateId', element: <EmptyRoute /> },
      { path: '/teams', element: <EmptyRoute /> },
      { path: '/profile', element: <EmptyRoute /> },
    ],
  },
]);
