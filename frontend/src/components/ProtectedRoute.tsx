import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context';
import { LoadingScreen } from '../components/ui';

interface ProtectedRouteProps {
    children: React.ReactNode;
    roles?: string[];
}

export function ProtectedRoute({ children, roles }: ProtectedRouteProps) {
    const { user, isAuthenticated, isLoading } = useAuth();
    const location = useLocation();

    if (isLoading) {
        return <LoadingScreen />;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    if (roles && user && !roles.includes(user.role)) {
        return <Navigate to="/dashboard" replace />;
    }

    return <>{children}</>;
}
