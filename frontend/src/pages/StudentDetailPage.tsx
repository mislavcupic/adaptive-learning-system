import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    ArrowLeft, ChevronDown, ChevronRight, Bot, BotOff, Download,
    CheckCircle2, XCircle, Clock, Mail, School, Award
} from 'lucide-react';
import { userService, submissionOverviewService } from '../services';
import {
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    Button,
    Badge,
    Avatar,
    LoadingScreen,
    ErrorState
} from '../components/ui';
import type { User, StudentBlock, AttemptEntry } from '../types';

const fmtDateTime = (iso: string | null) => {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('hr-HR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
};

const statusVariant = (status: string) => {
    if (status === 'COMPLETED' || status === 'REVIEWED') return 'success';
    if (status.includes('ERROR') || status === 'FAILED') return 'danger';
    return 'warning';
};

export function StudentDetailPage() {
    const { t } = useTranslation();
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const [student, setStudent] = useState<User | null>(null);
    const [block, setBlock] = useState<StudentBlock | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [expanded, setExpanded] = useState<Set<string>>(new Set());

    useEffect(() => {
        if (!id) return;
        load(id);
    }, [id]);

    const load = async (studentId: string) => {
        try {
            setLoading(true);
            setError(null);

            const [userData, overview] = await Promise.all([
                userService.getById(studentId),
                submissionOverviewService.getOverview({ studentId }),
            ]);

            setStudent(userData);
            // Filtar po studentu vraca najvise jedan blok.
            setBlock(overview.students[0] ?? null);
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setLoading(false);
        }
    };

    const toggle = (attemptId: string) => {
        setExpanded(prev => {
            const next = new Set(prev);
            next.has(attemptId) ? next.delete(attemptId) : next.add(attemptId);
            return next;
        });
    };

    const expandAll = () => {
        if (!block) return;
        setExpanded(new Set(block.attempts.map(a => a.submissionId)));
    };

    const exportCsv = () => {
        if (!block || !student) return;

        const headers = [
            'kolegij', 'zadatak', 'status', 'bodovi', 'max_bodovi',
            'testovi_prosli', 'testovi_ukupno', 'ai_feedback', 'nastavnik_feedback', 'vrijeme'
        ];

        const escape = (v: unknown): string => {
            if (v == null) return '';
            const s = String(v);
            return /[",\n;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
        };

        const rows = block.attempts.map(a => [
            a.courseName, a.taskTitle, a.status,
            a.finalScore ?? a.teacherScore ?? a.aiScore, a.maxScore,
            a.testsPassed, a.testsTotal,
            a.aiFeedback, a.teacherFeedback, a.createdAt,
        ].map(escape).join(','));

        const csv = [headers.join(','), ...rows].join('\n');
        const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `pokusaji-${student.lastName}-${student.firstName}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={() => id && load(id)} />;
    if (!student) return null;

    const isExperimental = student.researchGroup === 'EXPERIMENTAL';
    const isControl = student.researchGroup === 'CONTROL';
    const attempts = block?.attempts ?? [];

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Zaglavlje */}
            <div className="flex items-start gap-4">
                <button
                    onClick={() => navigate('/students')}
                    className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800 mt-1"
                >
                    <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                </button>

                <Avatar firstName={student.firstName} lastName={student.lastName} size="lg" />

                <div className="flex-1 min-w-0">
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {student.firstName} {student.lastName}
                    </h1>
                    <div className="flex items-center gap-4 mt-1.5 text-sm text-zinc-500 flex-wrap">
                        <span className="flex items-center gap-1.5">
                            <Mail className="w-3.5 h-3.5" />
                            {student.email}
                        </span>
                        {student.schoolClassName && (
                            <span className="flex items-center gap-1.5">
                                <School className="w-3.5 h-3.5" />
                                {student.schoolClassName}
                            </span>
                        )}
                        {isExperimental && (
                            <Badge variant="success" className="gap-1">
                                <Bot className="w-3 h-3" />
                                Eksperimentalna skupina
                            </Badge>
                        )}
                        {isControl && (
                            <Badge variant="warning" className="gap-1">
                                <BotOff className="w-3 h-3" />
                                Kontrolna skupina
                            </Badge>
                        )}
                    </div>
                </div>

                {attempts.length > 0 && (
                    <Button onClick={exportCsv} variant="secondary" className="gap-2 shrink-0">
                        <Download className="w-4 h-4" />
                        Izvezi
                    </Button>
                )}
            </div>

            {/* Statistika */}
            {block && (
                <div className="grid gap-4 sm:grid-cols-4">
                    <StatCard value={block.totalSubmissions} label="predaja" />
                    <StatCard value={block.tasksAttempted} label="različitih zadataka" />
                    <StatCard
                        value={block.averageScore != null
                            ? block.averageScore.toFixed(1).replace('.', ',')
                            : '—'}
                        label="prosječni bodovi"
                    />
                    <StatCard value={block.withAiFeedback} label="s AI feedbackom" />
                </div>
            )}

            {/* Napomena za kontrolnu skupinu */}
            {isControl && (
                <div className="p-4 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800">
                    <p className="text-sm text-zinc-700 dark:text-zinc-300 flex items-start gap-2.5">
                        <BotOff className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                        <span>
                            <strong>Kontrolna skupina.</strong> Prema nacrtu istraživanja ovaj student
                            ne prima personalizirani AI feedback. Uz svaku predaju dobiva samo bodovnu
                            ocjenu, rezultate testova i izlaz prevoditelja.
                        </span>
                    </p>
                </div>
            )}

            {/* Povijest pokusaja */}
            <Card>
                <CardHeader>
                    <div className="flex items-center justify-between">
                        <CardTitle>Povijest predaja</CardTitle>
                        {attempts.length > 0 && (
                            <div className="flex gap-2">
                                <Button variant="ghost" size="sm" onClick={expandAll}>
                                    Proširi sve
                                </Button>
                                <Button variant="ghost" size="sm" onClick={() => setExpanded(new Set())}>
                                    Sažmi sve
                                </Button>
                            </div>
                        )}
                    </div>
                </CardHeader>
                <CardContent className="px-0 pb-0">
                    {attempts.length === 0 ? (
                        <p className="text-center py-10 text-zinc-500 dark:text-zinc-400">
                            Student još nije predao nijedan zadatak.
                        </p>
                    ) : (
                        <div className="divide-y divide-zinc-100 dark:divide-zinc-800 border-t border-zinc-100 dark:border-zinc-800">
                            {attempts.map(attempt => (
                                <AttemptRow
                                    key={attempt.submissionId}
                                    attempt={attempt}
                                    isControl={isControl}
                                    expanded={expanded.has(attempt.submissionId)}
                                    onToggle={() => toggle(attempt.submissionId)}
                                />
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
}

function StatCard({ value, label }: { value: number | string; label: string }) {
    return (
        <Card>
            <CardContent className="py-4">
                <p className="text-2xl font-semibold text-zinc-900 dark:text-white">{value}</p>
                <p className="text-sm text-zinc-500 mt-0.5">{label}</p>
            </CardContent>
        </Card>
    );
}

function AttemptRow({ attempt, isControl, expanded, onToggle }: {
    attempt: AttemptEntry;
    isControl: boolean;
    expanded: boolean;
    onToggle: () => void;
}) {
    const score = attempt.finalScore ?? attempt.teacherScore ?? attempt.aiScore;
    const hasAi = Boolean(attempt.aiFeedback && attempt.aiFeedback.trim());
    const hasTeacher = Boolean(attempt.teacherFeedback && attempt.teacherFeedback.trim());
    const allTestsPassed = attempt.testsTotal != null
        && attempt.testsTotal > 0
        && attempt.testsPassed === attempt.testsTotal;

    return (
        <div>
            <button
                onClick={onToggle}
                className="w-full text-left px-6 py-4 flex items-center gap-3 hover:bg-zinc-50 dark:hover:bg-zinc-800/40 transition-colors"
            >
                {expanded
                    ? <ChevronDown className="w-4 h-4 text-zinc-400 shrink-0" />
                    : <ChevronRight className="w-4 h-4 text-zinc-400 shrink-0" />}

                <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-zinc-900 dark:text-white truncate">
                        {attempt.taskTitle}
                    </p>
                    <p className="text-xs text-zinc-400 mt-0.5">
                        {attempt.courseName} · {fmtDateTime(attempt.createdAt)}
                    </p>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                    {attempt.testsTotal != null && attempt.testsTotal > 0 && (
                        <span className="text-xs text-zinc-500 flex items-center gap-1">
                            {allTestsPassed
                                ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                : <XCircle className="w-3.5 h-3.5 text-red-400" />}
                            {attempt.testsPassed ?? 0}/{attempt.testsTotal}
                        </span>
                    )}

                    {score != null && (
                        <span className="text-sm font-medium text-zinc-700 dark:text-zinc-200 flex items-center gap-1">
                            <Award className="w-3.5 h-3.5 text-zinc-400" />
                            {score}{attempt.maxScore != null ? `/${attempt.maxScore}` : ''}
                        </span>
                    )}

                    {hasAi
                        ? <Bot className="w-4 h-4 text-emerald-500" />
                        : <BotOff className="w-4 h-4 text-zinc-300 dark:text-zinc-600" />}

                    <Badge variant={statusVariant(attempt.status)}>
                        {attempt.status}
                    </Badge>
                </div>
            </button>

            {expanded && (
                <div className="px-6 pb-5 pl-13 space-y-3">
                    {attempt.executionTimeMs != null && (
                        <p className="text-xs text-zinc-400 flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            {attempt.executionTimeMs} ms
                        </p>
                    )}

                    {hasAi && (
                        <div className="p-4 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800">
                            <p className="text-xs font-semibold text-blue-700 dark:text-blue-300 mb-2 flex items-center gap-1.5">
                                <Bot className="w-3.5 h-3.5" />
                                AI povratna informacija
                            </p>
                            <p className="text-sm text-blue-900 dark:text-blue-100 whitespace-pre-wrap leading-relaxed">
                                {attempt.aiFeedback}
                            </p>
                        </div>
                    )}

                    {!hasAi && isControl && (
                        <div className="p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-100 dark:border-zinc-700">
                            <p className="text-sm text-zinc-500 flex items-center gap-1.5">
                                <BotOff className="w-3.5 h-3.5" />
                                Kontrolna skupina — bez AI povratne informacije, prema nacrtu istraživanja.
                            </p>
                        </div>
                    )}

                    {!hasAi && !isControl && (
                        <div className="p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-100 dark:border-zinc-700">
                            <p className="text-sm text-zinc-500 flex items-center gap-1.5">
                                <BotOff className="w-3.5 h-3.5" />
                                Za ovu predaju AI feedback nije zabilježen.
                            </p>
                        </div>
                    )}

                    {hasTeacher && (
                        <div className="p-4 rounded-lg bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-100 dark:border-emerald-800">
                            <p className="text-xs font-semibold text-emerald-700 dark:text-emerald-300 mb-2">
                                Povratna informacija nastavnika
                            </p>
                            <p className="text-sm text-emerald-900 dark:text-emerald-100 whitespace-pre-wrap leading-relaxed">
                                {attempt.teacherFeedback}
                            </p>
                        </div>
                    )}

                    <a
                        href={`/submissions/${attempt.submissionId}`}
                        className="inline-block text-xs text-zinc-500 hover:text-zinc-900 dark:hover:text-white underline"
                    >
                        Otvori predaju s kodom i izlazom prevoditelja
                    </a>
                </div>
            )}
        </div>
    );
}