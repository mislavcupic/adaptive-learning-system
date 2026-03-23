// ============================================
// PROTECTED ROUTE - Zaštita ruta po roli
// ============================================

import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks';
import { ROUTES } from '../../constants';
import type { UserRole } from '../../types';
import { Loading } from '../common/Loading';

interface ProtectedRouteProps {
    children: React.ReactNode;
    allowedRoles?: UserRole[];
}

export const ProtectedRoute = ({
                                   children,
                                   allowedRoles,
                               }: ProtectedRouteProps) => {
    const { user, isAuthenticated, isLoading } = useAuth();
    const location = useLocation();

    // Čekaj dok se auth state učita
    if (isLoading) {
        return <Loading fullScreen />;
    }

    // Nije prijavljen - redirect na login
    if (!isAuthenticated) {
        return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />;
    }

    // Provjera role
    if (allowedRoles && user && !allowedRoles.includes(user.role)) {
        // Redirect na odgovarajući dashboard
        const dashboardRoute = getDashboardRoute(user.role);
        return <Navigate to={dashboardRoute} replace />;
    }

    return <>{children}</>;
};

// Helper za određivanje dashboarda po roli
const getDashboardRoute = (role: UserRole): string => {
    switch (role) {
        case 'ADMIN':
            return ROUTES.ADMIN_DASHBOARD;
        case 'TEACHER':
            return ROUTES.TEACHER_DASHBOARD;
        case 'STUDENT':
            return ROUTES.STUDENT_DASHBOARD;
        default:
            return ROUTES.LOGIN;
    }
};