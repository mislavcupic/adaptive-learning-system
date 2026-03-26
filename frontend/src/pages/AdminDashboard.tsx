import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { 
    Users, 
    GraduationCap, 
    UserCog, 
    ShieldCheck,
    BookOpen, 
    ClipboardList,
    FolderKanban,
    FileText,
    Database,
    Cpu,
    Code,
    CheckCircle,
    XCircle,
    AlertCircle
} from 'lucide-react';
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
    Button
} from '../components/ui';
import type { AdminDashboardData } from '../types';

export function AdminDashboard() {
    const { t } = useTranslation();
    const { user } = useAuth();
    
    const { data, loading, error, refetch } = useFetch<AdminDashboardData>(
        () => dashboardService.getAdminDashboard(),
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
                        {t('dashboard.overview')} - {t('roles.admin')}
                    </p>
                </div>
                <Link to="/users/new">
                    <Button>{t('users.createUser')}</Button>
                </Link>
            </div>

            {/* User Stats */}
            <div>
                <h2 className="text-lg font-medium text-zinc-900 dark:text-white mb-4">
                    {t('users.title')}
                </h2>
                <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
                    <StatCard
                        icon={<Users className="w-5 h-5" />}
                        label="Ukupno korisnika"
                        value={data.totalUsers}
                    />
                    <StatCard
                        icon={<GraduationCap className="w-5 h-5" />}
                        label={t('dashboard.totalStudents')}
                        value={data.totalStudents}
                    />
                    <StatCard
                        icon={<UserCog className="w-5 h-5" />}
                        label="Nastavnici"
                        value={data.totalTeachers}
                    />
                    <StatCard
                        icon={<ShieldCheck className="w-5 h-5" />}
                        label="Administratori"
                        value={data.totalAdmins}
                    />
                    <StatCard
                        icon={<CheckCircle className="w-5 h-5" />}
                        label={t('dashboard.activeUsers')}
                        value={data.activeUsers}
                        variant="success"
                    />
                </div>
            </div>

            {/* Content Stats */}
            <div>
                <h2 className="text-lg font-medium text-zinc-900 dark:text-white mb-4">
                    Sadržaj sustava
                </h2>
                <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
                    <StatCard
                        icon={<BookOpen className="w-5 h-5" />}
                        label={t('dashboard.totalCourses')}
                        value={data.totalCourses}
                    />
                    <StatCard
                        icon={<CheckCircle className="w-5 h-5" />}
                        label="Aktivni kolegiji"
                        value={data.activeCourses}
                        variant="success"
                    />
                    <StatCard
                        icon={<FolderKanban className="w-5 h-5" />}
                        label="Razredi"
                        value={data.totalClasses}
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
                </div>
            </div>

            {/* System Health */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('dashboard.systemHealth')}</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="grid sm:grid-cols-3 gap-4">
                        <HealthCard
                            icon={<Database className="w-5 h-5" />}
                            label={t('dashboard.database')}
                            status={data.systemHealth.databaseStatus}
                        />
                        <HealthCard
                            icon={<Cpu className="w-5 h-5" />}
                            label={t('dashboard.mlService')}
                            status={data.systemHealth.mlServiceStatus}
                        />
                        <HealthCard
                            icon={<Code className="w-5 h-5" />}
                            label={t('dashboard.codeExecutor')}
                            status={data.systemHealth.codeExecutorStatus}
                        />
                    </div>
                </CardContent>
            </Card>

            {/* Quick Actions */}
            <Card>
                <CardHeader>
                    <CardTitle>Brze akcije</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
                        <Link to="/users">
                            <QuickActionButton icon={<Users className="w-5 h-5" />} label="Upravljaj korisnicima" />
                        </Link>
                        <Link to="/courses">
                            <QuickActionButton icon={<BookOpen className="w-5 h-5" />} label="Upravljaj kolegijima" />
                        </Link>
                        <Link to="/classes">
                            <QuickActionButton icon={<FolderKanban className="w-5 h-5" />} label="Upravljaj razredima" />
                        </Link>
                        <Link to="/submissions">
                            <QuickActionButton icon={<FileText className="w-5 h-5" />} label="Pregled predaja" />
                        </Link>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}

function StatCard({ 
    icon, 
    label, 
    value,
    variant = 'default'
}: { 
    icon: React.ReactNode; 
    label: string; 
    value: string | number;
    variant?: 'default' | 'success' | 'warning' | 'danger';
}) {
    const variantStyles = {
        default: 'bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400',
        success: 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400',
        warning: 'bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400',
        danger: 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400',
    };

    return (
        <Card>
            <CardContent className="pt-4">
                <div className="flex items-center gap-3">
                    <div className={`p-2 rounded-lg ${variantStyles[variant]}`}>
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

function HealthCard({ 
    icon, 
    label, 
    status 
}: { 
    icon: React.ReactNode; 
    label: string; 
    status: string;
}) {
    const isHealthy = status.toLowerCase() === 'healthy' || status.toLowerCase() === 'up';
    const isWarning = status.toLowerCase() === 'degraded' || status.toLowerCase() === 'warning';

    const StatusIcon = isHealthy ? CheckCircle : isWarning ? AlertCircle : XCircle;
    const statusColor = isHealthy 
        ? 'text-emerald-500' 
        : isWarning 
        ? 'text-amber-500' 
        : 'text-red-500';
    const bgColor = isHealthy
        ? 'bg-emerald-50 dark:bg-emerald-900/20'
        : isWarning
        ? 'bg-amber-50 dark:bg-amber-900/20'
        : 'bg-red-50 dark:bg-red-900/20';

    return (
        <div className={`p-4 rounded-lg ${bgColor}`}>
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <div className="text-zinc-600 dark:text-zinc-400">
                        {icon}
                    </div>
                    <span className="font-medium text-zinc-900 dark:text-white">
                        {label}
                    </span>
                </div>
                <StatusIcon className={`w-5 h-5 ${statusColor}`} />
            </div>
            <p className={`text-sm mt-2 ${statusColor}`}>
                {status}
            </p>
        </div>
    );
}

function QuickActionButton({ icon, label }: { icon: React.ReactNode; label: string }) {
    return (
        <div className="flex items-center gap-3 p-4 rounded-lg border border-zinc-200 dark:border-zinc-700 hover:border-zinc-300 dark:hover:border-zinc-600 hover:bg-zinc-50 dark:hover:bg-zinc-800/50 transition-colors cursor-pointer">
            <div className="p-2 rounded-lg bg-zinc-100 dark:bg-zinc-800 text-zinc-600 dark:text-zinc-400">
                {icon}
            </div>
            <span className="font-medium text-zinc-900 dark:text-white">
                {label}
            </span>
        </div>
    );
}
