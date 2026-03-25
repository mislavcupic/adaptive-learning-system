import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { 
    BookOpen, ClipboardList, TrendingUp, Clock, CheckCircle, AlertCircle, ChevronRight
} from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../../components';
import { dashboardService } from '../../services';
import type { StudentDashboardData, Submission, SkillMastery, Course } from '../../types';

export const StudentDashboard = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [data, setData] = useState<StudentDashboardData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchDashboard = async () => {
            try {
                setLoading(true);
                const response = await dashboardService.getStudentDashboard();
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
                    Pokušaj ponovno
                </button>
            </div>
        );
    }

    if (!data) return null;

    const studentName = `${data.student.firstName} ${data.student.lastName}`;

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                    {t('dashboard.welcome', 'Dobrodošli')}, {studentName}
                </h1>
                <p className="text-gray-500 dark:text-gray-400 mt-1">
                    Pregledajte svoj napredak i zadatke
                </p>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                    icon={<ClipboardList className="w-6 h-6 text-violet-600" />}
                    label="Ukupno predaja"
                    value={data.totalSubmissions}
                />
                <StatCard
                    icon={<CheckCircle className="w-6 h-6 text-green-600" />}
                    label="Riješenih zadataka"
                    value={data.completedTasks}
                />
                <StatCard
                    icon={<Clock className="w-6 h-6 text-amber-600" />}
                    label="Preostalih zadataka"
                    value={data.pendingTasks}
                />
                <StatCard
                    icon={<TrendingUp className="w-6 h-6 text-cyan-600" />}
                    label="Prosječna razina"
                    value={`${Math.round(data.averageMastery * 100)}%`}
                    isText
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Skills */}
                <Card>
                    <CardHeader>
                        <CardTitle>Razina vještina</CardTitle>
                    </CardHeader>
                    <CardContent>
                        {data.skillMasteries.length > 0 ? (
                            <div className="space-y-4">
                                {data.skillMasteries.slice(0, 5).map((skill) => (
                                    <SkillBar key={skill.id} skill={skill} />
                                ))}
                            </div>
                        ) : (
                            <p className="text-gray-500 text-center py-4">
                                Još nema podataka o vještinama
                            </p>
                        )}
                    </CardContent>
                </Card>

                {/* Recent submissions */}
                <Card>
                    <CardHeader className="flex flex-row items-center justify-between">
                        <CardTitle>Nedavne predaje</CardTitle>
                        <button 
                            onClick={() => navigate('/submissions')}
                            className="text-sm text-violet-600 hover:text-violet-700 flex items-center gap-1"
                        >
                            Vidi sve <ChevronRight className="w-4 h-4" />
                        </button>
                    </CardHeader>
                    <CardContent>
                        {data.recentSubmissions.length > 0 ? (
                            <div className="space-y-3">
                                {data.recentSubmissions.slice(0, 5).map((sub) => (
                                    <SubmissionRow key={sub.id} submission={sub} />
                                ))}
                            </div>
                        ) : (
                            <p className="text-gray-500 text-center py-4">
                                Još nema predaja
                            </p>
                        )}
                    </CardContent>
                </Card>
            </div>

            {/* Courses */}
            <Card>
                <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle>Moji kolegiji</CardTitle>
                    <button 
                        onClick={() => navigate('/courses')}
                        className="text-sm text-violet-600 hover:text-violet-700 flex items-center gap-1"
                    >
                        Vidi sve <ChevronRight className="w-4 h-4" />
                    </button>
                </CardHeader>
                <CardContent>
                    {data.enrolledCourses.length > 0 ? (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {data.enrolledCourses.map((course) => (
                                <div 
                                    key={course.id}
                                    onClick={() => navigate(`/courses/${course.id}`)}
                                    className="p-4 border border-gray-200 dark:border-gray-700 rounded-lg hover:border-violet-500 cursor-pointer transition-colors"
                                >
                                    <div className="flex items-center gap-3">
                                        <div className="p-2 bg-violet-100 dark:bg-violet-900 rounded-lg">
                                            <BookOpen className="w-5 h-5 text-violet-600" />
                                        </div>
                                        <div>
                                            <p className="font-medium text-gray-900 dark:text-white">{course.name}</p>
                                            <p className="text-sm text-gray-500">{course.languageType}</p>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <p className="text-gray-500 text-center py-4">
                            Niste upisani ni u jedan kolegij
                        </p>
                    )}
                </CardContent>
            </Card>
        </div>
    );
};

// Komponente
const StatCard = ({ icon, label, value, isText = false }: { 
    icon: React.ReactNode; 
    label: string; 
    value: number | string; 
    isText?: boolean;
}) => (
    <Card>
        <CardContent>
            <div className="flex items-center gap-4">
                <div className="p-3 bg-gray-100 dark:bg-gray-700 rounded-lg">{icon}</div>
                <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
                    <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
                </div>
            </div>
        </CardContent>
    </Card>
);

const SkillBar = ({ skill }: { skill: SkillMastery }) => {
    const percentage = Math.round(skill.masteryLevel * 100);
    return (
        <div>
            <div className="flex justify-between mb-1">
                <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    {skill.skillName}
                </span>
                <span className="text-sm text-gray-500">{percentage}%</span>
            </div>
            <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                <div 
                    className={`h-2 rounded-full transition-all ${
                        percentage >= 80 ? 'bg-green-500' :
                        percentage >= 50 ? 'bg-amber-500' : 'bg-red-500'
                    }`}
                    style={{ width: `${percentage}%` }}
                />
            </div>
        </div>
    );
};

const SubmissionRow = ({ submission }: { submission: Submission }) => {
    const isSuccess = submission.status === 'COMPLETED';
    const isPending = submission.status === 'PENDING';
    
    return (
        <div className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
            <div className="flex items-center gap-3">
                <div className={`p-2 rounded-lg ${
                    isSuccess ? 'bg-green-100 dark:bg-green-900' : 
                    isPending ? 'bg-amber-100 dark:bg-amber-900' : 
                    'bg-red-100 dark:bg-red-900'
                }`}>
                    {isSuccess ? <CheckCircle className="w-4 h-4 text-green-500" /> :
                     isPending ? <Clock className="w-4 h-4 text-amber-500" /> :
                     <AlertCircle className="w-4 h-4 text-red-500" />}
                </div>
                <div>
                    <p className="font-medium text-gray-900 dark:text-white text-sm">
                        {submission.taskTitle}
                    </p>
                    <p className="text-xs text-gray-500">
                        {new Date(submission.createdAt).toLocaleDateString('hr-HR')}
                    </p>
                </div>
            </div>
            {submission.score !== null && (
                <span className="font-medium text-gray-900 dark:text-white">
                    {submission.score}%
                </span>
            )}
        </div>
    );
};

const DashboardSkeleton = () => (
    <div className="space-y-6 animate-pulse">
        <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-1/3"></div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {[...Array(4)].map((_, i) => (
                <div key={i} className="h-24 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
            ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="h-64 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
            <div className="h-64 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
        </div>
    </div>
);

export default StudentDashboard;
