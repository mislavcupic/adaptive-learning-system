import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Users, BookOpen, ClipboardList, FileText, Clock, TrendingUp } from 'lucide-react';
import { useAuth } from '../context';
import { useFetch } from '../hooks';
import { dashboardService } from '../services';
import { 
    Card, 
    CardHeader, 
    CardTitle, 
    CardContent,
    LoadingScreen,
    ErrorState,
    Badge,
    Avatar,
    Progress,
    EmptyState,
    Button
} from '../components/ui';
import { formatRelativeTime, formatSubmissionStatus, formatPercent } from '../utils';
import type { TeacherDashboardData } from '../types';

export function TeacherDashboard() {
    const { t } = useTranslation();
    const { user } = useAuth();
    
    const { data, loading, error, refetch } = useFetch<TeacherDashboardData>(
        () => dashboardService.getTeacherDashboard(),
        []
    );

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={refetch} />;
    if (!data) return null;

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Welcome */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {t('dashboard.welcome', { name: user?.firstName })}
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                        {t('dashboard.overview')}
                    </p>
                </div>
                <Link to="/tasks/new">
                    <Button>{t('tasks.createTask')}</Button>
                </Link>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
                <StatCard
                    icon={<Users className="w-5 h-5" />}
                    label={t('dashboard.totalStudents')}
                    value={data.totalStudents}
                />
                <StatCard
                    icon={<BookOpen className="w-5 h-5" />}
                    label={t('dashboard.totalCourses')}
                    value={data.totalCourses}
                />
                <StatCard
                    icon={<ClipboardList className="w-5 h-5" />}
                    label={t('dashboard.totalTasks')}
                    value={data.totalTasks}
                />
                <StatCard
                    icon={<FileText className="w-5 h-5" />}
                    label={t('dashboard.totalSubmissions')}
                    value={data.totalSubmissions}
                />
                <StatCard
                    icon={<Clock className="w-5 h-5" />}
                    label={t('dashboard.pendingReviews')}
                    value={data.pendingReviews}
                    highlight={data.pendingReviews > 0}
                />
                <StatCard
                    icon={<TrendingUp className="w-5 h-5" />}
                    label={t('dashboard.averageMastery')}
                    value={formatPercent(
                        data.studentProgress.length > 0
                            ? data.studentProgress.reduce((acc, s) => acc + s.averageMastery, 0) / data.studentProgress.length
                            : 0
                    )}
                />
            </div>

            <div className="grid lg:grid-cols-2 gap-6">
                {/* Student Progress */}
                <Card>
                    <CardHeader className="flex flex-row items-center justify-between">
                        <CardTitle>{t('dashboard.studentProgress')}</CardTitle>
                        <Link to="/students" className="text-sm text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300">
                            {t('common.actions')} →
                        </Link>
                    </CardHeader>
                    <CardContent>
                        {data.studentProgress.length === 0 ? (
                            <EmptyState
                                title={t('students.noStudents')}
                                description={t('students.noStudentsDesc')}
                            />
                        ) : (
                            <div className="space-y-4">
                                {data.studentProgress.slice(0, 5).map((student) => (
                                    <div
                                        key={student.studentId}
                                        className="flex items-center gap-4"
                                    >
                                        <Avatar
                                            firstName={student.studentName.split(' ')[0]}
                                            lastName={student.studentName.split(' ')[1]}
                                            size="sm"
                                        />
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center justify-between mb-1">
                                                <p className="font-medium text-zinc-900 dark:text-white truncate">
                                                    {student.studentName}
                                                </p>
                                                <span className="text-sm text-zinc-500 dark:text-zinc-400">
                                                    {formatPercent(student.averageMastery)}
                                                </span>
                                            </div>
                                            <Progress 
                                                value={student.averageMastery * 100} 
                                                size="sm"
                                                variant={
                                                    student.averageMastery >= 0.7 ? 'success' :
                                                    student.averageMastery >= 0.4 ? 'warning' : 'danger'
                                                }
                                            />
                                            <p className="text-xs text-zinc-400 mt-1">
                                                {student.totalSubmissions} predaja • {formatRelativeTime(student.lastActivity)}
                                            </p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* Recent Submissions */}
                <Card>
                    <CardHeader className="flex flex-row items-center justify-between">
                        <CardTitle>{t('dashboard.recentSubmissions')}</CardTitle>
                        <Link to="/submissions" className="text-sm text-zinc-500 hover:text-zinc-700 dark:hover:text-zinc-300">
                            {t('common.actions')} →
                        </Link>
                    </CardHeader>
                    <CardContent>
                        {data.recentSubmissions.length === 0 ? (
                            <EmptyState
                                title={t('submissions.noSubmissions')}
                                description={t('submissions.noSubmissionsDesc')}
                            />
                        ) : (
                            <div className="space-y-3">
                                {data.recentSubmissions.slice(0, 5).map((submission) => {
                                    const status = formatSubmissionStatus(submission.status);
                                    return (
                                        <Link
                                            key={submission.id}
                                            to={`/submissions/${submission.id}`}
                                            className="flex items-center justify-between py-2 px-3 -mx-3 rounded-lg hover:bg-zinc-50 dark:hover:bg-zinc-800/50 transition-colors"
                                        >
                                            <div className="min-w-0 flex-1">
                                                <p className="font-medium text-zinc-900 dark:text-white truncate">
                                                    {submission.studentName}
                                                </p>
                                                <p className="text-sm text-zinc-500 dark:text-zinc-400 truncate">
                                                    {submission.taskTitle} • {formatRelativeTime(submission.createdAt)}
                                                </p>
                                            </div>
                                            <Badge
                                                variant={
                                                    status.color === 'green' ? 'success' :
                                                    status.color === 'red' ? 'danger' :
                                                    status.color === 'amber' ? 'warning' : 'default'
                                                }
                                            >
                                                {status.label}
                                            </Badge>
                                        </Link>
                                    );
                                })}
                            </div>
                        )}
                    </CardContent>
                </Card>
            </div>

            {/* Courses */}
            <Card>
                <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle>{t('courses.title')}</CardTitle>
                    <Link to="/courses/new">
                        <Button variant="secondary" size="sm">{t('courses.createCourse')}</Button>
                    </Link>
                </CardHeader>
                <CardContent>
                    {data.courses.length === 0 ? (
                        <EmptyState
                            title={t('courses.noCourses')}
                            description={t('courses.noCoursesDesc')}
                            action={
                                <Link to="/courses/new">
                                    <Button>{t('courses.createCourse')}</Button>
                                </Link>
                            }
                        />
                    ) : (
                        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                            {data.courses.map((course) => (
                                <Link
                                    key={course.id}
                                    to={`/courses/${course.id}`}
                                    className="p-4 rounded-lg border border-zinc-200 dark:border-zinc-700 hover:border-zinc-300 dark:hover:border-zinc-600 transition-colors"
                                >
                                    <div className="flex items-start justify-between">
                                        <div>
                                            <h3 className="font-medium text-zinc-900 dark:text-white">
                                                {course.name}
                                            </h3>
                                            <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                                                {course.languageType === 'CSHARP' ? 'C#' : 'C'} • {course.outcomeCount || 0} ishoda
                                            </p>
                                        </div>
                                        <Badge variant={course.isActive ? 'success' : 'default'}>
                                            {course.isActive ? t('common.active') : t('common.inactive')}
                                        </Badge>
                                    </div>
                                </Link>
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}

function StatCard({ 
    icon, 
    label, 
    value,
    highlight = false
}: { 
    icon: React.ReactNode; 
    label: string; 
    value: string | number;
    highlight?: boolean;
}) {
    return (
        <Card className={highlight ? 'border-amber-300 dark:border-amber-700' : ''}>
            <CardContent className="pt-4">
                <div className="flex items-center gap-3">
                    <div className={`p-2 rounded-lg ${
                        highlight 
                            ? 'bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400'
                            : 'bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400'
                    }`}>
                        {icon}
                    </div>
                    <div>
                        <p className={`text-2xl font-semibold ${
                            highlight 
                                ? 'text-amber-600 dark:text-amber-400'
                                : 'text-zinc-900 dark:text-white'
                        }`}>
                            {value}
                        </p>
                        <p className="text-sm text-zinc-500 dark:text-zinc-400">
                            {label}
                        </p>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}
