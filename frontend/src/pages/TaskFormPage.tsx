import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Save, Plus, Trash2 } from 'lucide-react';
import { taskService, courseService } from '../services';
import {
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    Button,
    LoadingScreen
} from '../components/ui';
import type { Course, LearningOutcome, TaskType } from '../types';

export function TaskFormPage() {
    const { t } = useTranslation();
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const isEditing = Boolean(id);

    const [courses, setCourses] = useState<Course[]>([]);
    const [outcomes, setOutcomes] = useState<LearningOutcome[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [instructions, setInstructions] = useState('');
    const [taskType, setTaskType] = useState<TaskType>('CODE');
    const [options, setOptions] = useState<string[]>(['', '', '', '']);
    const [correctAnswer, setCorrectAnswer] = useState('');
    const [starterCode, setStarterCode] = useState('');
    const [solutionCode, setSolutionCode] = useState('');
    const [testCases, setTestCases] = useState('');
    const [gradingCriteria, setGradingCriteria] = useState('');
    const [maxScore, setMaxScore] = useState(100);
    const [timeLimitSeconds, setTimeLimitSeconds] = useState(30);
    const [memoryLimitMb, setMemoryLimitMb] = useState(256);
    const [selectedCourseId, setSelectedCourseId] = useState('');
    const [outcomeId, setOutcomeId] = useState('');
    const [dueDate, setDueDate] = useState('');

    useEffect(() => {
        loadData();
    }, [id]);

    useEffect(() => {
        if (selectedCourseId) {
            loadOutcomes(selectedCourseId);
        } else {
            setOutcomes([]);
        }
    }, [selectedCourseId]);

    const loadData = async () => {
        try {
            setLoading(true);
            const coursesData = await courseService.getAll();
            setCourses(coursesData);

            if (id) {
                const task = await taskService.getById(id);
                setTitle(task.title);
                setDescription(task.description || '');
                setInstructions(task.instructions || '');
                setTaskType(task.taskType || 'CODE');
                if (task.options) {
                    try {
                        setOptions(JSON.parse(task.options));
                    } catch {
                        setOptions(['', '', '', '']);
                    }
                }
                setStarterCode(task.starterCode || '');
                setTestCases(task.testCases || '');
                setGradingCriteria(task.gradingCriteria || '');
                setMaxScore(task.maxScore);
                setTimeLimitSeconds(task.timeLimitSeconds);
                setMemoryLimitMb(task.memoryLimitMb);
                if (task.courseId) {
                    setSelectedCourseId(task.courseId);
                }
                setOutcomeId(task.outcomeId);
                if (task.dueDate) {
                    setDueDate(task.dueDate.slice(0, 16));
                }
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setLoading(false);
        }
    };

    const loadOutcomes = async (courseId: string) => {
        try {
            const outcomesData = await courseService.getOutcomes(courseId);
            setOutcomes(outcomesData);
        } catch {
            setOutcomes([]);
        }
    };

    const handleOptionChange = (index: number, value: string) => {
        const newOptions = [...options];
        newOptions[index] = value;
        setOptions(newOptions);
    };

    const handleAddOption = () => {
        setOptions([...options, '']);
    };

    const handleRemoveOption = (index: number) => {
        if (options.length > 2) {
            setOptions(options.filter((_, i) => i !== index));
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!title.trim() || !outcomeId) {
            setError(t('tasks.form.titleRequired'));
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const data = {
                title: title.trim(),
                description: description.trim() || undefined,
                instructions: instructions.trim() || undefined,
                taskType,
                options: ['MULTIPLE_CHOICE', 'CHECKLIST'].includes(taskType)
                    ? JSON.stringify(options.filter(o => o.trim()))
                    : undefined,
                correctAnswer: taskType !== 'CODE' ? correctAnswer : undefined,
                starterCode: taskType === 'CODE' ? starterCode : undefined,
                solutionCode: taskType === 'CODE' ? solutionCode : undefined,
                testCases: taskType === 'CODE' ? testCases : undefined,
                gradingCriteria: gradingCriteria.trim() || undefined,
                maxScore,
                timeLimitSeconds,
                memoryLimitMb,
                outcomeId,
                dueDate: dueDate || undefined
            };

            if (isEditing && id) {
                await taskService.update(id, data);
            } else {
                await taskService.create(data);
            }

            navigate('/tasks');
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <LoadingScreen />;

    return (
        <div className="space-y-6 animate-fade-in">
            <div className="flex items-center gap-4">
                <button
                    onClick={() => navigate('/tasks')}
                    className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800"
                >
                    <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                </button>
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {isEditing ? t('tasks.editTask') : t('tasks.newTask')}
                    </h1>
                </div>
            </div>

            {error && (
                <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
                    <p className="text-red-700 dark:text-red-300">{error}</p>
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <Card>
                    <CardHeader>
                        <CardTitle>{t('tasks.form.basicInfo')}</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="grid gap-4 sm:grid-cols-2">
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('tasks.form.title')} *
                                </label>
                                <input
                                    type="text"
                                    value={title}
                                    onChange={(e) => setTitle(e.target.value)}
                                    className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                    required
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('tasks.form.taskType')}
                                </label>
                                <select
                                    value={taskType}
                                    onChange={(e) => setTaskType(e.target.value as TaskType)}
                                    className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                >
                                    <option value="CODE">{t('tasks.form.typeCode')}</option>
                                    <option value="TEXT">{t('tasks.form.typeText')}</option>
                                    <option value="MULTIPLE_CHOICE">{t('tasks.form.typeMultipleChoice')}</option>
                                    <option value="CHECKLIST">{t('tasks.form.typeChecklist')}</option>
                                </select>
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('tasks.form.description')}
                            </label>
                            <textarea
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                rows={3}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('tasks.form.instructions')}
                            </label>
                            <textarea
                                value={instructions}
                                onChange={(e) => setInstructions(e.target.value)}
                                rows={3}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>

                        <div className="grid gap-4 sm:grid-cols-2">
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('tasks.form.course')} *
                                </label>
                                <select
                                    value={selectedCourseId}
                                    onChange={(e) => {
                                        setSelectedCourseId(e.target.value);
                                        setOutcomeId('');
                                    }}
                                    className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                    required
                                >
                                    <option value="">{t('tasks.form.selectCourse')}</option>
                                    {courses.map(course => (
                                        <option key={course.id} value={course.id}>
                                            {course.name}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('tasks.form.outcome')} *
                                </label>
                                <select
                                    value={outcomeId}
                                    onChange={(e) => setOutcomeId(e.target.value)}
                                    className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                    required
                                    disabled={!selectedCourseId}
                                >
                                    <option value="">{t('tasks.form.selectOutcome')}</option>
                                    {outcomes.map(outcome => (
                                        <option key={outcome.id} value={outcome.id}>
                                            {outcome.name}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {['MULTIPLE_CHOICE', 'CHECKLIST'].includes(taskType) && (
                    <Card className="mt-6">
                        <CardHeader>
                            <div className="flex items-center justify-between">
                                <CardTitle>{t('tasks.form.options')}</CardTitle>
                                <Button type="button" variant="secondary" onClick={handleAddOption} className="gap-2">
                                    <Plus className="w-4 h-4" />
                                    {t('tasks.form.addOption')}
                                </Button>
                            </div>
                        </CardHeader>
                        <CardContent className="space-y-3">
                            {options.map((option, index) => (
                                <div key={index} className="flex items-center gap-2">
                                    <input
                                        type="text"
                                        value={option}
                                        onChange={(e) => handleOptionChange(index, e.target.value)}
                                        placeholder={`${t('tasks.form.option')} ${index + 1}`}
                                        className="flex-1 px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                    />
                                    {options.length > 2 && (
                                        <button
                                            type="button"
                                            onClick={() => handleRemoveOption(index)}
                                            className="p-2 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    )}
                                </div>
                            ))}

                            <div className="mt-4">
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('tasks.form.correctAnswer')}
                                </label>
                                {taskType === 'MULTIPLE_CHOICE' ? (
                                    <select
                                        value={correctAnswer}
                                        onChange={(e) => setCorrectAnswer(e.target.value)}
                                        className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                    >
                                        <option value="">{t('tasks.form.selectCorrectAnswer')}</option>
                                        {options.filter(o => o.trim()).map((option, index) => (
                                            <option key={index} value={option}>
                                                {option}
                                            </option>
                                        ))}
                                    </select>
                                ) : (
                                    <p className="text-sm text-zinc-500">
                                        {t('tasks.form.checklistHint')}
                                    </p>
                                )}
                            </div>
                        </CardContent>
                    </Card>
                )}

                {taskType === 'TEXT' && (
                    <Card className="mt-6">
                        <CardHeader>
                            <CardTitle>{t('tasks.form.textSettings')}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('tasks.form.expectedAnswer')}
                                </label>
                                <textarea
                                    value={correctAnswer}
                                    onChange={(e) => setCorrectAnswer(e.target.value)}
                                    rows={3}
                                    placeholder={t('tasks.form.expectedAnswerHint')}
                                    className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                />
                            </div>
                        </CardContent>
                    </Card>
                )}

                {taskType === 'CODE' && (
                    <Card className="mt-6">
                        <CardHeader>
                            <CardTitle>{t('tasks.form.codeSettings')}</CardTitle>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('tasks.form.starterCode')}
                                </label>
                                <textarea
                                    value={starterCode}
                                    onChange={(e) => setStarterCode(e.target.value)}
                                    rows={6}
                                    className="w-full px-3 py-2 font-mono text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('tasks.form.solutionCode')}
                                </label>
                                <textarea
                                    value={solutionCode}
                                    onChange={(e) => setSolutionCode(e.target.value)}
                                    rows={6}
                                    className="w-full px-3 py-2 font-mono text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('tasks.form.testCases')}
                                </label>
                                <textarea
                                    value={testCases}
                                    onChange={(e) => setTestCases(e.target.value)}
                                    rows={4}
                                    placeholder='[{"input": "5", "expectedOutput": "25"}]'
                                    className="w-full px-3 py-2 font-mono text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                />
                            </div>

                            <div className="grid gap-4 sm:grid-cols-3">
                                <div>
                                    <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                        {t('tasks.form.timeLimit')}
                                    </label>
                                    <input
                                        type="number"
                                        value={timeLimitSeconds}
                                        onChange={(e) => setTimeLimitSeconds(parseInt(e.target.value) || 30)}
                                        min={1}
                                        className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                        {t('tasks.form.memoryLimit')}
                                    </label>
                                    <input
                                        type="number"
                                        value={memoryLimitMb}
                                        onChange={(e) => setMemoryLimitMb(parseInt(e.target.value) || 256)}
                                        min={1}
                                        className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                        {t('tasks.form.maxScore')}
                                    </label>
                                    <input
                                        type="number"
                                        value={maxScore}
                                        onChange={(e) => setMaxScore(parseInt(e.target.value) || 100)}
                                        min={1}
                                        className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                    />
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                )}

                <Card className="mt-6">
                    <CardHeader>
                        <CardTitle>{t('tasks.form.grading')}</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('tasks.form.gradingCriteria')}
                            </label>
                            <textarea
                                value={gradingCriteria}
                                onChange={(e) => setGradingCriteria(e.target.value)}
                                rows={3}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>

                        {taskType !== 'CODE' && (
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('tasks.form.maxScore')}
                                </label>
                                <input
                                    type="number"
                                    value={maxScore}
                                    onChange={(e) => setMaxScore(parseInt(e.target.value) || 100)}
                                    min={1}
                                    className="w-full max-w-xs px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                />
                            </div>
                        )}

                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('tasks.form.dueDate')}
                            </label>
                            <input
                                type="datetime-local"
                                value={dueDate}
                                onChange={(e) => setDueDate(e.target.value)}
                                className="w-full max-w-xs px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>
                    </CardContent>
                </Card>

                <div className="flex justify-end mt-6">
                    <Button type="submit" isLoading={saving} className="gap-2">
                        <Save className="w-4 h-4" />
                        {isEditing ? t('common.save') : t('tasks.form.createTask')}
                    </Button>
                </div>
            </form>
        </div>
    );
}