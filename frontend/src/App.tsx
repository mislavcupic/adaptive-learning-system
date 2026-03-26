import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, ThemeProvider, AccessibilityProvider } from './context';
import { MainLayout } from './components/layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import {
    LoginPage,
    RegisterPage,
    DashboardPage,
    SettingsPage,
    CoursesPage,
    TasksPage,
    TaskSolvePage,
    SubmissionsPage,
    StudentsPage,
    ClassesPage,
} from './pages';

// Lazy load less common pages
// const CourseDetailPage = lazy(() => import('./pages/CourseDetailPage'));

function App() {
    return (
        <BrowserRouter>
            <ThemeProvider>
                <AccessibilityProvider>
                    <AuthProvider>
                        <Routes>
                            {/* Public routes */}
                            <Route path="/login" element={<LoginPage />} />
                            <Route path="/register" element={<RegisterPage />} />

                            {/* Protected routes */}
                            <Route
                                path="/"
                                element={
                                    <ProtectedRoute>
                                        <MainLayout />
                                    </ProtectedRoute>
                                }
                            >
                                <Route index element={<Navigate to="/dashboard" replace />} />
                                <Route path="dashboard" element={<DashboardPage />} />
                                <Route path="settings" element={<SettingsPage />} />

                                {/* Courses */}
                                <Route path="courses" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <CoursesPage />
                                    </ProtectedRoute>
                                } />
                                <Route path="courses/:id" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <CoursesPage />
                                    </ProtectedRoute>
                                } />

                                {/* Tasks */}
                                <Route path="tasks" element={<TasksPage />} />
                                <Route path="tasks/:id" element={<TasksPage />} />
                                <Route path="tasks/:id/solve" element={<TaskSolvePage />} />

                                {/* Submissions */}
                                <Route path="submissions" element={<SubmissionsPage />} />
                                <Route path="submissions/:id" element={<SubmissionsPage />} />

                                {/* Students */}
                                <Route path="students" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <StudentsPage />
                                    </ProtectedRoute>
                                } />
                                <Route path="students/:id" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <StudentsPage />
                                    </ProtectedRoute>
                                } />

                                {/* Classes */}
                                <Route path="classes" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <ClassesPage />
                                    </ProtectedRoute>
                                } />
                                <Route path="classes/:id" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <ClassesPage />
                                    </ProtectedRoute>
                                } />

                                {/* Users - Admin only */}
                                <Route path="users" element={
                                    <ProtectedRoute roles={['ADMIN']}>
                                        <StudentsPage />
                                    </ProtectedRoute>
                                } />
                            </Route>

                            {/* 404 */}
                            <Route path="*" element={<Navigate to="/dashboard" replace />} />
                        </Routes>
                    </AuthProvider>
                </AccessibilityProvider>
            </ThemeProvider>
        </BrowserRouter>
    );
}

export default App;
