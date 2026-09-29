import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    ArrowLeft, ChevronDown, ChevronRight, Award,
    CheckCircle2, XCircle, Bot, Save, Download, Users
} from 'lucide-react';
import { examService } from '../services';
import {
    Card, CardContent, Button, Badge,
    LoadingScreen, ErrorState
} from '../components/ui';
import type { Exam, ExamAttempt, ExamAnswerEntry } from '../types';

const statusVariant = (status: string) =>
    status === 'REVIEWED' ? 'success'
        : status === 'IN_PROGRESS' ? 'warning'
            : status === 'EXPIRED' ? 'danger'
                : 'info';

const fmtDateTime = (iso: string | null) => {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('hr-HR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
};

export function ExamAttemptsPage() {
    const { t } = useTranslation();
    const { id: examId } = useParams<{ id: string }>();
    const navigate = useNavigate();

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

    const [exam, setExam] = useState<Exam | null>(null);
    const [attempts, setAttempts] = useState<ExamAttempt[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [openId, setOpenId] = useState<string | null>(null);
    const [detail, setDetail] = useState<ExamAttempt | null>(null);

    useEffect(() => {
        if (!examId) return;
        load(examId);
    }, [examId]);

    const load = async (id: string) => {
        try {
            setLoading(true);
            setError(null);
            const [examData, attemptList] = await Promise.all([
                examService.getById(id),
                examService.getAttempts(id),
            ]);
            setExam(examData);
            setAttempts(attemptList);
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setLoading(false);
        }
    };

    const toggle = async (attemptId: string) => {
        if (openId === attemptId) {
            setOpenId(null);
            setDetail(null);
            return;
        }
        setOpenId(attemptId);
        setDetail(null);
        try {
            setDetail(await examService.getAttempt(attemptId));
        } catch {
            setDetail(null);
        }
    };

    const refreshDetail = async (attemptId: string) => {
        setDetail(await examService.getAttempt(attemptId));
        if (examId) setAttempts(await examService.getAttempts(examId));
    };

    const exportCsv = () => {
        if (!exam) return;

        const headers = ['student', 'email', 'status', 'bodovi', 'max_bodovi', 'postotak', 'predano'];
        const escape = (v: unknown) => {
            if (v == null) return '';
            const s = String(v);
            return /[",\n;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
        };

        const rows = attempts.map(a => [
            a.studentName, a.studentEmail, statusLabel(a.status),
            a.totalScore, a.maxScore,
            a.percentage != null ? a.percentage.toFixed(1) : '',
            a.submittedAt,
        ].map(escape).join(','));

        const csv = [headers.join(','), ...rows].join('\n');
        const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `ispit-${exam.title.replace(/\s+/g, '-')}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={() => examId && load(examId)} />;
    if (!exam) return null;

    const submitted = attempts.filter(a => a.status !== 'IN_PROGRESS');
    const avgScore = submitted.length > 0
        ? submitted.reduce((s, a) => s + (a.percentage ?? 0), 0) / submitted.length
        : null;

    return (
        <div className="space-y-6 animate-fade-in">
            <div className="flex items-start gap-4">
                <button
                    onClick={() => navigate('/exams')}
                    className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800 mt-1"
                >
                    <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                </button>

                <div className="flex-1 min-w-0">
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {exam.title}
                    </h1>
                    <p className="text-sm text-zinc-500 mt-1">
                        {exam.taskCount} {t('exams.tasks')} · {exam.maxScore} {t('exams.points')} · {exam.courseName}
                    </p>
                </div>

                {attempts.length > 0 && (
                    <Button onClick={exportCsv} variant="secondary" className="gap-2 shrink-0">
                        <Download className="w-4 h-4" />
                        {t('exams.review.export')}
                    </Button>
                )}
            </div>

            {/* Sazetak */}
            <div className="grid gap-4 sm:grid-cols-3">
                <Card>
                    <CardContent className="py-4">
                        <Users className="w-4 h-4 text-zinc-400 mb-1" />
                        <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {attempts.length}
                        </p>
                        <p className="text-sm text-zinc-500">{t('exams.review.attemptsCount')}</p>
                    </CardContent>
                </Card>
                <Card>
                    <CardContent className="py-4">
                        <CheckCircle2 className="w-4 h-4 text-emerald-500 mb-1" />
                        <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {submitted.length}
                        </p>
                        <p className="text-sm text-zinc-500">{t('exams.review.submittedCount')}</p>
                    </CardContent>
                </Card>
                <Card>
                    <CardContent className="py-4">
                        <Award className="w-4 h-4 text-zinc-400 mb-1" />
                        <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {avgScore != null ? `${avgScore.toFixed(1)}%` : '—'}
                        </p>
                        <p className="text-sm text-zinc-500">{t('exams.review.average')}</p>
                    </CardContent>
                </Card>
            </div>

            {attempts.length === 0 ? (
                <Card>
                    <CardContent className="py-12 text-center text-zinc-500">
                        {t('exams.review.noAttempts')}
                    </CardContent>
                </Card>
            ) : (
                <div className="space-y-3">
                    {attempts.map(attempt => (
                        <Card key={attempt.id}>
                            <button
                                onClick={() => toggle(attempt.id)}
                                className="w-full text-left px-6 py-4 flex items-center gap-4 hover:bg-zinc-50 dark:hover:bg-zinc-800/50 rounded-t-xl"
                            >
                                {openId === attempt.id
                                    ? <ChevronDown className="w-5 h-5 text-zinc-400 shrink-0" />
                                    : <ChevronRight className="w-5 h-5 text-zinc-400 shrink-0" />}

                                <div className="min-w-0 flex-1">
                                    <p className="font-medium text-zinc-900 dark:text-white">
                                        {attempt.studentName}
                                    </p>
                                    <p className="text-xs text-zinc-500 mt-0.5">
                                        {attempt.studentEmail}
                                    </p>
                                </div>

                                <div className="hidden sm:flex items-center gap-6 text-sm shrink-0">
                                    <div className="text-right">
                                        <p className="font-semibold text-zinc-900 dark:text-white">
                                            {attempt.totalScore ?? '—'}/{attempt.maxScore ?? '—'}
                                        </p>
                                        <p className="text-xs text-zinc-400">
                                            {attempt.percentage != null
                                                ? `${attempt.percentage.toFixed(0)}%`
                                                : t('exams.points')}
                                        </p>
                                    </div>
                                    <div className="text-right min-w-[120px]">
                                        <p className="text-xs text-zinc-400">
                                            {t('exams.review.submittedAt')}
                                        </p>
                                        <p className="text-xs text-zinc-600 dark:text-zinc-300">
                                            {fmtDateTime(attempt.submittedAt)}
                                        </p>
                                    </div>
                                </div>

                                <Badge variant={statusVariant(attempt.status)}>
                                    {statusLabel(attempt.status)}
                                </Badge>
                            </button>

                            {openId === attempt.id && (
                                <div className="border-t border-zinc-100 dark:border-zinc-800">
                                    {!detail ? (
                                        <p className="py-8 text-center text-sm text-zinc-500">
                                            {t('exams.review.loading')}
                                        </p>
                                    ) : (
                                        <AttemptDetail
                                            attempt={detail}
                                            onGraded={() => refreshDetail(attempt.id)}
                                        />
                                    )}
                                </div>
                            )}
                        </Card>
                    ))}
                </div>
            )}
        </div>
    );
}

// ======================================================================

function AttemptDetail({ attempt, onGraded }: {
    attempt: ExamAttempt;
    onGraded: () => void;
}) {
    const { t } = useTranslation();
    const [overallFeedback, setOverallFeedback] = useState(attempt.teacherFeedback ?? '');
    const [finalizing, setFinalizing] = useState(false);

    const handleFinalize = async () => {
        setFinalizing(true);
        try {
            await examService.finalizeReview(attempt.id, overallFeedback);
            onGraded();
        } finally {
            setFinalizing(false);
        }
    };

    return (
        <div>
            <div className="divide-y divide-zinc-100 dark:divide-zinc-800">
                {attempt.answers?.map((answer, i) => (
                    <AnswerBlock
                        key={answer.taskId}
                        index={i}
                        answer={answer}
                        attemptId={attempt.id}
                        onGraded={onGraded}
                    />
                ))}
            </div>

            <div className="px-6 py-5 bg-zinc-50 dark:bg-zinc-800/40 space-y-3">
                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300">
                    {t('exams.review.overallComment')}
                </label>
                <textarea
                    value={overallFeedback}
                    onChange={(e) => setOverallFeedback(e.target.value)}
                    rows={3}
                    placeholder={t('exams.review.overallPlaceholder')}
                    className="w-full px-3 py-2 text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                />
                <div className="flex justify-end">
                    <Button onClick={handleFinalize} isLoading={finalizing} className="gap-2">
                        <CheckCircle2 className="w-4 h-4" />
                        {t('exams.review.finalize')}
                    </Button>
                </div>
            </div>
        </div>
    );
}

// ======================================================================

function AnswerBlock({ index, answer, attemptId, onGraded }: {
    index: number;
    answer: ExamAnswerEntry;
    attemptId: string;
    onGraded: () => void;
}) {
    const { t } = useTranslation();
    const [score, setScore] = useState<number | ''>(
        answer.teacherScore ?? answer.finalScore ?? answer.aiScore ?? ''
    );
    const [feedback, setFeedback] = useState(answer.teacherFeedback ?? '');
    const [saving, setSaving] = useState(false);
    const [expanded, setExpanded] = useState(false);

    const needsManual = answer.taskType === 'TEXT' || answer.taskType === 'CHECKLIST';

    const handleSave = async () => {
        setSaving(true);
        try {
            await examService.gradeAnswer(
                attemptId,
                answer.taskId,
                score === '' ? null : Number(score),
                feedback
            );
            onGraded();
        } finally {
            setSaving(false);
        }
    };

    const renderAnswer = () => {
        if (!answer.answerContent) {
            return <p className="text-sm text-zinc-400 italic">{t('exams.review.noAnswer')}</p>;
        }

        if (answer.taskType === 'CHECKLIST') {
            try {
                const items: string[] = JSON.parse(answer.answerContent);
                return (
                    <ul className="space-y-1">
                        {items.map((item, i) => (
                            <li key={i} className="text-sm text-zinc-700 dark:text-zinc-300 flex items-center gap-2">
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                {item}
                            </li>
                        ))}
                    </ul>
                );
            } catch {
                return <p className="text-sm">{answer.answerContent}</p>;
            }
        }

        if (answer.taskType === 'CODE') {
            return (
                <pre className="p-3 rounded-lg bg-zinc-100 dark:bg-zinc-800 text-xs overflow-x-auto max-h-80">
                    {answer.answerContent}
                </pre>
            );
        }

        return (
            <p className="text-sm text-zinc-700 dark:text-zinc-300 whitespace-pre-wrap">
                {answer.answerContent}
            </p>
        );
    };

    return (
        <div className="px-6 py-4">
            <button
                onClick={() => setExpanded(!expanded)}
                className="w-full text-left flex items-center gap-3"
            >
                {expanded
                    ? <ChevronDown className="w-4 h-4 text-zinc-400 shrink-0" />
                    : <ChevronRight className="w-4 h-4 text-zinc-400 shrink-0" />}

                <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-zinc-900 dark:text-white">
                        {index + 1}. {answer.taskTitle}
                    </p>
                </div>

                {answer.testsTotal != null && answer.testsTotal > 0 && (
                    <span className="text-xs text-zinc-500 flex items-center gap-1">
                        {answer.testsPassed === answer.testsTotal
                            ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                            : <XCircle className="w-3.5 h-3.5 text-red-400" />}
                        {answer.testsPassed}/{answer.testsTotal}
                    </span>
                )}

                {needsManual && answer.teacherScore == null && (
                    <Badge variant="warning">{t('exams.review.needsReview')}</Badge>
                )}

                <span className="text-sm font-medium text-zinc-700 dark:text-zinc-200 shrink-0">
                    {answer.finalScore ?? answer.teacherScore ?? answer.aiScore ?? '—'}
                    /{answer.maxScore}
                </span>
            </button>

            {expanded && (
                <div className="mt-4 pl-7 space-y-4">
                    <div>
                        <p className="text-xs font-medium text-zinc-500 uppercase tracking-wide mb-2">
                            {t('exams.review.studentAnswer')}
                        </p>
                        {renderAnswer()}
                    </div>

                    {answer.aiFeedback && (
                        <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800">
                            <p className="text-xs font-semibold text-blue-700 dark:text-blue-300 mb-1.5 flex items-center gap-1.5">
                                <Bot className="w-3.5 h-3.5" />
                                {t('exams.result.aiFeedback')}
                            </p>
                            <p className="text-sm text-blue-900 dark:text-blue-100 whitespace-pre-wrap">
                                {answer.aiFeedback}
                            </p>
                        </div>
                    )}

                    <div className="flex items-end gap-3 flex-wrap">
                        <div className="w-32">
                            <label className="block text-xs font-medium text-zinc-500 mb-1">
                                {t('exams.review.score', { max: answer.maxScore })}
                            </label>
                            <input
                                type="number"
                                value={score}
                                onChange={(e) => setScore(
                                    e.target.value === '' ? '' : Number(e.target.value)
                                )}
                                min={0}
                                max={answer.maxScore ?? undefined}
                                className="w-full px-3 py-2 text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>

                        <div className="flex-1 min-w-[200px]">
                            <label className="block text-xs font-medium text-zinc-500 mb-1">
                                {t('exams.review.comment')}
                            </label>
                            <input
                                type="text"
                                value={feedback}
                                onChange={(e) => setFeedback(e.target.value)}
                                placeholder={t('exams.review.commentPlaceholder')}
                                className="w-full px-3 py-2 text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>

                        <Button onClick={handleSave} isLoading={saving} size="sm" className="gap-1.5">
                            <Save className="w-4 h-4" />
                            {t('exams.review.save')}
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
}