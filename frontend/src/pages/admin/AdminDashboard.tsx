import { useTranslation } from 'react-i18next';
import { Users, ClipboardList, BarChart3, Settings } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../../components';

export const AdminDashboard = () => {
    const { t } = useTranslation();

    return (
        <div className="space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                    {t('dashboard.welcome', { name: 'Admin' })}
                </h1>
                <p className="text-gray-500 dark:text-gray-400 mt-1">
                    Administracija sustava
                </p>
            </div>

            {/* Stats cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    icon={<Users className="w-6 h-6 text-indigo-600" />}
                    label={t('users.title')}
                    value="156"
                />
                <StatCard
                    icon={<ClipboardList className="w-6 h-6 text-green-600" />}
                    label={t('surveys.title')}
                    value="4"
                />
                <StatCard
                    icon={<BarChart3 className="w-6 h-6 text-amber-600" />}
                    label={t('surveys.responses')}
                    value="89"
                />
                <StatCard
                    icon={<Settings className="w-6 h-6 text-blue-600" />}
                    label={t('nav.settings')}
                    value="OK"
                />
            </div>

            {/* Main content */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                    <CardHeader>
                        <CardTitle>{t('surveys.title')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-gray-500 dark:text-gray-400">
                            {t('surveys.noSurveys')}
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle>Statistika sustava</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-3">
                            <div className="flex justify-between">
                                <span className="text-gray-600 dark:text-gray-400">Nastavnika</span>
                                <span className="font-medium text-gray-900 dark:text-white">8</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-gray-600 dark:text-gray-400">Učenika</span>
                                <span className="font-medium text-gray-900 dark:text-white">148</span>
                            </div>
                            <div className="flex justify-between">
                                <span className="text-gray-600 dark:text-gray-400">Razreda</span>
                                <span className="font-medium text-gray-900 dark:text-white">6</span>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

// Stat card component
const StatCard = ({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) => (
    <Card>
        <CardContent>
            <div className="flex items-center gap-4">
                <div className="p-3 bg-gray-100 dark:bg-gray-700 rounded-lg">
                    {icon}
                </div>
                <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
                </div>
            </div>
        </CardContent>
    </Card>
);