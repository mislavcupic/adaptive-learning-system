import { useTranslation } from 'react-i18next';
import { Users, FileText, BarChart3, AlertCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent, Badge } from '../../components';

export const TeacherDashboard = () => {
    const { t } = useTranslation();

    return (
        <div className="space-y-6">
            {/* Header */}
            <div>
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                    {t('dashboard.welcome', { name: 'Profesor' })}
                </h1>
                <p className="text-gray-500 dark:text-gray-400 mt-1">
                    {t('dashboard.classOverview')}
                </p>
            </div>

            {/* Stats cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    icon={<Users className="w-6 h-6 text-indigo-600" />}
                    label={t('nav.students')}
                    value="48"
                />
                <StatCard
                    icon={<FileText className="w-6 h-6 text-green-600" />}
                    label={t('dashboard.pendingSubmissions')}
                    value="12"
                />
                <StatCard
                    icon={<BarChart3 className="w-6 h-6 text-amber-600" />}
                    label={t('analytics.avgMastery')}
                    value="68%"
                />
                <StatCard
                    icon={<AlertCircle className="w-6 h-6 text-red-600" />}
                    label={t('dashboard.commonIssues')}
                    value="3"
                />
            </div>

            {/* Main content */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                    <CardHeader>
                        <CardTitle>{t('analytics.groupComparison')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-4">
                            <GroupStat
                                label={t('groups.experimental')}
                                value={72}
                                color="bg-violet-500"
                            />
                            <GroupStat
                                label={t('groups.control')}
                                value={65}
                                color="bg-indigo-500"
                            />
                        </div>
                    </CardContent>
                </Card>

                <Card>
                    <CardHeader>
                        <CardTitle>{t('dashboard.commonIssues')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-3">
                            <IssueItem
                                skill="Granični uvjeti petlje"
                                count={8}
                                severity="high"
                            />
                            <IssueItem
                                skill="Memory leakovi"
                                count={5}
                                severity="medium"
                            />
                            <IssueItem
                                skill="Null pointer"
                                count={3}
                                severity="low"
                            />
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

// Group stat component
const GroupStat = ({ label, value, color }: { label: string; value: number; color: string }) => (
    <div>
        <div className="flex justify-between text-sm mb-1">
            <span className="text-gray-700 dark:text-gray-300">{label}</span>
            <span className="text-gray-500 dark:text-gray-400">{value}%</span>
        </div>
        <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
            <div
                className={`${color} h-3 rounded-full transition-all`}
                style={{ width: `${value}%` }}
            />
        </div>
    </div>
);

// Issue item component
const IssueItem = ({ skill, count, severity }: { skill: string; count: number; severity: 'high' | 'medium' | 'low' }) => {
    const badgeVariant = severity === 'high' ? 'danger' : severity === 'medium' ? 'warning' : 'info';

    return (
        <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
            <span className="text-gray-700 dark:text-gray-300">{skill}</span>
            <Badge variant={badgeVariant}>{count} učenika</Badge>
        </div>
    );
};