import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { 
    Users, BookOpen, ClipboardList, FileText, Clock, CheckCircle, ChevronRight, TrendingUp
} from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../../components';
import { dashboardService } from '../../services';
import type { TeacherDashboardData, Submission, StudentProgressSummary, Course } from '../../types';

export const TeacherDashboard = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [data, setData] = useState<TeacherDashboardData | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchDashboard = async () => {
            try {
                setLoading(true);
                const response = await dashboardService.getTeacherDashboard();
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

    const teacherName = `${data.teacher.firstName} ${data.teacher.lastName}`;

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                    {t('dashboard.welcome', 'Dobrodošli')}, {teacherName}
                </h1>
                <p className="text-gray-500 dark:text-gray-400 mt-1">
                    Pregledajte napredak studenata i upravljajte kolegijima
                </p>
            </div>

            {/* Stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
                <StatCard
                    icon={<Users className="w-6 h-6 text-violet-600" />}
                    label="Studenata"
                    value={data.totalStudents}
                />
                <StatCard
                    icon={<BookOpen className="w-6 h-6 text-cyan-600" />}
                    label="Kolegija"
                    value={data.totalCourses}
                />
                <StatCard
                    icon={<ClipboardList className="w-6 h-6 text-green-600" />}
                    label="Zadataka"
                    value={data.totalTasks}
                />
                <StatCard
                    icon={<FileText className="w-6 h-6 text-blue-600" />}
                    label="Predaja"
                    value={data.totalSubmissions}
                />
                <StatCard
                    icon={<Clock className="w-6 h-6 text-amber-600" />}
                    label="Čeka pregled"
                    value={data.pendingReviews}
                    highlight={data.pendingReviews > 0}
                />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Student progress */}
                <Card>
                    <CardHeader className="flex flex-row items-center justify-between">
                        <CardTitle>Napredak studenata</CardTitle>
                        <button 
                            onClick={() => navigate('/students')}
                            className="text-sm text-violet-600 hover:text-violet-700 flex items-center gap-1"
                        >
                            Vidi sve <ChevronRight className="w-4 h-4" />
                        </button>
                    </CardHeader>
                    <CardContent>
                        {data.studentProgress.length > 0 ? (
                            <div className="space-y-3">
                                {data.studentProgress.slice(0, 5).map((student) => (
                                    <StudentProgressRow 
                                        key={student.studentId} 
                                        student={student}
                                        onClick={() => navigate(`/students/${student.studentId}`)}
                                    />
                                ))}
                            </div>
                        ) : (
                            <p className="text-gray-500 text-center py-4">
                                Još nema studenata
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
                                    <SubmissionRow 
                                        key={sub.id} 
                                        submission={sub}
                                        onClick={() => navigate(`/submissions/${sub.id}`)}
                                    />
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
                    {data.courses.length > 0 ? (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                            {data.courses.map((course) => (
                                <div 
                                    key={course.id}
                                    onClick={() => navigate(`/courses/${course.id}`)}
                                    className="p-4 border border-gray-200 dark:border-gray-700 rounded-lg hover:border-violet-500 cursor-pointer transition-colors"
                                >
                                    <div className="flex items-center gap-3 mb-2">
                                        <div className="p-2 bg-violet-100 dark:bg-violet-900 rounded-lg">
                                            <BookOpen className="w-5 h-5 text-violet-600" />
                                        </div>
                                        <div>
                                            <p className="font-medium text-gray-900 dark:text-white">{course.name}</p>
                                            <p className="text-sm text-gray-500">{course.languageType}</p>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-4 text-sm text-gray-500">
                                        <span>{course.outcomeCount} ishoda</span>
                                        <span className={`px-2 py-0.5 rounded ${
                                            course.isActive 
                                                ? 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300'
                                                : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                                        }`}>
                                            {course.isActive ? 'Aktivan' : 'Neaktivan'}
                                        </span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <p className="text-gray-500 text-center py-4">
                            Nemate kreiranih kolegija
                        </p>
                    )}
                </CardContent>
            </Card>
        </div>
    );
};

// Komponente
const StatCard = ({ icon, label, value, highlight = false }: { 
    icon: React.ReactNode; 
    label: string; 
    value: number;
    highlight?: boolean;
}) => (
    <Card className={highlight ? 'border-amber-500' : ''}>
        <CardContent>
            <div className="flex items-center gap-4">
                <div className="p-3 bg-gray-100 dark:bg-gray-700 rounded-lg">{icon}</div>
                <div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
                    <p className={`text-2xl font-bold ${
                        highlight ? 'text-amber-600' : 'text-gray-900 dark:text-white'
                    }`}>
                        {value}
                    </p>
                </div>
            </div>
        </CardContent>
    </Card>
);

const StudentProgressRow = ({ student, onClick }: { 
    student: StudentProgress; 
    onClick: () => void;
}) => {
    const masteryPct = Math.round(student.averageMastery * 100);
    
    return (
        <div 
            onClick={onClick}
            className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
        >
            <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-violet-100 dark:bg-violet-900 rounded-full flex items-center justify-center">
                    <span className="text-violet-600 font-medium">
                        {student.studentName.charAt(0).toUpperCase()}
                    </span>
                </div>
                <div>
                    <p className="font-medium text-gray-900 dark:text-white text-sm">
                        {student.studentName}
                    </p>
                    <p className="text-xs text-gray-500">
                        {student.totalSubmissions} predaja
                    </p>
                </div>
            </div>
            <div className="flex items-center gap-2">
                <TrendingUp className={`w-4 h-4 ${
                    masteryPct >= 70 ? 'text-green-500' : 
                    masteryPct >= 40 ? 'text-amber-500' : 'text-red-500'
                }`} />
                <span className="font-medium text-gray-900 dark:text-white">
                    {masteryPct}%
                </span>
            </div>
        </div>
    );
};

const SubmissionRow = ({ submission, onClick }: { 
    submission: Submission; 
    onClick: () => void;
}) => {
    const isCompleted = submission.status === 'COMPLETED';
    
    return (
        <div 
            onClick={onClick}
            className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
        >
            <div className="flex items-center gap-3">
                <div className={`p-2 rounded-lg ${
                    isCompleted ? 'bg-green-100 dark:bg-green-900' : 'bg-amber-100 dark:bg-amber-900'
                }`}>
                    {isCompleted 
                        ? <CheckCircle className="w-4 h-4 text-green-500" />
                        : <Clock className="w-4 h-4 text-amber-500" />
                    }
                </div>
                <div>
                    <p className="font-medium text-gray-900 dark:text-white text-sm">
                        {submission.taskTitle}
                    </p>
                    <p className="text-xs text-gray-500">
                        {submission.studentName} • {new Date(submission.createdAt).toLocaleDateString('hr-HR')}
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
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
            {[...Array(5)].map((_, i) => (
                <div key={i} className="h-24 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
            ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="h-64 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
            <div className="h-64 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
        </div>
    </div>
);

export default TeacherDashboard;
