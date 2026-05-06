import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Editor from '@monaco-editor/react';
import { ArrowLeft, Save, Trash2, Edit2, X } from 'lucide-react';
import { useTheme } from '../context';
import { taskService, outcomeService } from '../services';
import {
    Card, CardHeader, CardTitle, CardContent,
    Button, Input, LoadingScreen, ErrorState, Badge, Modal, ModalFooter
} from '../components/ui';
import { formatDate } from '../utils';
import type { Task, LearningOutcome } from '../types';

export function TaskDetailPage() {
    const { t } = useTranslation();
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { theme } = useTheme();

    const [task, setTask] = useState<Task | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [editing, setEditing] = useState(false);
    const [saving, setSaving] = useState(false);
    const [deleteModal, setDeleteModal] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [outcomes, setOutcomes] = useState<LearningOutcome[]>([]);

    const [form, setForm] = useState({
        title: '',
        description: '',
        outcomeId: '',
        starterCode: '',
        solutionCode: '',
        testCases: '',
        gradingCriteria: '',
        maxScore: 100,
        timeLimitSeconds: 30,
        memoryLimitMb: 256,
        orderIndex: 0,
        dueDate: '',
    });

    useEffect(() => {
        if (!id) return;
        taskService.getById(id)
            .then(data => {
                setTask(data);
                setForm({
                    title: data.title || '',
                    description: data.description || '',
                    outcomeId: data.outcomeId || '',
                    starterCode: data.starterCode || '',
                    solutionCode: data.solutionCode || '',
                    testCases: data.testCases || '',
                    gradingCriteria: data.gradingCriteria || '',
                    maxScore: data.maxScore || 100,
                    timeLimitSeconds: data.timeLimitSeconds || 30,
                    memoryLimitMb: data.memoryLimitMb || 256,
                    orderIndex: data.orderIndex || 0,
                    dueDate: data.dueDate ? new Date(data.dueDate).toISOString().slice(0, 16) : '',
                });
            })
            .catch(e => setError(e.message))
            .finally(() => setLoading(false));

        outcomeService.getAll().then(setOutcomes).catch(() => {});
    }, [id]);

    const set = (field: string, value: any) =>
        setForm(f => ({ ...f, [field]: value }));

    const getMonacoLanguage = () => {
        const lang = (task as any)?.languageType || 'C';
        if (lang === 'CSHARP') return 'csharp';
        if (lang === 'PYTHON') return 'python';
        return 'c';
    };

    const handleSave = async () => {
        if (!id || !form.title.trim()) return;
        setSaving(true);
        try {
            const updated = await taskService.update(id, {
                instructions: "",
                title: form.title,
                description: form.description || undefined,
                outcomeId: form.outcomeId as any,
                starterCode: form.starterCode || undefined,
                solutionCode: form.solutionCode || undefined,
                testCases: form.testCases || undefined,
                gradingCriteria: form.gradingCriteria || undefined,
                maxScore: form.maxScore,
                timeLimitSeconds: form.timeLimitSeconds,
                memoryLimitMb: form.memoryLimitMb,
                orderIndex: form.orderIndex,
                dueDate: form.dueDate ? new Date(form.dueDate).toISOString() as any : undefined
            });
            setTask(updated);
            setEditing(false);
        } catch {
            setError(t('errors.generic'));
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async () => {
        if (!id) return;
        setDeleting(true);
        try {
            await taskService.delete(id);
            navigate('/tasks');
        } finally {
            setDeleting(false);
        }
    };

    if (loading) return <LoadingScreen />;
    if (error && !task) return <ErrorState description={error} onRetry={() => navigate('/tasks')} />;
    if (!task) return null;

    const editorOptions = {
        minimap: { enabled: false },
        fontSize: 13,
        lineNumbers: 'on' as const,
        scrollBeyondLastLine: false,
        automaticLayout: true,
        tabSize: 4,
        readOnly: !editing,
    };

    const outcomesByCourse = outcomes.reduce((acc, o) => {
        const key = o.courseName || '';
        if (!acc[key]) acc[key] = [];
        acc[key].push(o);
        return acc;
    }, {} as Record<string, LearningOutcome[]>);

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <button onClick={() => navigate('/tasks')} className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800">
                        <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                    </button>
                    <div>
                        <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {editing ? t('tasks.editTask') : task.title}
                        </h1>
                        {!editing && (
                            <div className="flex items-center gap-2 mt-1">
                                {task.courseName && <Badge variant="info">{task.courseName}</Badge>}
                                {task.outcomeName && <Badge>{task.outcomeName}</Badge>}
                            </div>
                        )}
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    {editing ? (
                        <>
                            <Button variant="ghost" onClick={() => setEditing(false)} className="gap-2">
                                <X className="w-4 h-4" />
                                {t('common.cancel')}
                            </Button>
                            <Button onClick={handleSave} isLoading={saving} className="gap-2">
                                <Save className="w-4 h-4" />
                                {t('common.save')}
                            </Button>
                        </>
                    ) : (
                        <>
                            <Button variant="ghost" onClick={() => setDeleteModal(true)} className="gap-2 text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20">
                                <Trash2 className="w-4 h-4" />
                                {t('common.delete')}
                            </Button>
                            <Button onClick={() => setEditing(true)} className="gap-2">
                                <Edit2 className="w-4 h-4" />
                                {t('common.edit')}
                            </Button>
                        </>
                    )}
                </div>
            </div>

            {error && (
                <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-700 dark:text-red-400 text-sm">
                    {error}
                </div>
            )}

            {/* Osnovno */}
            <Card>
                <CardHeader><CardTitle>{t('tasks.basicInfo')}</CardTitle></CardHeader>
                <CardContent className="space-y-4">
                    {editing ? (
                        <>
                            <Input
                                label={`${t('tasks.taskTitle')} *`}
                                value={form.title}
                                onChange={e => set('title', e.target.value)}
                            />
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                                    {t('common.description')}
                                </label>
                                <textarea
                                    value={form.description}
                                    onChange={e => set('description', e.target.value)}
                                    rows={3}
                                    className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500 resize-none"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                                    {t('tasks.learningOutcome')} *
                                </label>
                                <select
                                    value={form.outcomeId}
                                    onChange={e => set('outcomeId', e.target.value)}
                                    className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500"
                                >
                                    {Object.entries(outcomesByCourse).map(([course, outs]) => (
                                        <optgroup key={course} label={course}>
                                            {outs.map(o => (
                                                <option key={o.id} value={o.id}>{o.name}</option>
                                            ))}
                                        </optgroup>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                                    {t('tasks.studentInstructions')}
                                </label>
                                <textarea
                                    value={form.gradingCriteria}
                                    onChange={e => set('gradingCriteria', e.target.value)}
                                    rows={5}
                                    className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500 resize-none font-mono"
                                />
                            </div>
                        </>
                    ) : (
                        <div className="space-y-3">
                            <div className="grid grid-cols-3 gap-4 text-sm">
                                <div>
                                    <p className="text-zinc-500 dark:text-zinc-400">{t('tasks.maxScore')}</p>
                                    <p className="font-medium text-zinc-900 dark:text-white">{task.maxScore} {t('common.noData') === 'Nema podataka' ? 'bodova' : 'points'}</p>
                                </div>
                                <div>
                                    <p className="text-zinc-500 dark:text-zinc-400">{t('tasks.timeLimit')}</p>
                                    <p className="font-medium text-zinc-900 dark:text-white">{task.timeLimitSeconds}s</p>
                                </div>
                                <div>
                                    <p className="text-zinc-500 dark:text-zinc-400">{t('tasks.memoryLimit')}</p>
                                    <p className="font-medium text-zinc-900 dark:text-white">{task.memoryLimitMb}MB</p>
                                </div>
                            </div>
                            {task.gradingCriteria && (
                                <div>
                                    <p className="text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">{t('tasks.studentInstructions')}</p>
                                    <p className="text-sm text-zinc-600 dark:text-zinc-400 whitespace-pre-wrap font-mono bg-zinc-50 dark:bg-zinc-800/50 p-3 rounded-lg">
                                        {task.gradingCriteria}
                                    </p>
                                </div>
                            )}
                            <p className="text-xs text-zinc-400">{t('common.date')}: {formatDate(task.createdAt)}</p>
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* Starter code */}
            <Card>
                <CardHeader><CardTitle>{t('tasks.starterCode')}</CardTitle></CardHeader>
                <CardContent className="p-0">
                    <div className="h-[250px]">
                        <Editor
                            height="250px"
                            language={getMonacoLanguage()}
                            value={editing ? form.starterCode : (task.starterCode || '')}
                            onChange={v => editing && set('starterCode', v || '')}
                            theme={theme === 'dark' ? 'vs-dark' : 'light'}
                            options={editorOptions}
                        />
                    </div>
                </CardContent>
            </Card>

            {/* Test cases */}
            <Card>
                <CardHeader><CardTitle>{t('tasks.testCases')}</CardTitle></CardHeader>
                <CardContent>
                    {editing ? (
                        <textarea
                            value={form.testCases}
                            onChange={e => set('testCases', e.target.value)}
                            rows={8}
                            className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500 resize-none font-mono"
                        />
                    ) : (
                        <pre className="text-sm font-mono text-zinc-600 dark:text-zinc-400 bg-zinc-50 dark:bg-zinc-800/50 p-3 rounded-lg whitespace-pre-wrap">
                            {task.testCases || '—'}
                        </pre>
                    )}
                </CardContent>
            </Card>

            {/* Settings — only in edit mode */}
            {editing && (
                <Card>
                    <CardHeader><CardTitle>{t('tasks.taskSettings')}</CardTitle></CardHeader>
                    <CardContent>
                        <div className="grid grid-cols-3 gap-4">
                            {[
                                { label: t('tasks.maxScore'), field: 'maxScore', min: 1, max: 1000 },
                                { label: t('tasks.timeLimit'), field: 'timeLimitSeconds', min: 1, max: 300 },
                                { label: t('tasks.memoryLimit'), field: 'memoryLimitMb', min: 16, max: 1024 },
                            ].map(({ label, field, min, max }) => (
                                <div key={field}>
                                    <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">{label}</label>
                                    <input
                                        type="number"
                                        value={(form as any)[field]}
                                        onChange={e => set(field, Number(e.target.value))}
                                        min={min} max={max}
                                        className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500"
                                    />
                                </div>
                            ))}
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* Delete Modal */}
            <Modal isOpen={deleteModal} onClose={() => setDeleteModal(false)} title={t('common.confirm')}>
                <p className="text-zinc-600 dark:text-zinc-400">
                    Zadatak "{task.title}" će biti trajno obrisan.
                </p>
                <ModalFooter>
                    <Button variant="ghost" onClick={() => setDeleteModal(false)}>{t('common.cancel')}</Button>
                    <Button variant="danger" onClick={handleDelete} isLoading={deleting}>{t('common.delete')}</Button>
                </ModalFooter>
            </Modal>
        </div>
    );
}