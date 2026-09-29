import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Editor from '@monaco-editor/react';
import {
    Clock, CheckCircle2, Circle, AlertTriangle, Send, Save,
    ChevronLeft, ChevronRight, Play, Award, XCircle, Bot
} from 'lucide-react';
import { useTheme } from '../context';
import { examService } from '../services';
import {
    Card, CardHeader, CardTitle, CardContent,
    Button, Badge, LoadingScreen, ErrorState
} from '../components/ui';
import type { Exam, ExamAttempt, ExamAnswerEntry } from '../types';

const monacoLanguage = (lang: string | null | undefined) =>
    lang === 'CSHARP' ? 'csharp' : lang === 'PYTHON' ? 'python' : 'c';

const formatTime = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    const pad = (n: number) => String(n).padStart(2, '0');
    return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`;
};

export function ExamSolvePage() {
    const { t } = useTranslation();
    const { id: examId } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { theme } = useTheme();

    const [exam, setExam] = useState<Exam | null>(null);
    const [attempt, setAttempt] = useState<ExamAttempt | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [currentIndex, setCurrentIndex] = useState(0);
    const [draft, setDraft] = useState('');
    const [saving, setSaving] = useState(false);
    const [running, setRunning] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const [remaining, setRemaining] = useState<number | null>(null);

    useEffect(() => {
        if (!examId) return;
        load(examId);
    }, [examId]);

    const load = async (id: string) => {
        try {
            setLoading(true);
            setError(null);

            const examData = await examService.getById(id);
            setExam(examData);

            // Pokusaj se otvara ako ne postoji, inace se vraca postojeci
            const attemptData = await examService.start(id);
            setAttempt(attemptData);
            setRemaining(attemptData.remainingSeconds);
            setDraft(attemptData.answers?.[0]?.answerContent ?? '');
        } catch (err) {
            setError(err instanceof Error ? err.message : t('exams.solve.cannotOpen'));
        } finally {
            setLoading(false);
        }
    };

    // Odbrojavanje; pri isteku ispit se predaje automatski
    useEffect(() => {
        if (remaining === null || remaining <= 0) return;
        if (attempt?.status !== 'IN_PROGRESS') return;

        const timer = setInterval(() => {
            setRemaining(prev => {
                if (prev === null) return null;
                if (prev <= 1) {
                    handleSubmit(true);
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);

        return () => clearInterval(timer);
    }, [remaining, attempt?.status]);

    const answers = attempt?.answers ?? [];
    const current: ExamAnswerEntry | undefined = answers[currentIndex];
    const isOpen = attempt?.status === 'IN_PROGRESS';

    const saveCurrent = useCallback(async (markDone?: boolean, runTests = false) => {
        if (!examId || !current || !isOpen) return;

        setSaving(true);
        try {
            const updated = await examService.saveAnswer(examId, {
                taskId: current.taskId,
                answerContent: draft,
                markedDone: markDone ?? current.markedDone,
                runTests,
            });
            setAttempt(updated);
        } catch (err) {
            setError(err instanceof Error ? err.message : t('exams.solve.saveFailed'));
        } finally {
            setSaving(false);
        }
    }, [examId, current, draft, isOpen, t]);

    const goTo = async (index: number) => {
        if (index < 0 || index >= answers.length) return;
        if (isOpen && current && draft !== (current.answerContent ?? '')) {
            await saveCurrent();
        }
        setCurrentIndex(index);
        setDraft(answers[index]?.answerContent ?? '');
    };

    const handleRun = async () => {
        setRunning(true);
        await saveCurrent(undefined, true);
        setRunning(false);
    };

    const handleSubmit = async (automatic = false) => {
        if (!examId) return;

        setSubmitting(true);
        try {
            if (!automatic && current) {
                await saveCurrent();
            }
            setAttempt(await examService.submit(examId));
            setShowConfirm(false);
        } catch (err) {
            setError(err instanceof Error ? err.message : t('exams.solve.submitFailed'));
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) return <LoadingScreen />;
    if (error && !attempt) {
        return <ErrorState description={error} onRetry={() => examId && load(examId)} />;
    }
    if (!exam || !attempt) return null;

    if (attempt.status !== 'IN_PROGRESS') {
        return <ExamResult exam={exam} attempt={attempt} onBack={() => navigate('/exams')} />;
    }

    const doneCount = answers.filter(a => a.markedDone).length;
    const lowTime = remaining !== null && remaining < 300;

    return (
        <div className="space-y-5 animate-fade-in pb-24">
            {/* Zaglavlje s odbrojavanjem */}
            <div className="sticky top-0 z-20 -mx-4 px-4 py-3 bg-white/95 dark:bg-zinc-900/95 backdrop-blur border-b border-zinc-200 dark:border-zinc-800">
                <div className="flex items-center justify-between gap-4 flex-wrap">
                    <div className="min-w-0">
                        <h1 className="text-lg font-semibold text-zinc-900 dark:text-white truncate">
                            {exam.title}
                        </h1>
                        <p className="text-xs text-zinc-500">
                            {t('exams.solve.doneOf', { done: doneCount, total: answers.length })}
                        </p>
                    </div>

                    <div className="flex items-center gap-3">
                        {remaining !== null && (
                            <div className={`flex items-center gap-2 px-3 py-1.5 rounded-lg font-mono text-sm font-medium ${
                                lowTime
                                    ? 'bg-red-50 text-red-700 dark:bg-red-900/30 dark:text-red-300'
                                    : 'bg-zinc-100 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-200'
                            }`}>
                                <Clock className="w-4 h-4" />
                                {formatTime(remaining)}
                            </div>
                        )}

                        <Button
                            onClick={() => setShowConfirm(true)}
                            disabled={submitting}
                            className="gap-2"
                        >
                            <Send className="w-4 h-4" />
                            {t('exams.solve.submitExam')}
                        </Button>
                    </div>
                </div>
            </div>

            {lowTime && (
                <div className="flex items-center gap-2 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
                    <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
                    <p className="text-sm text-red-700 dark:text-red-300">
                        {t('exams.solve.timeRunningOut')}
                    </p>
                </div>
            )}

            {error && (
                <div className="p-3 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
                    <p className="text-sm text-amber-800 dark:text-amber-300">{error}</p>
                </div>
            )}

            {/* Navigacija po zadacima */}
            <div className="flex flex-wrap gap-2">
                {answers.map((a, i) => (
                    <button
                        key={a.taskId}
                        onClick={() => goTo(i)}
                        className={`w-9 h-9 rounded-lg text-sm font-medium border transition-colors ${
                            i === currentIndex
                                ? 'border-zinc-900 bg-zinc-900 text-white dark:border-white dark:bg-white dark:text-zinc-900'
                                : a.markedDone
                                    ? 'border-emerald-300 bg-emerald-50 text-emerald-700 dark:border-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300'
                                    : 'border-zinc-200 text-zinc-500 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-800'
                        }`}
                    >
                        {i + 1}
                    </button>
                ))}
            </div>

            {current && (
                <>
                    <Card>
                        <CardHeader>
                            <div className="flex items-start justify-between gap-4">
                                <div className="min-w-0">
                                    <CardTitle>
                                        {currentIndex + 1}. {current.taskTitle}
                                    </CardTitle>
                                    <p className="text-sm text-zinc-500 mt-1 flex items-center gap-2">
                                        <Award className="w-3.5 h-3.5" />
                                        {current.maxScore} {t('exams.points')}
                                    </p>
                                </div>
                                <Badge variant={current.markedDone ? 'success' : 'default'}>
                                    {current.markedDone
                                        ? t('exams.solve.done')
                                        : t('exams.solve.inProgress')}
                                </Badge>
                            </div>
                        </CardHeader>
                        <CardContent>
                            <TaskPrompt taskId={current.taskId} exam={exam} />
                        </CardContent>
                    </Card>

                    <Card className="overflow-hidden">
                        <CardHeader>
                            <CardTitle>{t('exams.solve.yourAnswer')}</CardTitle>
                        </CardHeader>

                        <AnswerInput
                            entry={current}
                            exam={exam}
                            value={draft}
                            onChange={setDraft}
                            theme={theme}
                        />
                    </Card>

                    {exam.showTestResults && current.taskType === 'CODE'
                        && current.testsTotal != null && current.testsTotal > 0 && (
                            <Card>
                                <CardContent className="py-4">
                                    <div className="flex items-center gap-3">
                                        {current.testsPassed === current.testsTotal
                                            ? <CheckCircle2 className="w-5 h-5 text-emerald-600" />
                                            : <XCircle className="w-5 h-5 text-red-500" />}
                                        <p className="text-sm text-zinc-700 dark:text-zinc-300">
                                            {t('exams.solve.testsPassed', {
                                                passed: current.testsPassed,
                                                total: current.testsTotal
                                            })}
                                        </p>
                                    </div>
                                    {current.compilerOutput && (
                                        <pre className="mt-3 p-3 rounded-lg bg-zinc-100 dark:bg-zinc-800 text-xs overflow-x-auto">
                                        {current.compilerOutput}
                                    </pre>
                                    )}
                                </CardContent>
                            </Card>
                        )}

                    <div className="flex items-center justify-between gap-3 flex-wrap">
                        <div className="flex gap-2">
                            <Button
                                variant="secondary"
                                onClick={() => goTo(currentIndex - 1)}
                                disabled={currentIndex === 0}
                                className="gap-1"
                            >
                                <ChevronLeft className="w-4 h-4" />
                                {t('exams.solve.previous')}
                            </Button>
                            <Button
                                variant="secondary"
                                onClick={() => goTo(currentIndex + 1)}
                                disabled={currentIndex === answers.length - 1}
                                className="gap-1"
                            >
                                {t('exams.solve.next')}
                                <ChevronRight className="w-4 h-4" />
                            </Button>
                        </div>

                        <div className="flex gap-2">
                            {exam.showTestResults && current.taskType === 'CODE' && (
                                <Button
                                    variant="secondary"
                                    onClick={handleRun}
                                    isLoading={running}
                                    className="gap-2"
                                >
                                    <Play className="w-4 h-4" />
                                    {t('exams.solve.runTests')}
                                </Button>
                            )}

                            <Button
                                variant="secondary"
                                onClick={() => saveCurrent()}
                                isLoading={saving}
                                className="gap-2"
                            >
                                <Save className="w-4 h-4" />
                                {t('exams.solve.save')}
                            </Button>

                            <Button
                                onClick={() => saveCurrent(!current.markedDone)}
                                className="gap-2"
                            >
                                {current.markedDone
                                    ? <Circle className="w-4 h-4" />
                                    : <CheckCircle2 className="w-4 h-4" />}
                                {current.markedDone
                                    ? t('exams.solve.unmarkDone')
                                    : t('exams.solve.markDone')}
                            </Button>
                        </div>
                    </div>
                </>
            )}

            {showConfirm && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
                    <Card className="w-full max-w-md">
                        <CardHeader>
                            <CardTitle>{t('exams.solve.confirmTitle')}</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <p className="text-sm text-zinc-600 dark:text-zinc-400">
                                {t('exams.solve.confirmText')}
                            </p>

                            {doneCount < answers.length && (
                                <div className="p-3 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
                                    <p className="text-sm text-amber-800 dark:text-amber-300">
                                        {t('exams.solve.confirmUnfinished', {
                                            count: answers.length - doneCount,
                                            total: answers.length
                                        })}
                                    </p>
                                </div>
                            )}

                            <div className="flex justify-end gap-2 pt-2">
                                <Button variant="secondary" onClick={() => setShowConfirm(false)}>
                                    {t('exams.solve.cancel')}
                                </Button>
                                <Button onClick={() => handleSubmit()} isLoading={submitting}>
                                    {t('exams.solve.submit')}
                                </Button>
                            </div>
                        </CardContent>
                    </Card>
                </div>
            )}
        </div>
    );
}

// ======================================================================

function TaskPrompt({ taskId, exam }: { taskId: string; exam: Exam }) {
    const task = exam.tasks?.find(t => t.taskId === taskId);
    if (!task) return null;

    return (
        <div className="space-y-3">
            {task.instructions && (
                <p className="text-sm text-zinc-700 dark:text-zinc-300 whitespace-pre-wrap leading-relaxed">
                    {task.instructions}
                </p>
            )}
            {task.description && (
                <p className="text-sm text-zinc-500 dark:text-zinc-400 whitespace-pre-wrap">
                    {task.description}
                </p>
            )}
        </div>
    );
}

// ======================================================================

function AnswerInput({ entry, exam, value, onChange, theme }: {
    entry: ExamAnswerEntry;
    exam: Exam;
    value: string;
    onChange: (v: string) => void;
    theme: string;
}) {
    const { t } = useTranslation();
    const task = exam.tasks?.find(tk => tk.taskId === entry.taskId);

    const parseOptions = (json: string | null | undefined): string[] => {
        if (!json) return [];
        try {
            return JSON.parse(json);
        } catch {
            return [];
        }
    };

    const options = parseOptions(task?.options);

    if (entry.taskType === 'CODE') {
        return (
            <div className="h-[420px] border-t border-zinc-100 dark:border-zinc-800">
                <Editor
                    height="100%"
                    language={monacoLanguage(task?.languageType)}
                    value={value}
                    onChange={(v) => onChange(v || '')}
                    theme={theme === 'dark' ? 'vs-dark' : 'light'}
                    options={{
                        minimap: { enabled: false },
                        fontSize: 14,
                        scrollBeyondLastLine: false,
                        automaticLayout: true,
                        tabSize: 4,
                        wordWrap: 'on',
                    }}
                />
            </div>
        );
    }

    if (entry.taskType === 'TEXT') {
        return (
            <CardContent>
                <textarea
                    value={value}
                    onChange={(e) => onChange(e.target.value)}
                    rows={12}
                    placeholder={t('exams.solve.answerPlaceholder')}
                    className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white resize-none"
                />
            </CardContent>
        );
    }

    if (entry.taskType === 'MULTIPLE_CHOICE') {
        return (
            <CardContent className="space-y-2.5">
                {options.map((option, i) => (
                    <label
                        key={i}
                        className={`flex items-center gap-3 p-3.5 rounded-lg border cursor-pointer transition-colors ${
                            value === option
                                ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                                : 'border-zinc-200 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-800'
                        }`}
                    >
                        <input
                            type="radio"
                            name={`exam-task-${entry.taskId}`}
                            value={option}
                            checked={value === option}
                            onChange={(e) => onChange(e.target.value)}
                            className="w-4 h-4"
                        />
                        <span className="text-sm text-zinc-700 dark:text-zinc-300">{option}</span>
                    </label>
                ))}
            </CardContent>
        );
    }

    const selected: string[] = (() => {
        try {
            return value ? JSON.parse(value) : [];
        } catch {
            return [];
        }
    })();

    const toggle = (option: string) => {
        const next = selected.includes(option)
            ? selected.filter(o => o !== option)
            : [...selected, option];
        onChange(JSON.stringify(next));
    };

    return (
        <CardContent className="space-y-2.5">
            {options.map((option, i) => (
                <label
                    key={i}
                    className={`flex items-center gap-3 p-3.5 rounded-lg border cursor-pointer transition-colors ${
                        selected.includes(option)
                            ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                            : 'border-zinc-200 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-800'
                    }`}
                >
                    <input
                        type="checkbox"
                        checked={selected.includes(option)}
                        onChange={() => toggle(option)}
                        className="w-4 h-4 rounded"
                    />
                    <span className="text-sm text-zinc-700 dark:text-zinc-300">{option}</span>
                </label>
            ))}
        </CardContent>
    );
}

// ======================================================================

function ExamResult({ exam, attempt, onBack }: {
    exam: Exam;
    attempt: ExamAttempt;
    onBack: () => void;
}) {
    const { t } = useTranslation();

    const passed = attempt.passed;
    const awaitingReview = attempt.answers?.some(
        a => (a.taskType === 'TEXT' || a.taskType === 'CHECKLIST') && a.teacherScore == null
    );

    return (
        <div className="space-y-5 animate-fade-in max-w-3xl">
            <Card>
                <CardContent className="py-8 text-center">
                    <div className={`w-16 h-16 mx-auto rounded-full flex items-center justify-center ${
                        passed === true
                            ? 'bg-emerald-50 dark:bg-emerald-900/30'
                            : 'bg-zinc-100 dark:bg-zinc-800'
                    }`}>
                        {passed === true
                            ? <CheckCircle2 className="w-9 h-9 text-emerald-600" />
                            : <Award className="w-9 h-9 text-zinc-400" />}
                    </div>

                    <h1 className="mt-5 text-xl font-semibold text-zinc-900 dark:text-white">
                        {attempt.status === 'EXPIRED'
                            ? t('exams.result.expired')
                            : t('exams.result.submitted')}
                    </h1>
                    <p className="mt-1 text-sm text-zinc-500">{exam.title}</p>

                    <div className="mt-6 flex items-center justify-center gap-8">
                        <div>
                            <p className="text-3xl font-semibold text-zinc-900 dark:text-white">
                                {attempt.totalScore ?? '—'}
                                <span className="text-lg text-zinc-400">/{attempt.maxScore ?? '—'}</span>
                            </p>
                            <p className="text-sm text-zinc-500 mt-1">{t('exams.result.points')}</p>
                        </div>
                        {attempt.percentage != null && (
                            <div>
                                <p className="text-3xl font-semibold text-zinc-900 dark:text-white">
                                    {attempt.percentage.toFixed(0)}%
                                </p>
                                <p className="text-sm text-zinc-500 mt-1">
                                    {t('exams.result.percentage')}
                                </p>
                            </div>
                        )}
                    </div>

                    {awaitingReview && (
                        <div className="mt-6 p-3 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 text-left">
                            <p className="text-sm text-amber-800 dark:text-amber-300">
                                {t('exams.result.awaitingReview')}
                            </p>
                        </div>
                    )}

                    <Button onClick={onBack} className="mt-6">
                        {t('exams.result.backToExams')}
                    </Button>
                </CardContent>
            </Card>

            {attempt.answers && attempt.answers.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle>{t('exams.result.taskOverview')}</CardTitle>
                    </CardHeader>
                    <CardContent className="px-0 pb-0">
                        <div className="divide-y divide-zinc-100 dark:divide-zinc-800 border-t border-zinc-100 dark:border-zinc-800">
                            {attempt.answers.map((a, i) => (
                                <div key={a.taskId} className="px-6 py-4 space-y-3">
                                    <div className="flex items-center justify-between gap-3">
                                        <p className="text-sm font-medium text-zinc-900 dark:text-white">
                                            {i + 1}. {a.taskTitle}
                                        </p>
                                        <span className="text-sm text-zinc-600 dark:text-zinc-300 shrink-0">
                                            {a.finalScore ?? a.teacherScore ?? a.aiScore ?? '—'}
                                            /{a.maxScore}
                                        </span>
                                    </div>

                                    {a.aiFeedback && (
                                        <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800">
                                            <p className="text-xs font-semibold text-blue-700 dark:text-blue-300 mb-1.5 flex items-center gap-1.5">
                                                <Bot className="w-3.5 h-3.5" />
                                                {t('exams.result.aiFeedback')}
                                            </p>
                                            <p className="text-sm text-blue-900 dark:text-blue-100 whitespace-pre-wrap">
                                                {a.aiFeedback}
                                            </p>
                                        </div>
                                    )}

                                    {a.teacherFeedback && (
                                        <div className="p-3 rounded-lg bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-100 dark:border-emerald-800">
                                            <p className="text-xs font-semibold text-emerald-700 dark:text-emerald-300 mb-1.5">
                                                {t('exams.result.teacherComment')}
                                            </p>
                                            <p className="text-sm text-emerald-900 dark:text-emerald-100 whitespace-pre-wrap">
                                                {a.teacherFeedback}
                                            </p>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </CardContent>
                </Card>
            )}

            {attempt.teacherFeedback && (
                <Card>
                    <CardHeader>
                        <CardTitle>{t('exams.result.teacherComment')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <p className="text-sm text-zinc-700 dark:text-zinc-300 whitespace-pre-wrap">
                            {attempt.teacherFeedback}
                        </p>
                    </CardContent>
                </Card>
            )}
        </div>
    );
}