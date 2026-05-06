import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Editor from '@monaco-editor/react';
import { ArrowLeft, Save, Code2, FileText, Settings, TestTube } from 'lucide-react';
import { useTheme } from '../context';
import { taskService, outcomeService } from '../services';
import {
    Card, CardHeader, CardTitle, CardContent,
    Button, Input, LoadingScreen
} from '../components/ui';
import type { LearningOutcome } from '../types';

type Tab = 'basic' | 'code' | 'testing' | 'settings';

export function TaskFormPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { theme } = useTheme();

    const [activeTab, setActiveTab] = useState<Tab>('basic');
    const [outcomes, setOutcomes] = useState<LearningOutcome[]>([]);
    const [loadingOutcomes, setLoadingOutcomes] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

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

    const selectedOutcome = outcomes.find(o => o.id === form.outcomeId);

    const getMonacoLanguage = () => {
        const lang = (selectedOutcome as any)?.languageType || 'C';
        if (lang === 'CSHARP') return 'csharp';
        if (lang === 'PYTHON') return 'python';
        return 'c';
    };

    const getDefaultStarterCode = () => {
        const lang = (selectedOutcome as any)?.languageType || 'C';
        if (lang === 'CSHARP') return `using System;\n\nclass Program\n{\n    static void Main()\n    {\n        // ${t('tasks.starterCodeTitle')}\n    }\n}`;
        if (lang === 'PYTHON') return `# ${t('tasks.starterCodeTitle')}\n\ndef main():\n    pass\n\nif __name__ == "__main__":\n    main()`;
        return `#include <stdio.h>\n\nint main() {\n    // ${t('tasks.starterCodeTitle')}\n    return 0;\n}`;
    };

    useEffect(() => {
        outcomeService.getAll()
            .then(setOutcomes)
            .catch(() => setError(t('errors.generic')))
            .finally(() => setLoadingOutcomes(false));
    }, []);

    useEffect(() => {
        if (form.outcomeId && !form.starterCode) {
            setForm(f => ({ ...f, starterCode: getDefaultStarterCode() }));
        }
    }, [form.outcomeId]);

    const set = (field: string, value: any) =>
        setForm(f => ({ ...f, [field]: value }));

    const handleSubmit = async () => {
        if (!form.title.trim()) { setError(t('tasks.titleRequired')); return; }
        if (!form.outcomeId) { setError(t('tasks.outcomeRequired')); return; }

        setSubmitting(true);
        setError(null);
        try {
            await taskService.create({
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
            navigate('/tasks');
        } catch (err) {
            setError(t('tasks.createError'));
        } finally {
            setSubmitting(false);
        }
    };

    const tabs: { id: Tab; label: string; icon: React.ReactNode }[] = [
        { id: 'basic',    label: t('tasks.basicInfo'), icon: <FileText className="w-4 h-4" /> },
        { id: 'code',     label: t('tasks.code'),      icon: <Code2 className="w-4 h-4" /> },
        { id: 'testing',  label: t('tasks.testing'),   icon: <TestTube className="w-4 h-4" /> },
        { id: 'settings', label: t('tasks.settings'),  icon: <Settings className="w-4 h-4" /> },
    ];

    if (loadingOutcomes) return <LoadingScreen />;

    const outcomesByCourse = outcomes.reduce((acc, o) => {
        const key = o.courseName || t('common.noData');
        if (!acc[key]) acc[key] = [];
        acc[key].push(o);
        return acc;
    }, {} as Record<string, LearningOutcome[]>);

    const editorOptions = {
        minimap: { enabled: false },
        fontSize: 13,
        lineNumbers: 'on' as const,
        scrollBeyondLastLine: false,
        automaticLayout: true,
        tabSize: 4,
    };

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate(-1)}
                        className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors"
                    >
                        <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                    </button>
                    <div>
                        <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {t('tasks.newTask')}
                        </h1>
                        <p className="text-zinc-500 dark:text-zinc-400 mt-1 text-sm">
                            {t('tasks.newTaskDesc')}
                        </p>
                    </div>
                </div>
                <div className="flex items-center gap-3">
                    <Button variant="ghost" onClick={() => navigate(-1)}>
                        {t('common.cancel')}
                    </Button>
                    <Button onClick={handleSubmit} isLoading={submitting} className="gap-2">
                        <Save className="w-4 h-4" />
                        {t('tasks.saveTask')}
                    </Button>
                </div>
            </div>

            {/* Error */}
            {error && (
                <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg text-red-700 dark:text-red-400 text-sm">
                    {error}
                </div>
            )}

            {/* Tabs */}
            <div className="flex gap-1 border-b border-zinc-200 dark:border-zinc-800">
                {tabs.map(tab => (
                    <button
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
                            activeTab === tab.id
                                ? 'border-zinc-900 dark:border-white text-zinc-900 dark:text-white'
                                : 'border-transparent text-zinc-500 dark:text-zinc-400 hover:text-zinc-700 dark:hover:text-zinc-300'
                        }`}
                    >
                        {tab.icon}
                        {tab.label}
                    </button>
                ))}
            </div>

            {/* Tab: Osnovno */}
            {activeTab === 'basic' && (
                <div className="space-y-4">
                    <Card>
                        <CardHeader>
                            <CardTitle>{t('tasks.taskInfo')}</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <Input
                                label={`${t('tasks.taskTitle')} *`}
                                value={form.title}
                                onChange={e => set('title', e.target.value)}
                                placeholder={t('tasks.taskTitlePlaceholder')}
                            />
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                                    {t('common.description')} ({t('common.optional')})
                                </label>
                                <textarea
                                    value={form.description}
                                    onChange={e => set('description', e.target.value)}
                                    placeholder={t('tasks.descriptionPlaceholder')}
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
                                    <option value="">{t('tasks.selectOutcome')}</option>
                                    {Object.entries(outcomesByCourse).map(([course, outs]) => (
                                        <optgroup key={course} label={course}>
                                            {outs.map(o => (
                                                <option key={o.id} value={o.id}>
                                                    {o.name}
                                                </option>
                                            ))}
                                        </optgroup>
                                    ))}
                                </select>
                                {selectedOutcome && (
                                    <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
                                        {t('tasks.courseLabel')}: {selectedOutcome.courseName}
                                    </p>
                                )}
                            </div>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardHeader>
                            <CardTitle>{t('tasks.studentInstructions')}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <textarea
                                value={form.gradingCriteria}
                                onChange={e => set('gradingCriteria', e.target.value)}
                                placeholder={t('tasks.instructionsPlaceholder')}
                                rows={6}
                                className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500 resize-none font-mono"
                            />
                            <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
                                {t('tasks.instructionsHint')}
                            </p>
                        </CardContent>
                    </Card>
                </div>
            )}

            {/* Tab: Kod */}
            {activeTab === 'code' && (
                <div className="space-y-4">
                    {!form.outcomeId && (
                        <div className="p-4 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg text-amber-700 dark:text-amber-400 text-sm">
                            {t('tasks.selectOutcomeFirst')}
                        </div>
                    )}
                    <Card>
                        <CardHeader>
                            <CardTitle>{t('tasks.starterCodeTitle')}</CardTitle>
                        </CardHeader>
                        <CardContent className="p-0">
                            <div className="h-[300px]">
                                <Editor
                                    height="300px"
                                    language={getMonacoLanguage()}
                                    value={form.starterCode}
                                    onChange={v => set('starterCode', v || '')}
                                    theme={theme === 'dark' ? 'vs-dark' : 'light'}
                                    options={editorOptions}
                                />
                            </div>
                        </CardContent>
                    </Card>
                    <Card>
                        <CardHeader>
                            <CardTitle>{t('tasks.solutionCodeTitle')}</CardTitle>
                        </CardHeader>
                        <CardContent className="p-0">
                            <div className="h-[300px]">
                                <Editor
                                    height="300px"
                                    language={getMonacoLanguage()}
                                    value={form.solutionCode}
                                    onChange={v => set('solutionCode', v || '')}
                                    theme={theme === 'dark' ? 'vs-dark' : 'light'}
                                    options={editorOptions}
                                />
                            </div>
                        </CardContent>
                    </Card>
                </div>
            )}

            {/* Tab: Testiranje */}
            {activeTab === 'testing' && (
                <Card>
                    <CardHeader>
                        <CardTitle>{t('tasks.testCases')}</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <p className="text-sm text-zinc-500 dark:text-zinc-400">
                            {t('tasks.testCasesFormat')}
                        </p>
                        <textarea
                            value={form.testCases}
                            onChange={e => set('testCases', e.target.value)}
                            placeholder={`5 3|8\n10 20|30\n0 0|0`}
                            rows={10}
                            className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500 resize-none font-mono"
                        />
                        <p className="text-xs text-zinc-500 dark:text-zinc-400">
                            {t('tasks.testCasesHint')}
                        </p>
                    </CardContent>
                </Card>
            )}

            {/* Tab: Postavke */}
            {activeTab === 'settings' && (
                <Card>
                    <CardHeader>
                        <CardTitle>{t('tasks.taskSettings')}</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="grid grid-cols-3 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                                    {t('tasks.maxScore')}
                                </label>
                                <input
                                    type="number"
                                    value={form.maxScore}
                                    onChange={e => set('maxScore', Number(e.target.value))}
                                    min={1} max={1000}
                                    className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                                    {t('tasks.timeLimit')}
                                </label>
                                <input
                                    type="number"
                                    value={form.timeLimitSeconds}
                                    onChange={e => set('timeLimitSeconds', Number(e.target.value))}
                                    min={1} max={300}
                                    className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                                    {t('tasks.memoryLimit')}
                                </label>
                                <input
                                    type="number"
                                    value={form.memoryLimitMb}
                                    onChange={e => set('memoryLimitMb', Number(e.target.value))}
                                    min={16} max={1024}
                                    className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500"
                                />
                            </div>
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                                    {t('tasks.dueDate')} ({t('common.optional')})
                                </label>
                                <input
                                    type="datetime-local"
                                    value={form.dueDate}
                                    onChange={e => set('dueDate', e.target.value)}
                                    className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1.5">
                                    {t('tasks.orderIndex')}
                                </label>
                                <input
                                    type="number"
                                    value={form.orderIndex}
                                    onChange={e => set('orderIndex', Number(e.target.value))}
                                    min={0}
                                    className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500"
                                />
                            </div>
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* Footer */}
            <div className="flex justify-end gap-3 pt-4 border-t border-zinc-200 dark:border-zinc-800">
                <Button variant="ghost" onClick={() => navigate(-1)}>
                    {t('common.cancel')}
                </Button>
                <Button onClick={handleSubmit} isLoading={submitting} className="gap-2">
                    <Save className="w-4 h-4" />
                    {t('tasks.saveTask')}
                </Button>
            </div>
        </div>
    );
}