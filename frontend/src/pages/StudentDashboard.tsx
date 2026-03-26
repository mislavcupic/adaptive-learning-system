import { useTranslation } from 'react-i18next';
import { BookOpen, ClipboardList, Trophy, TrendingUp } from 'lucide-react';
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
    SkillProgress,
    EmptyState
} from '../components/ui';
import { formatRelativeTime, formatSubmissionStatus, formatPercent } from '../utils';
import type { StudentDashboardData } from '../types';

export function StudentDashboard() {
    const { t } = useTranslation();
    const { user } = useAuth();
    
    const { data, loading, error, refetch } = useFetch<StudentDashboardData>(
        () => dashboardService.getStudentDashboard(),
        []
    );

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={refetch} />;
    if (!data) return null;

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Welcome */}
            <div>
                <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                    {t('dashboard.welcome', { name: user?.firstName })}
                </h1>
                <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                    {t('dashboard.overview')}
                </p>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    icon={<ClipboardList className="w-5 h-5" />}
                    label={t('dashboard.totalSubmissions')}
                    value={data.totalSubmissions}
                />
                <StatCard
                    icon={<Trophy className="w-5 h-5" />}
                    label={t('dashboard.completedTasks')}
                    value={data.completedTasks}
                />
                <StatCard
                    icon={<BookOpen className="w-5 h-5" />}
                    label={t('courses.title')}
                    value={data.enrolledCourses.length}
                />
                <StatCard
                    icon={<TrendingUp className="w-5 h-5" />}
                    label={t('dashboard.averageMastery')}
                    value={formatPercent(data.averageMastery)}
                />
            </div>

            <div className="grid lg:grid-cols-2 gap-6">
                {/* Skills */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('students.skillMastery')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {data.skillMasteries.length === 0 ? (
                            <EmptyState
                                title={t('common.noData')}
                                description="Riješite zadatke za praćenje vještina"
                            />
                        ) : (
                            <div className="space-y-4">
                                {data.skillMasteries.slice(0, 6).map((skill) => (
                                    <SkillProgress
                                        key={skill.id}
                                        name={skill.skillName}
                                        value={skill.masteryLevel}
                                    />
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* Recent Submissions */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('dashboard.recentSubmissions')}</CardTitle>
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
                                        <div
                                            key={submission.id}
                                            className="flex items-center justify-between py-2 border-b border-zinc-100 dark:border-zinc-800 last:border-0"
                                        >
                                            <div className="min-w-0 flex-1">
                                                <p className="font-medium text-zinc-900 dark:text-white truncate">
                                                    {submission.taskTitle}
                                                </p>
                                                <p className="text-sm text-zinc-500 dark:text-zinc-400">
                                                    {formatRelativeTime(submission.createdAt)}
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
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </CardContent>
                </Card>
            </div>

            {/* Enrolled Courses */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('students.enrolledCourses')}</CardTitle>
                </CardHeader>
                <CardContent>
                    {data.enrolledCourses.length === 0 ? (
                        <EmptyState
                            title={t('courses.noCourses')}
                            description="Niste upisani ni na jedan kolegij"
                        />
                    ) : (
                        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                            {data.enrolledCourses.map((course) => (
                                <div
                                    key={course.id}
                                    className="p-4 rounded-lg border border-zinc-200 dark:border-zinc-700 hover:border-zinc-300 dark:hover:border-zinc-600 transition-colors"
                                >
                                    <div className="flex items-start justify-between">
                                        <div>
                                            <h3 className="font-medium text-zinc-900 dark:text-white">
                                                {course.name}
                                            </h3>
                                            <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                                                {course.languageType === 'CSHARP' ? 'C#' : 'C'}
                                            </p>
                                        </div>
                                        <Badge variant={course.isActive ? 'success' : 'default'}>
                                            {course.isActive ? t('common.active') : t('common.inactive')}
                                        </Badge>
                                    </div>
                                </div>
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
    value 
}: { 
    icon: React.ReactNode; 
    label: string; 
    value: string | number;
}) {
    return (
        <Card>
            <CardContent className="pt-4">
                <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400">
                        {icon}
                    </div>
                    <div>
                        <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
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
