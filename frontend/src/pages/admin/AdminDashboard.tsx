import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Users, BookOpen, GraduationCap, ClipboardList, Server, Database, Cpu } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../../components';
import { dashboardService } from '../../services';
import type { AdminDashboardData } from '../../types';

export const AdminDashboard = () => {
    const { t } = useTranslation();
    const [data, setData] = useState<AdminDashboardData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchDashboard = async () => {
            try {
                setLoading(true);
                const response = await dashboardService.getAdminDashboard();
                setData(response);
                setError(null);
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to load dashboard');
            } finally {
                setLoading(false);
            }
        };

        fetchDashboard();
    }, []);

    if (loading) return <DashboardSkeleton />;

    if (error) {
        return (
            <div className="p-6 text-center">
                <p className="text-red-500">{error}</p>
                <button 
                    onClick={() => window.location.reload()}
                    className="mt-4 px-4 py-2 bg-violet-600 text-white rounded-lg hover:bg-violet-700"
                >
                    {t('common.retry', 'Pokušaj ponovno')}
                </button>
            </div>
        );
    }

    if (!data) return null;

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                    {t('dashboard.admin.title', 'Admin Dashboard')}
                </h1>
                <p className="text-gray-500 dark:text-gray-400 mt-1">
                    {t('dashboard.admin.subtitle', 'Pregled sustava i statistike')}
                </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    icon={<Users className="w-6 h-6 text-violet-600" />}
                    label={t('dashboard.admin.totalUsers', 'Ukupno korisnika')}
                    value={data.totalUsers}
                    subtitle={`${data.activeUsers} aktivnih`}
                />
                <StatCard
                    icon={<GraduationCap className="w-6 h-6 text-cyan-600" />}
                    label={t('dashboard.admin.students', 'Studenata')}
                    value={data.totalStudents}
                />
                <StatCard
                    icon={<Users className="w-6 h-6 text-green-600" />}
                    label={t('dashboard.admin.teachers', 'Nastavnika')}
                    value={data.totalTeachers}
                />
                <StatCard
                    icon={<BookOpen className="w-6 h-6 text-amber-600" />}
                    label={t('dashboard.admin.courses', 'Kolegija')}
                    value={data.totalCourses}
                    subtitle={`${data.activeCourses} aktivnih`}
                />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <StatCard
                    icon={<Users className="w-6 h-6 text-indigo-600" />}
                    label={t('dashboard.admin.classes', 'Razreda')}
                    value={data.totalClasses}
                />
                <StatCard
                    icon={<ClipboardList className="w-6 h-6 text-pink-600" />}
                    label={t('dashboard.admin.tasks', 'Zadataka')}
                    value={data.totalTasks}
                />
                <StatCard
                    icon={<ClipboardList className="w-6 h-6 text-blue-600" />}
                    label={t('dashboard.admin.submissions', 'Predaja')}
                    value={data.totalSubmissions}
                />
            </div>

            <Card>
                <CardHeader>
                    <CardTitle>{t('dashboard.admin.systemHealth', 'Stanje sustava')}</CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <HealthIndicator
                            icon={<Database className="w-5 h-5" />}
                            label="Baza podataka"
                            status={data.systemHealth.databaseStatus}
                        />
                        <HealthIndicator
                            icon={<Cpu className="w-5 h-5" />}
                            label="ML Servis"
                            status={data.systemHealth.mlServiceStatus}
                        />
                        <HealthIndicator
                            icon={<Server className="w-5 h-5" />}
                            label="Code Executor"
                            status={data.systemHealth.codeExecutorStatus}
                        />
                    </div>
                </CardContent>
            </Card>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                    <CardHeader>
                        <CardTitle>Pregled korisnika</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-3">
                            <StatRow label="Administratora" value={data.totalAdmins} />
                            <StatRow label="Nastavnika" value={data.totalTeachers} />
                            <StatRow label="Studenata" value={data.totalStudents} />
                        </div>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle>Statistika sadržaja</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-3">
                            <StatRow label="Ukupno kolegija" value={data.totalCourses} />
                            <StatRow label="Aktivnih kolegija" value={data.activeCourses} />
                            <StatRow label="Ukupno zadataka" value={data.totalTasks} />
                            <StatRow label="Ukupno predaja" value={data.totalSubmissions} />
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

// Komponente
const StatCard = ({ icon, label, value, subtitle }: { 
    icon: React.ReactNode; 
    label: string; 
    value: number; 
    subtitle?: string;
}) => (
    <Card>
        <CardContent>
            <div className="flex items-center gap-4">
                <div className="p-3 bg-gray-100 dark:bg-gray-700 rounded-lg">{icon}</div>
                <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
                    {subtitle && <p className="text-xs text-gray-400">{subtitle}</p>}
                </div>
            </div>
        </CardContent>
    </Card>
);

const HealthIndicator = ({ icon, label, status }: { 
    icon: React.ReactNode; 
    label: string; 
    status: 'UP' | 'DOWN';
}) => (
    <div className="flex items-center gap-3 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
        <div className={status === 'UP' ? 'text-green-500' : 'text-red-500'}>{icon}</div>
        <div className="flex-1">
            <p className="text-sm font-medium text-gray-900 dark:text-white">{label}</p>
        </div>
        <div className={`px-2 py-1 rounded text-xs font-medium ${
            status === 'UP' 
                ? 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300' 
                : 'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300'
        }`}>
            {status}
        </div>
    </div>
);

const StatRow = ({ label, value }: { label: string; value: number }) => (
    <div className="flex justify-between">
        <span className="text-gray-600 dark:text-gray-400">{label}</span>
        <span className="font-medium text-gray-900 dark:text-white">{value}</span>
    </div>
);

const DashboardSkeleton = () => (
    <div className="space-y-6 animate-pulse">
        <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-1/3"></div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {[...Array(4)].map((_, i) => (
                <div key={i} className="h-24 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
            ))}
        </div>
    </div>
);

export default AdminDashboard;
