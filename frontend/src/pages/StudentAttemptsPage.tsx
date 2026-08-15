import { useState, useEffect } from 'react';

import { useTranslation } from 'react-i18next';
import {
    ChevronDown, ChevronRight, Bot, BotOff, Download, Filter,
    CheckCircle2, XCircle, Clock, User as UserIcon, GraduationCap
} from 'lucide-react';
import { submissionOverviewService, courseService } from '../services';
import {
    Card,
    CardContent,
    Button,
    Badge,
    LoadingScreen,
    ErrorState
} from '../components/ui';
import type { SubmissionsOverview, StudentBlock, AttemptEntry, Course } from '../types';

const DAY_OPTIONS = [
    { value: 0, label: 'Sve' },
    { value: 7, label: 'Zadnjih 7 dana' },
    { value: 30, label: 'Zadnjih 30 dana' },
    { value: 90, label: 'Zadnjih 90 dana' },
];

const fmtDateTime = (iso: string | null) => {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleString('hr-HR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
};

const statusVariant = (status: string) => {
    if (status === 'COMPLETED' || status === 'REVIEWED') return 'success';
    if (status.includes('ERROR') || status === 'FAILED') return 'danger';
    return 'warning';
};

export function StudentAttemptsPage() {
    const { t } = useTranslation();

    const [data, setData] = useState<SubmissionsOverview | null>(null);
    const [courses, setCourses] = useState<Course[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [courseId, setCourseId] = useState('');
    const [lastDays, setLastDays] = useState(0);

    const [expandedStudents, setExpandedStudents] = useState<Set<string>>(new Set());
    const [expandedAttempts, setExpandedAttempts] = useState<Set<string>>(new Set());

    useEffect(() => {
        courseService.getAll().then(setCourses).catch(() => setCourses([]));
    }, []);

    useEffect(() => {
        load();
    }, [courseId, lastDays]);

    const load = async () => {
        try {
            setLoading(true);
            setError(null);
            const result = await submissionOverviewService.getOverview({
                courseId: courseId || undefined,
                lastDays: lastDays || undefined,
            });
            setData(result);
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setLoading(false);
        }
    };

    const toggleStudent = (id: string) => {
        setExpandedStudents(prev => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    };

    const toggleAttempt = (id: string) => {
        setExpandedAttempts(prev => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    };

    const expandAll = () => {
        if (!data) return;
        setExpandedStudents(new Set(data.students.map(s => s.studentId)));
    };

    const collapseAll = () => {
        setExpandedStudents(new Set());
        setExpandedAttempts(new Set());
    };

    const exportCsv = () => {
        if (!data) return;

        const headers = [
            'student', 'email', 'skupina', 'razred', 'kolegij', 'zadatak',
            'status', 'bodovi', 'max_bodovi', 'testovi_prosli', 'testovi_ukupno',
            'ima_ai_feedback', 'ai_feedback', 'nastavnik_feedback', 'vrijeme'
        ];

        const escape = (v: unknown): string => {
            if (v == null) return '';
            const s = String(v);
            return /[",\n;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
        };

        const rows: string[] = [];
        for (const st of data.students) {
            for (const a of st.attempts) {
                rows.push([
                    `${st.firstName} ${st.lastName}`,
                    st.email,
                    st.researchGroup,
                    st.schoolClassName,
                    a.courseName,
                    a.taskTitle,
                    a.status,
                    a.finalScore ?? a.teacherScore ?? a.aiScore,
                    a.maxScore,
                    a.testsPassed,
                    a.testsTotal,
                    a.aiFeedback ? 'DA' : 'NE',
                    a.aiFeedback,
                    a.teacherFeedback,
                    a.createdAt,
                ].map(escape).join(','));
            }
        }

        const csv = [headers.join(','), ...rows].join('\n');
        const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `pokusaji-studenata-${new Date().toISOString().slice(0, 10)}.csv`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
    };

    if (loading && !data) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={load} />;

    const experimentalCount = data?.students.filter(s => s.researchGroup === 'EXPERIMENTAL').length ?? 0;
    const controlCount = data?.students.filter(s => s.researchGroup === 'CONTROL').length ?? 0;

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Zaglavlje */}
            <div className="flex items-start justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        Pokušaji rješavanja
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                        Sve predaje studenata i povratne informacije koje su primili
                    </p>
                </div>
                <Button onClick={exportCsv} disabled={!data?.totalSubmissions} className="gap-2">
                    <Download className="w-4 h-4" />
                    Izvezi (CSV)
                </Button>
            </div>

            {/* Filtri */}
            <Card>
                <CardContent className="py-4">
                    <div className="flex flex-wrap items-end gap-4">
                        <div className="flex items-center gap-2 text-sm text-zinc-500">
                            <Filter className="w-4 h-4" />
                            Filtri
                        </div>

                        <div>
                            <label className="block text-xs font-medium text-zinc-500 mb-1">
                                Kolegij
                            </label>
                            <select
                                value={courseId}
                                onChange={(e) => setCourseId(e.target.value)}
                                className="px-3 py-2 text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            >
                                <option value="">Svi kolegiji</option>
                                {courses.map(c => (
                                    <option key={c.id} value={c.id}>{c.name}</option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label className="block text-xs font-medium text-zinc-500 mb-1">
                                Razdoblje
                            </label>
                            <select
                                value={lastDays}
                                onChange={(e) => setLastDays(Number(e.target.value))}
                                className="px-3 py-2 text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            >
                                {DAY_OPTIONS.map(o => (
                                    <option key={o.value} value={o.value}>{o.label}</option>
                                ))}
                            </select>
                        </div>

                        <div className="ml-auto flex gap-2">
                            <Button variant="secondary" onClick={expandAll}>Proširi sve</Button>
                            <Button variant="ghost" onClick={collapseAll}>Sažmi sve</Button>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Sazetak */}
            <div className="grid gap-4 sm:grid-cols-4">
                <SummaryCard
                    icon={<UserIcon className="w-4 h-4 text-zinc-400" />}
                    value={data?.totalStudents ?? 0}
                    label="studenata"
                />
                <SummaryCard
                    icon={<GraduationCap className="w-4 h-4 text-zinc-400" />}
                    value={data?.totalSubmissions ?? 0}
                    label="predaja ukupno"
                />
                <SummaryCard
                    icon={<Bot className="w-4 h-4 text-emerald-500" />}
                    value={experimentalCount}
                    label="eksperimentalna skupina"
                />
                <SummaryCard
                    icon={<BotOff className="w-4 h-4 text-amber-500" />}
                    value={controlCount}
                    label="kontrolna skupina"
                />
            </div>

            {/* Popis studenata */}
            {!data || data.students.length === 0 ? (
                <Card>
                    <CardContent className="py-12 text-center text-zinc-500 dark:text-zinc-400">
                        Nema predaja za odabrane filtre.
                    </CardContent>
                </Card>
            ) : (
                <div className="space-y-3">
                    {data.students.map(student => (
                        <StudentRow
                            key={student.studentId}
                            student={student}
                            expanded={expandedStudents.has(student.studentId)}
                            onToggle={() => toggleStudent(student.studentId)}
                            expandedAttempts={expandedAttempts}
                            onToggleAttempt={toggleAttempt}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}

function SummaryCard({ icon, value, label }: {
    icon: React.ReactNode;
    value: number;
    label: string;
}) {
    return (
        <Card>
            <CardContent className="py-4">
                <div className="flex items-center gap-2 mb-1">{icon}</div>
                <p className="text-2xl font-semibold text-zinc-900 dark:text-white">{value}</p>
                <p className="text-sm text-zinc-500">{label}</p>
            </CardContent>
        </Card>
    );
}

function StudentRow({ student, expanded, onToggle, expandedAttempts, onToggleAttempt }: {
    student: StudentBlock;
    expanded: boolean;
    onToggle: () => void;
    expandedAttempts: Set<string>;
    onToggleAttempt: (id: string) => void;
}) {
    const isExperimental = student.researchGroup === 'EXPERIMENTAL';
    const isControl = student.researchGroup === 'CONTROL';

    return (
        <Card>
            <button
                onClick={onToggle}
                className="w-full text-left px-6 py-4 flex items-center gap-4 hover:bg-zinc-50 dark:hover:bg-zinc-800/50 transition-colors rounded-t-xl"
            >
                {expanded
                    ? <ChevronDown className="w-5 h-5 text-zinc-400 shrink-0" />
                    : <ChevronRight className="w-5 h-5 text-zinc-400 shrink-0" />}

                <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                        <p className="font-medium text-zinc-900 dark:text-white">
                            {student.firstName} {student.lastName}
                        </p>
                        {isExperimental && (
                            <Badge variant="success" className="gap-1">
                                <Bot className="w-3 h-3" />
                                Eksperimentalna
                            </Badge>
                        )}
                        {isControl && (
                            <Badge variant="warning" className="gap-1">
                                <BotOff className="w-3 h-3" />
                                Kontrolna
                            </Badge>
                        )}
                        {student.schoolClassName && (
                            <span className="text-xs text-zinc-400">{student.schoolClassName}</span>
                        )}
                    </div>
                    <p className="text-xs text-zinc-500 mt-0.5">{student.email}</p>
                </div>

                <div className="hidden sm:flex items-center gap-6 text-sm shrink-0">
                    <Stat value={student.totalSubmissions} label="predaja" />
                    <Stat value={student.tasksAttempted} label="zadataka" />
                    <Stat
                        value={student.averageScore != null
                            ? student.averageScore.toFixed(1).replace('.', ',')
                            : '—'}
                        label="prosjek"
                    />
                    <div className="text-right min-w-[110px]">
                        <p className="text-xs text-zinc-400">zadnja aktivnost</p>
                        <p className="text-xs text-zinc-600 dark:text-zinc-300">
                            {fmtDateTime(student.lastActivity)}
                        </p>
                    </div>
                </div>
            </button>

            {expanded && (
                <div className="border-t border-zinc-100 dark:border-zinc-800">
                    {isControl && (
                        <div className="px-6 py-3 bg-amber-50 dark:bg-amber-900/20 border-b border-amber-100 dark:border-amber-900">
                            <p className="text-sm text-zinc-700 dark:text-zinc-300 flex items-start gap-2">
                                <BotOff className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                                <span>
                                    Student pripada kontrolnoj skupini i prema nacrtu istraživanja
                                    ne prima personalizirani AI feedback, nego samo bodovnu ocjenu
                                    i izlaz prevoditelja.
                                </span>
                            </p>
                        </div>
                    )}

                    <div className="divide-y divide-zinc-100 dark:divide-zinc-800">
                        {student.attempts.map(attempt => (
                            <AttemptRow
                                key={attempt.submissionId}
                                attempt={attempt}
                                expanded={expandedAttempts.has(attempt.submissionId)}
                                onToggle={() => onToggleAttempt(attempt.submissionId)}
                            />
                        ))}
                    </div>
                </div>
            )}
        </Card>
    );
}

function Stat({ value, label }: { value: number | string; label: string }) {
    return (
        <div className="text-center">
            <p className="font-semibold text-zinc-900 dark:text-white">{value}</p>
            <p className="text-xs text-zinc-400">{label}</p>
        </div>
    );
}

function AttemptRow({ attempt, expanded, onToggle }: {
    attempt: AttemptEntry;
    expanded: boolean;
    onToggle: () => void;
}) {
    const score = attempt.finalScore ?? attempt.teacherScore ?? attempt.aiScore;
    const hasAi = Boolean(attempt.aiFeedback && attempt.aiFeedback.trim());
    const hasTeacher = Boolean(attempt.teacherFeedback && attempt.teacherFeedback.trim());

    return (
        <div>
            <button
                onClick={onToggle}
                className="w-full text-left px-6 py-3 flex items-center gap-3 hover:bg-zinc-50 dark:hover:bg-zinc-800/30 transition-colors"
            >
                {expanded
                    ? <ChevronDown className="w-4 h-4 text-zinc-300 shrink-0" />
                    : <ChevronRight className="w-4 h-4 text-zinc-300 shrink-0" />}

                <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-zinc-900 dark:text-white truncate">
                        {attempt.taskTitle}
                    </p>
                    <p className="text-xs text-zinc-400">
                        {attempt.courseName} · {fmtDateTime(attempt.createdAt)}
                    </p>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                    {attempt.testsTotal != null && attempt.testsTotal > 0 && (
                        <span className="text-xs text-zinc-500 flex items-center gap-1">
                            {attempt.testsPassed === attempt.testsTotal
                                ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                : <XCircle className="w-3.5 h-3.5 text-red-400" />}
                            {attempt.testsPassed ?? 0}/{attempt.testsTotal}
                        </span>
                    )}

                    {score != null && (
                        <span className="text-sm font-medium text-zinc-700 dark:text-zinc-200">
                            {score}{attempt.maxScore != null ? `/${attempt.maxScore}` : ''}
                        </span>
                    )}

                    {hasAi ? (
                        <Bot className="w-4 h-4 text-emerald-500" />
                    ) : (
                        <BotOff className="w-4 h-4 text-zinc-300 dark:text-zinc-600" />
                    )}

                    <Badge variant={statusVariant(attempt.status)}>
                        {attempt.status}
                    </Badge>
                </div>
            </button>

            {expanded && (
                <div className="px-6 pb-4 pl-13 space-y-3">
                    {attempt.executionTimeMs != null && (
                        <p className="text-xs text-zinc-400 flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            {attempt.executionTimeMs} ms
                        </p>
                    )}

                    {hasAi ? (
                        <div className="p-4 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800">
                            <p className="text-xs font-semibold text-blue-700 dark:text-blue-300 mb-2 flex items-center gap-1.5">
                                <Bot className="w-3.5 h-3.5" />
                                AI povratna informacija
                            </p>
                            <p className="text-sm text-blue-900 dark:text-blue-100 whitespace-pre-wrap leading-relaxed">
                                {attempt.aiFeedback}
                            </p>
                        </div>
                    ) : (
                        <div className="p-3 rounded-lg bg-zinc-50 dark:bg-zinc-800/50 border border-zinc-100 dark:border-zinc-700">
                            <p className="text-sm text-zinc-500 flex items-center gap-1.5">
                                <BotOff className="w-3.5 h-3.5" />
                                Bez AI povratne informacije
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
                        Otvori cijelu predaju s kodom
                    </a>
                </div>
            )}
        </div>
    );
}