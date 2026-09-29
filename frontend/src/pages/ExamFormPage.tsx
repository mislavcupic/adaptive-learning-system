import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
    ArrowLeft, Save, Plus, X, Search,
    Code, List, CheckSquare, AlignLeft
} from 'lucide-react';
import { examService, courseService, taskService } from '../services';
import {
    Card, CardHeader, CardTitle, CardContent,
    Button, LoadingScreen
} from '../components/ui';
import type { Course, Task, TaskType } from '../types';

const TYPE_ICON: Record<TaskType, React.ReactNode> = {
    CODE: <Code className="w-3.5 h-3.5" />,
    TEXT: <AlignLeft className="w-3.5 h-3.5" />,
    MULTIPLE_CHOICE: <List className="w-3.5 h-3.5" />,
    CHECKLIST: <CheckSquare className="w-3.5 h-3.5" />,
};

export function ExamFormPage() {
    const { t } = useTranslation();
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const isEditing = Boolean(id);

    const typeLabel = (type: TaskType) => {
        const map: Record<TaskType, string> = {
            CODE: t('exams.taskType.code'),
            TEXT: t('exams.taskType.text'),
            MULTIPLE_CHOICE: t('exams.taskType.multipleChoice'),
            CHECKLIST: t('exams.taskType.checklist'),
        };
        return map[type] ?? type;
    };

    const [courses, setCourses] = useState<Course[]>([]);
    const [allTasks, setAllTasks] = useState<Task[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [instructions, setInstructions] = useState('');
    const [courseId, setCourseId] = useState('');
    const [timeLimitMinutes, setTimeLimitMinutes] = useState<number | ''>('');
    const [passingScore, setPassingScore] = useState(50);
    const [availableFrom, setAvailableFrom] = useState('');
    const [availableUntil, setAvailableUntil] = useState('');
    const [showTestResults, setShowTestResults] = useState(true);

    const [selectedIds, setSelectedIds] = useState<string[]>([]);
    const [search, setSearch] = useState('');

    useEffect(() => {
        init();
    }, [id]);

    useEffect(() => {
        if (courseId) loadTasks(courseId);
    }, [courseId]);

    const init = async () => {
        try {
            setLoading(true);
            const courseList = await courseService.getAll();
            setCourses(courseList);

            if (id) {
                const exam = await examService.getById(id);
                setTitle(exam.title);
                setDescription(exam.description ?? '');
                setInstructions(exam.instructions ?? '');
                setCourseId(exam.courseId);
                setTimeLimitMinutes(exam.timeLimitMinutes ?? '');
                setPassingScore(exam.passingScore ?? 50);
                setAvailableFrom(exam.availableFrom?.slice(0, 16) ?? '');
                setAvailableUntil(exam.availableUntil?.slice(0, 16) ?? '');
                setShowTestResults(exam.showTestResults);
                setSelectedIds(exam.tasks?.map(tk => tk.taskId) ?? []);
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setLoading(false);
        }
    };

    const loadTasks = async (course: string) => {
        try {
            const all = await taskService.getAll();
            setAllTasks(all.filter(tk => tk.courseId === course));
        } catch {
            setAllTasks([]);
        }
    };

    const addTask = (taskId: string) => {
        if (!selectedIds.includes(taskId)) {
            setSelectedIds([...selectedIds, taskId]);
        }
    };

    const removeTask = (taskId: string) => {
        setSelectedIds(selectedIds.filter(tk => tk !== taskId));
    };

    const move = (index: number, direction: -1 | 1) => {
        const target = index + direction;
        if (target < 0 || target >= selectedIds.length) return;

        const next = [...selectedIds];
        [next[index], next[target]] = [next[target], next[index]];
        setSelectedIds(next);
    };

    const selectedTasks = selectedIds
        .map(tid => allTasks.find(tk => tk.id === tid))
        .filter((tk): tk is Task => Boolean(tk));

    const availableTasks = allTasks.filter(tk =>
        !selectedIds.includes(tk.id) &&
        (search === '' || tk.title.toLowerCase().includes(search.toLowerCase()))
    );

    const totalScore = selectedTasks.reduce((sum, tk) => sum + (tk.maxScore ?? 0), 0);

    const handleSave = async () => {
        if (!title.trim() || !courseId) {
            setError(t('exams.form.titleAndCourseRequired'));
            return;
        }
        if (selectedIds.length === 0) {
            setError(t('exams.form.atLeastOneTask'));
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const payload = {
                title: title.trim(),
                description: description.trim() || undefined,
                instructions: instructions.trim() || undefined,
                courseId,
                taskIds: selectedIds,
                timeLimitMinutes: timeLimitMinutes === '' ? null : Number(timeLimitMinutes),
                passingScore,
                availableFrom: availableFrom || null,
                availableUntil: availableUntil || null,
                showTestResults,
            };

            if (isEditing && id) {
                await examService.update(id, payload);
            } else {
                await examService.create(payload);
            }

            navigate('/exams');
        } catch (err) {
            setError(err instanceof Error ? err.message : t('exams.form.saveFailed'));
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <LoadingScreen />;

    return (
        <div className="space-y-6 animate-fade-in pb-12">
            <div className="flex items-center gap-4">
                <button
                    onClick={() => navigate('/exams')}
                    className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800"
                >
                    <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                </button>
                <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                    {isEditing ? t('exams.editExam') : t('exams.newExam')}
                </h1>
            </div>

            {error && (
                <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
                    <p className="text-sm text-red-700 dark:text-red-300">{error}</p>
                </div>
            )}

            {/* Osnovni podaci */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('exams.form.basicInfo')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="grid gap-4 sm:grid-cols-2">
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('exams.form.examTitle')} *
                            </label>
                            <input
                                type="text"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                placeholder={t('exams.form.examTitlePlaceholder')}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('exams.course')} *
                            </label>
                            <select
                                value={courseId}
                                onChange={(e) => {
                                    setCourseId(e.target.value);
                                    setSelectedIds([]);
                                }}
                                disabled={isEditing}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white disabled:opacity-60"
                            >
                                <option value="">{t('exams.form.selectCourse')}</option>
                                {courses.map(c => (
                                    <option key={c.id} value={c.id}>{c.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                            {t('exams.form.description')}
                        </label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={2}
                            className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                            {t('exams.form.instructions')}
                        </label>
                        <textarea
                            value={instructions}
                            onChange={(e) => setInstructions(e.target.value)}
                            rows={3}
                            placeholder={t('exams.form.instructionsPlaceholder')}
                            className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                        />
                    </div>
                </CardContent>
            </Card>

            {/* Zadaci */}
            <Card>
                <CardHeader>
                    <div className="flex items-center justify-between">
                        <CardTitle>{t('exams.form.tasksTitle')}</CardTitle>
                        <span className="text-sm text-zinc-500">
                            {t('exams.form.selectedCount', {
                                count: selectedTasks.length,
                                points: totalScore
                            })}
                        </span>
                    </div>
                </CardHeader>
                <CardContent>
                    {!courseId ? (
                        <p className="text-sm text-zinc-500 py-4 text-center">
                            {t('exams.form.selectCourseFirst')}
                        </p>
                    ) : (
                        <div className="grid gap-5 lg:grid-cols-2">
                            {/* Odabrani */}
                            <div>
                                <p className="text-xs font-medium text-zinc-500 uppercase tracking-wide mb-2">
                                    {t('exams.form.inExam')}
                                </p>
                                {selectedTasks.length === 0 ? (
                                    <div className="p-6 rounded-lg border border-dashed border-zinc-300 dark:border-zinc-700 text-center">
                                        <p className="text-sm text-zinc-400">
                                            {t('exams.form.addFromRight')}
                                        </p>
                                    </div>
                                ) : (
                                    <div className="space-y-2">
                                        {selectedTasks.map((task, i) => (
                                            <div
                                                key={task.id}
                                                className="flex items-center gap-2 p-3 rounded-lg border border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800/50"
                                            >
                                                <div className="flex flex-col">
                                                    <button
                                                        onClick={() => move(i, -1)}
                                                        disabled={i === 0}
                                                        className="text-zinc-400 hover:text-zinc-700 disabled:opacity-30 text-xs leading-none"
                                                    >
                                                        ▲
                                                    </button>
                                                    <button
                                                        onClick={() => move(i, 1)}
                                                        disabled={i === selectedTasks.length - 1}
                                                        className="text-zinc-400 hover:text-zinc-700 disabled:opacity-30 text-xs leading-none"
                                                    >
                                                        ▼
                                                    </button>
                                                </div>

                                                <span className="w-6 text-sm text-zinc-400">{i + 1}.</span>

                                                <div className="min-w-0 flex-1">
                                                    <p className="text-sm font-medium text-zinc-900 dark:text-white truncate">
                                                        {task.title}
                                                    </p>
                                                    <div className="flex items-center gap-2 mt-0.5">
                                                        <span className="text-xs text-zinc-400 flex items-center gap-1">
                                                            {TYPE_ICON[task.taskType]}
                                                            {typeLabel(task.taskType)}
                                                        </span>
                                                        <span className="text-xs text-zinc-400">
                                                            {task.maxScore} {t('exams.points')}
                                                        </span>
                                                    </div>
                                                </div>

                                                <button
                                                    onClick={() => removeTask(task.id)}
                                                    className="p-1 text-zinc-400 hover:text-red-500"
                                                >
                                                    <X className="w-4 h-4" />
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            {/* Dostupni */}
                            <div>
                                <p className="text-xs font-medium text-zinc-500 uppercase tracking-wide mb-2">
                                    {t('exams.form.availableTasks')}
                                </p>

                                <div className="relative mb-2">
                                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400" />
                                    <input
                                        type="text"
                                        value={search}
                                        onChange={(e) => setSearch(e.target.value)}
                                        placeholder={t('exams.form.searchTasks')}
                                        className="w-full pl-9 pr-3 py-2 text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                    />
                                </div>

                                <div className="space-y-2 max-h-96 overflow-y-auto pr-1">
                                    {availableTasks.length === 0 ? (
                                        <p className="text-sm text-zinc-400 py-4 text-center">
                                            {t('exams.form.noAvailableTasks')}
                                        </p>
                                    ) : (
                                        availableTasks.map(task => (
                                            <button
                                                key={task.id}
                                                onClick={() => addTask(task.id)}
                                                className="w-full text-left flex items-center gap-2 p-3 rounded-lg border border-zinc-200 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-800 transition-colors"
                                            >
                                                <Plus className="w-4 h-4 text-zinc-400 shrink-0" />
                                                <div className="min-w-0 flex-1">
                                                    <p className="text-sm font-medium text-zinc-900 dark:text-white truncate">
                                                        {task.title}
                                                    </p>
                                                    <div className="flex items-center gap-2 mt-0.5">
                                                        <span className="text-xs text-zinc-400 flex items-center gap-1">
                                                            {TYPE_ICON[task.taskType]}
                                                            {typeLabel(task.taskType)}
                                                        </span>
                                                        <span className="text-xs text-zinc-400">
                                                            {task.maxScore} {t('exams.points')}
                                                        </span>
                                                    </div>
                                                </div>
                                            </button>
                                        ))
                                    )}
                                </div>
                            </div>
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* Postavke */}
            <Card>
                <CardHeader>
                    <CardTitle>{t('exams.form.settings')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="grid gap-4 sm:grid-cols-2">
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('exams.form.timeLimit')}
                            </label>
                            <input
                                type="number"
                                value={timeLimitMinutes}
                                onChange={(e) => setTimeLimitMinutes(
                                    e.target.value === '' ? '' : Number(e.target.value)
                                )}
                                min={1}
                                placeholder={t('exams.form.noTimeLimit')}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('exams.form.passingScore')}
                            </label>
                            <input
                                type="number"
                                value={passingScore}
                                onChange={(e) => setPassingScore(Number(e.target.value))}
                                min={0}
                                max={100}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>
                    </div>

                    <div className="grid gap-4 sm:grid-cols-2">
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('exams.form.availableFrom')}
                            </label>
                            <input
                                type="datetime-local"
                                value={availableFrom}
                                onChange={(e) => setAvailableFrom(e.target.value)}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('exams.form.availableUntil')}
                            </label>
                            <input
                                type="datetime-local"
                                value={availableUntil}
                                onChange={(e) => setAvailableUntil(e.target.value)}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>
                    </div>

                    <label className="flex items-start gap-3 p-3 rounded-lg border border-zinc-200 dark:border-zinc-700 cursor-pointer">
                        <input
                            type="checkbox"
                            checked={showTestResults}
                            onChange={(e) => setShowTestResults(e.target.checked)}
                            className="w-4 h-4 mt-0.5 rounded"
                        />
                        <div>
                            <p className="text-sm font-medium text-zinc-900 dark:text-white">
                                {t('exams.form.showTestResults')}
                            </p>
                            <p className="text-xs text-zinc-500 mt-0.5">
                                {t('exams.form.showTestResultsHint')}
                            </p>
                        </div>
                    </label>
                </CardContent>
            </Card>

            <div className="flex justify-end">
                <Button onClick={handleSave} isLoading={saving} className="gap-2">
                    <Save className="w-4 h-4" />
                    {isEditing ? t('exams.form.saveChanges') : t('exams.form.create')}
                </Button>
            </div>
        </div>
    );
}