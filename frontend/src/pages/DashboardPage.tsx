import { useAuth } from '../context';
import { StudentDashboard } from './StudentDashboard';
import { TeacherDashboard } from './TeacherDashboard';
import { AdminDashboard } from './AdminDashboard';
import { LoadingScreen } from '../components/ui';

export function DashboardPage() {
    const { user, isLoading } = useAuth();

    if (isLoading) {
        return <LoadingScreen />;
    }

    switch (user?.role) {
        case 'ADMIN':
            return <AdminDashboard />;
        case 'TEACHER':
            return <TeacherDashboard />;
        case 'STUDENT':
            return <StudentDashboard />;
        default:
            return <StudentDashboard />;
    }
}
