import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    Plus, FileText, Clock, Award, Eye, Send, EyeOff,
    Trash2, Users, PlayCircle, Pencil
} from 'lucide-react';
import { useAuth } from '../context';
import { examService, courseService } from '../services';
import {
    Card, CardContent, Button, Badge,
    LoadingScreen, ErrorState, EmptyState
} from '../components/ui';
import type { Exam, Course } from '../types';

const fmtDate = (iso: string | null) => {
    if (!iso) return null;
    return new Date(iso).toLocaleString('hr-HR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
};

export function ExamsPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { user } = useAuth();

    const isTeacher = user?.role === 'TEACHER' || user?.role === 'ADMIN';

    const [exams, setExams] = useState<Exam[]>([]);
    const [courses, setCourses] = useState<Course[]>([]);
    const [courseId, setCourseId] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const statusLabel = (status: string) => {
        const map: Record<string, string> = {
            IN_PROGRESS: t('exams.status.inProgress'),
            SUBMITTED: t('exams.status.submitted'),
            EXPIRED: t('exams.status.expired'),
            GRADED: t('exams.status.graded'),
            REVIEWED: t('exams.status.reviewed'),
        };
        return map[status] ?? status;
    };

    useEffect(() => {
        init();
    }, []);

    useEffect(() => {
        if (!isTeacher && courseId) {
            loadStudentExams(courseId);
        }
    }, [courseId]);

    const init = async () => {
        try {
            setLoading(true);
            setError(null);

            const courseList = await courseService.getAll();
            setCourses(courseList);

            if (isTeacher) {
                setExams(await examService.getMyExams());
            } else if (courseList.length > 0) {
                setCourseId(courseList[0].id);
                setExams(await examService.getAvailable(courseList[0].id));
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setLoading(false);
        }
    };

    const loadStudentExams = async (id: string) => {
        try {
            setExams(await examService.getAvailable(id));
        } catch {
            setExams([]);
        }
    };

    const refresh = async () => {
        if (isTeacher) {
            setExams(await examService.getMyExams());
        } else if (courseId) {
            await loadStudentExams(courseId);
        }
    };

    const handlePublish = async (exam: Exam) => {
        try {
            if (exam.isPublished) {
                await examService.unpublish(exam.id);
            } else {
                await examService.publish(exam.id);
            }
            await refresh();
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        }
    };

    const handleDelete = async (exam: Exam) => {
        if (!confirm(t('exams.deleteConfirm', { title: exam.title }))) return;
        try {
            await examService.delete(exam.id);
            await refresh();
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        }
    };

    if (loading) return <LoadingScreen />;
    if (error && exams.length === 0) {
        return <ErrorState description={error} onRetry={init} />;
    }

    return (
        <div className="space-y-6 animate-fade-in">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {t('exams.title')}
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                        {isTeacher ? t('exams.subtitleTeacher') : t('exams.subtitleStudent')}
                    </p>
                </div>

                {isTeacher && (
                    <Button onClick={() => navigate('/exams/new')} className="gap-2">
                        <Plus className="w-4 h-4" />
                        {t('exams.newExam')}
                    </Button>
                )}
            </div>

            {error && (
                <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
                    <p className="text-sm text-red-700 dark:text-red-300">{error}</p>
                </div>
            )}

            {!isTeacher && courses.length > 1 && (
                <div>
                    <label className="block text-xs font-medium text-zinc-500 mb-1">
                        {t('exams.course')}
                    </label>
                    <select
                        value={courseId}
                        onChange={(e) => setCourseId(e.target.value)}
                        className="px-3 py-2 text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                    >
                        {courses.map(c => (
                            <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                    </select>
                </div>
            )}

            {exams.length === 0 ? (
                <EmptyState
                    title={t('exams.noExams')}
                    description={isTeacher
                        ? t('exams.noExamsTeacher')
                        : t('exams.noExamsStudent')}
                />
            ) : (
                <div className="grid gap-4">
                    {exams.map(exam => (
                        <Card key={exam.id}>
                            <CardContent className="py-5">
                                <div className="flex items-start justify-between gap-4 flex-wrap">
                                    <div className="min-w-0 flex-1">
                                        <div className="flex items-center gap-2 flex-wrap">
                                            <h2 className="font-medium text-zinc-900 dark:text-white">
                                                {exam.title}
                                            </h2>

                                            {isTeacher && (
                                                <Badge variant={exam.isPublished ? 'success' : 'default'}>
                                                    {exam.isPublished
                                                        ? t('exams.published')
                                                        : t('exams.draft')}
                                                </Badge>
                                            )}

                                            {!isTeacher && exam.myAttemptStatus && (
                                                <Badge variant={
                                                    exam.myAttemptStatus === 'IN_PROGRESS' ? 'warning' : 'success'
                                                }>
                                                    {statusLabel(exam.myAttemptStatus)}
                                                </Badge>
                                            )}
                                        </div>

                                        {exam.description && (
                                            <p className="text-sm text-zinc-500 mt-1.5">
                                                {exam.description}
                                            </p>
                                        )}

                                        <div className="flex items-center gap-4 mt-3 text-xs text-zinc-500 flex-wrap">
                                            <span className="flex items-center gap-1">
                                                <FileText className="w-3.5 h-3.5" />
                                                {exam.taskCount} {t('exams.tasks')}
                                            </span>
                                            <span className="flex items-center gap-1">
                                                <Award className="w-3.5 h-3.5" />
                                                {exam.maxScore} {t('exams.points')}
                                            </span>
                                            {exam.timeLimitMinutes && (
                                                <span className="flex items-center gap-1">
                                                    <Clock className="w-3.5 h-3.5" />
                                                    {exam.timeLimitMinutes} {t('exams.minutes')}
                                                </span>
                                            )}
                                            <span>{exam.courseName}</span>
                                        </div>

                                        {(exam.availableFrom || exam.availableUntil) && (
                                            <p className="text-xs text-zinc-400 mt-2">
                                                {exam.availableFrom &&
                                                    `${t('exams.from')} ${fmtDate(exam.availableFrom)}`}
                                                {exam.availableFrom && exam.availableUntil && ' · '}
                                                {exam.availableUntil &&
                                                    `${t('exams.to')} ${fmtDate(exam.availableUntil)}`}
                                            </p>
                                        )}
                                    </div>

                                    <div className="flex gap-2 shrink-0">
                                        {isTeacher ? (
                                            <>
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    onClick={() => navigate(`/exams/${exam.id}/attempts`)}
                                                    className="gap-1.5"
                                                >
                                                    <Users className="w-4 h-4" />
                                                    {t('exams.attempts')}
                                                </Button>
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    onClick={() => navigate(`/exams/${exam.id}/edit`)}
                                                    className="gap-1.5"
                                                >
                                                    <Pencil className="w-4 h-4" />
                                                    {t('exams.edit')}
                                                </Button>
                                                <Button
                                                    variant="secondary"
                                                    size="sm"
                                                    onClick={() => handlePublish(exam)}
                                                    className="gap-1.5"
                                                >
                                                    {exam.isPublished
                                                        ? <><EyeOff className="w-4 h-4" />{t('exams.unpublish')}</>
                                                        : <><Send className="w-4 h-4" />{t('exams.publish')}</>}
                                                </Button>
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    onClick={() => handleDelete(exam)}
                                                    className="text-red-500"
                                                >
                                                    <Trash2 className="w-4 h-4" />
                                                </Button>
                                            </>
                                        ) : (
                                            <Button
                                                onClick={() => navigate(`/exams/${exam.id}/solve`)}
                                                className="gap-2"
                                                variant={exam.myAttemptStatus && exam.myAttemptStatus !== 'IN_PROGRESS'
                                                    ? 'secondary' : 'primary'}
                                            >
                                                {!exam.myAttemptStatus && (
                                                    <><PlayCircle className="w-4 h-4" />{t('exams.start')}</>
                                                )}
                                                {exam.myAttemptStatus === 'IN_PROGRESS' && (
                                                    <><PlayCircle className="w-4 h-4" />{t('exams.continue')}</>
                                                )}
                                                {exam.myAttemptStatus && exam.myAttemptStatus !== 'IN_PROGRESS' && (
                                                    <><Eye className="w-4 h-4" />{t('exams.viewResult')}</>)}
                                            </Button>
                                        )}
                                    </div>
                                </div>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            )}
        </div>
    );
}