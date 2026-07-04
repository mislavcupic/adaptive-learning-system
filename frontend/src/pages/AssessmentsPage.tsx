import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ClipboardList, Clock, CheckCircle, Lock, PlayCircle, Plus } from 'lucide-react';
import { useAuth } from '../context';
import { assessmentService, courseService } from '../services';
import {
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    Button,
    Badge,
    LoadingScreen,
    ErrorState
} from '../components/ui';
import type { Assessment, AssessmentAttempt, Course } from '../types';

export function AssessmentsPage() {
    const { t } = useTranslation();
    const { user } = useAuth();
    const navigate = useNavigate();

    const [courses, setCourses] = useState<Course[]>([]);
    const [assessmentsByCourse, setAssessmentsByCourse] = useState<Record<string, Assessment[]>>({});
    const [myAttempts, setMyAttempts] = useState<AssessmentAttempt[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            setLoading(true);

            const coursesData = await courseService.getAll();
            setCourses(coursesData);

            if (user?.role === 'STUDENT') {
                const attemptsData = await assessmentService.getMyAttempts();
                setMyAttempts(attemptsData);
            }

            const assessmentsMap: Record<string, Assessment[]> = {};
            for (const course of coursesData) {
                try {
                    const assessments = await assessmentService.getByCourse(course.id);
                    if (assessments.length > 0) {
                        assessmentsMap[course.id] = assessments;
                    }
                } catch {
                    // Course might not have assessments
                }
            }
            setAssessmentsByCourse(assessmentsMap);

        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setLoading(false);
        }
    };

    const getAttemptForAssessment = (assessmentId: string): AssessmentAttempt | undefined => {
        return myAttempts.find(a => a.assessmentId === assessmentId && a.isCompleted);
    };

    const canTakeAssessment = (assessment: Assessment): boolean => {
        const attempt = getAttemptForAssessment(assessment.id);
        return !attempt;
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={loadData} />;

    const coursesWithAssessments = courses.filter(c => assessmentsByCourse[c.id]?.length > 0);

    return (
        <div className="space-y-6 animate-fade-in">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {t('assessments.title')}
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                        {t('assessments.subtitle')}
                    </p>
                </div>
                {user && ['TEACHER', 'ADMIN'].includes(user.role) && (
                    <Button onClick={() => navigate('/assessments/new')} className="gap-2">
                        <Plus className="w-4 h-4" />
                        {t('assessments.newAssessment')}
                    </Button>
                )}
            </div>

            {user?.role === 'STUDENT' && myAttempts.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle className="text-lg">{t('assessments.myResults')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                            {myAttempts.filter(a => a.isCompleted).map(attempt => (
                                <div
                                    key={attempt.id}
                                    className={`p-4 rounded-lg border ${
                                        attempt.passed
                                            ? 'border-green-200 bg-green-50 dark:border-green-800 dark:bg-green-900/20'
                                            : 'border-red-200 bg-red-50 dark:border-red-800 dark:bg-red-900/20'
                                    }`}
                                >
                                    <div className="flex items-center justify-between mb-2">
                                        <span className="font-medium text-zinc-900 dark:text-white">
                                            {attempt.assessmentTitle}
                                        </span>
                                        {attempt.passed ? (
                                            <CheckCircle className="w-5 h-5 text-green-600" />
                                        ) : (
                                            <Lock className="w-5 h-5 text-red-600" />
                                        )}
                                    </div>
                                    <div className="flex items-center justify-between text-sm">
                                        <span className="text-zinc-500">
                                            {attempt.score}/{attempt.maxScore} {t('assessments.points')}
                                        </span>
                                        <Badge variant={attempt.passed ? 'success' : 'danger'}>
                                            {attempt.percentage.toFixed(0)}%
                                        </Badge>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </CardContent>
                </Card>
            )}

            {coursesWithAssessments.length === 0 ? (
                <Card>
                    <CardContent className="py-12 text-center">
                        <ClipboardList className="w-12 h-12 mx-auto text-zinc-300 dark:text-zinc-600 mb-4" />
                        <p className="text-zinc-500 dark:text-zinc-400">
                            {t('assessments.noAssessments')}
                        </p>
                    </CardContent>
                </Card>
            ) : (
                <div className="space-y-6">
                    {coursesWithAssessments.map(course => (
                        <Card key={course.id}>
                            <CardHeader>
                                <CardTitle>{course.name}</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <div className="space-y-4">
                                    {assessmentsByCourse[course.id]?.map(assessment => {
                                        const attempt = getAttemptForAssessment(assessment.id);
                                        const canTake = canTakeAssessment(assessment);

                                        return (
                                            <div
                                                key={assessment.id}
                                                className="flex items-center justify-between p-4 rounded-lg border border-zinc-200 dark:border-zinc-700"
                                            >
                                                <div className="flex items-center gap-4">
                                                    <div className={`p-3 rounded-lg ${
                                                        assessment.assessmentType === 'PRETEST'
                                                            ? 'bg-amber-100 dark:bg-amber-900/30'
                                                            : 'bg-emerald-100 dark:bg-emerald-900/30'
                                                    }`}>
                                                        <ClipboardList className={`w-6 h-6 ${
                                                            assessment.assessmentType === 'PRETEST'
                                                                ? 'text-amber-600 dark:text-amber-400'
                                                                : 'text-emerald-600 dark:text-emerald-400'
                                                        }`} />
                                                    </div>
                                                    <div>
                                                        <h3 className="font-medium text-zinc-900 dark:text-white">
                                                            {assessment.title}
                                                        </h3>
                                                        <div className="flex items-center gap-3 mt-1 text-sm text-zinc-500">
                                                            <Badge variant={assessment.assessmentType === 'PRETEST' ? 'warning' : 'success'}>
                                                                {assessment.assessmentType === 'PRETEST'
                                                                    ? t('assessments.pretest')
                                                                    : t('assessments.posttest')}
                                                            </Badge>
                                                            <span>{assessment.questionCount} {t('assessments.questions')}</span>
                                                            {assessment.timeLimitMinutes && (
                                                                <span className="flex items-center gap-1">
                                                                    <Clock className="w-4 h-4" />
                                                                    {assessment.timeLimitMinutes} min
                                                                </span>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>

                                                <div className="flex items-center gap-3">
                                                    {user?.role === 'STUDENT' ? (
                                                        attempt ? (
                                                            <div className="text-right">
                                                                <Badge variant={attempt.passed ? 'success' : 'danger'}>
                                                                    {attempt.passed ? t('assessments.passed') : t('assessments.notPassed')}
                                                                </Badge>
                                                                <p className="text-sm text-zinc-500 mt-1">
                                                                    {attempt.score}/{attempt.maxScore} ({attempt.percentage.toFixed(0)}%)
                                                                </p>
                                                            </div>
                                                        ) : (
                                                            <Button
                                                                onClick={() => navigate(`/assessments/${assessment.id}/solve`)}
                                                                disabled={!canTake}
                                                                className="gap-2"
                                                            >
                                                                <PlayCircle className="w-4 h-4" />
                                                                {t('assessments.startTest')}
                                                            </Button>
                                                        )
                                                    ) : (
                                                        <Button
                                                            variant="secondary"
                                                            onClick={() => navigate(`/assessments/${assessment.id}/edit`)}
                                                        >
                                                            {t('common.edit')}
                                                        </Button>
                                                    )}
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            )}
        </div>
    );
}