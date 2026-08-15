import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, ThemeProvider, AccessibilityProvider } from './context';
import { MainLayout } from './components/layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import {
    LoginPage,
    RegisterPage,
    RegistrationPendingPage,
    DashboardPage,
    SettingsPage,
    CoursesPage,
    TasksPage,
    TaskFormPage,
    UsersPage,
    TaskSolvePage,
    SubmissionsPage,
    SubmissionDetailPage,
    StudentsPage,
    ClassesPage,
    ApprovalQueuePage,
    AssessmentsPage,
    AssessmentSolvePage,
    AssessmentFormPage,
    ResearchResultsPage,
    AuditLogsPage,
    VerifyEmailPage,
    OAuth2CallbackPage
} from './pages';

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
                            <Route path="/registration-pending" element={<RegistrationPendingPage />} />
                            <Route path="/verify-email" element={<VerifyEmailPage />} />
                            <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />

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
                                <Route path="tasks/new" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <TaskFormPage />
                                    </ProtectedRoute>
                                } />
                                <Route path="tasks/:id" element={<TasksPage />} />
                                <Route path="tasks/:id/solve" element={<TaskSolvePage />} />

                                {/* Research */}
                                <Route path="/research-results" element={<ResearchResultsPage />} />

                                {/* Audit logs */}
                                <Route path="/audit-logs" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <AuditLogsPage />
                                    </ProtectedRoute>
                                } />

                                {/* Assessments */}
                                <Route path="assessments" element={<AssessmentsPage />} />
                                <Route path="assessments/:id/solve" element={<AssessmentSolvePage />} />
                                <Route path="assessments/new" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <AssessmentFormPage />
                                    </ProtectedRoute>
                                } />
                                <Route path="assessments/:id/edit" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <AssessmentFormPage />
                                    </ProtectedRoute>
                                } />

                                {/* Submissions */}
                                <Route path="submissions" element={<SubmissionsPage />} />
                                <Route path="/submissions/:id" element={<SubmissionDetailPage />} />

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

                                {/* Approval Queue */}
                                <Route path="pending-registrations" element={
                                    <ProtectedRoute roles={['ADMIN', 'TEACHER']}>
                                        <ApprovalQueuePage />
                                    </ProtectedRoute>
                                } />

                                {/* Users - Admin */}
                                <Route path="users" element={
                                    <ProtectedRoute roles={['ADMIN']}>
                                        <UsersPage />
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