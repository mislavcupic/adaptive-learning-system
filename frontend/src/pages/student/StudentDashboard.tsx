import { useTranslation } from 'react-i18next';
import { BookOpen, TrendingUp, Clock, Target } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../../components';

export const StudentDashboard = () => {
    const { t } = useTranslation();

    return (
        <div className="space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                    {t('dashboard.welcome', { name: 'Marko' })}
                </h1>
                <p className="text-gray-500 dark:text-gray-400 mt-1">
                    {t('dashboard.continuelearning')}
                </p>
            </div>

            {/* Stats cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    icon={<BookOpen className="w-6 h-6 text-indigo-600" />}
                    label={t('nav.courses')}
                    value="3"
                />
                <StatCard
                    icon={<Target className="w-6 h-6 text-green-600" />}
                    label={t('submissions.title')}
                    value="24"
                />
                <StatCard
                    icon={<TrendingUp className="w-6 h-6 text-amber-600" />}
                    label={t('progress.masteryLevel')}
                    value="72%"
                />
                <StatCard
                    icon={<Clock className="w-6 h-6 text-blue-600" />}
                    label={t('dashboard.upcomingTasks')}
                    value="5"
                />
            </div>

            {/* Main content */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                    <CardHeader>
                        <CardTitle>{t('dashboard.upcomingTasks')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-gray-500 dark:text-gray-400">
                            {t('dashboard.noUpcomingTasks')}
                        </p>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle>{t('dashboard.skillsToFocus')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-3">
                            <SkillProgress name="Petlje" progress={45} />
                            <SkillProgress name="Nizovi" progress={62} />
                            <SkillProgress name="Funkcije" progress={78} />
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

// Skill progress component
const SkillProgress = ({ name, progress }: { name: string; progress: number }) => (
    <div>
        <div className="flex justify-between text-sm mb-1">
            <span className="text-gray-700 dark:text-gray-300">{name}</span>
            <span className="text-gray-500 dark:text-gray-400">{progress}%</span>
        </div>
        <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
            <div
                className="bg-indigo-600 h-2 rounded-full transition-all"
                style={{ width: `${progress}%` }}
            />
        </div>
    </div>
);