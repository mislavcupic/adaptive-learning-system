import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { FontProvider } from './context/FontContext';
import { Layout } from './components';
import { Login } from './pages/auth';
import { StudentDashboard } from './pages/student';
import { TeacherDashboard } from './pages/teacher';
import { AdminDashboard } from './pages/admin';
import { ROUTES } from './constants';

function App() {
    return (
        <BrowserRouter>
            <FontProvider>
                <AuthProvider>
                    <Routes>
                        <Route path={ROUTES.LOGIN} element={<Login />} />

                        <Route element={<Layout />}>
                            <Route path={ROUTES.STUDENT_DASHBOARD} element={<StudentDashboard />} />
                            <Route path={ROUTES.TEACHER_DASHBOARD} element={<TeacherDashboard />} />
                            <Route path={ROUTES.ADMIN_DASHBOARD} element={<AdminDashboard />} />
                        </Route>

                        <Route path="/" element={<Navigate to={ROUTES.LOGIN} replace />} />
                        <Route path="*" element={<Navigate to={ROUTES.LOGIN} replace />} />
                    </Routes>
                </AuthProvider>
            </FontProvider>
        </BrowserRouter>
    );
}

export default App;