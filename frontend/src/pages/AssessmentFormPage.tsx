import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Plus, Trash2, Save, GripVertical } from 'lucide-react';
import { assessmentService, courseService } from '../services';
import {
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    Button,
    Badge,
    LoadingScreen
} from '../components/ui';
import type { Assessment, Course, QuestionType } from '../types';

interface QuestionForm {
    id?: string;
    questionText: string;
    questionType: QuestionType;
    options: string[];
    correctAnswer: string;
    codeTemplate: string;
    testCases: string;
    points: number;
}

const emptyQuestion: QuestionForm = {
    questionText: '',
    questionType: 'MULTIPLE_CHOICE',
    options: ['', '', '', ''],
    correctAnswer: '',
    codeTemplate: '',
    testCases: '',
    points: 1
};

export function AssessmentFormPage() {
    const { t } = useTranslation();
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const isEditing = Boolean(id);

    const [courses, setCourses] = useState<Course[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [assessmentType, setAssessmentType] = useState<'PRETEST' | 'POSTTEST'>('PRETEST');
    const [courseId, setCourseId] = useState('');
    const [timeLimitMinutes, setTimeLimitMinutes] = useState<number | ''>('');
    const [passingScore, setPassingScore] = useState(60);

    const [assessment, setAssessment] = useState<Assessment | null>(null);
    const [questions, setQuestions] = useState<QuestionForm[]>([]);
    const [activeQuestion, setActiveQuestion] = useState<number | null>(null);

    useEffect(() => {
        loadData();
    }, [id]);

    const loadData = async () => {
        try {
            setLoading(true);
            const coursesData = await courseService.getAll();
            setCourses(coursesData);

            if (id) {
                const assessmentData = await assessmentService.getById(id);
                setAssessment(assessmentData);
                setTitle(assessmentData.title);
                setDescription(assessmentData.description || '');
                setAssessmentType(assessmentData.assessmentType);
                setCourseId(assessmentData.courseId);
                setTimeLimitMinutes(assessmentData.timeLimitMinutes || '');
                setPassingScore(assessmentData.passingScore);

                if (assessmentData.questions) {
                    setQuestions(assessmentData.questions.map(q => ({
                        id: q.id,
                        questionText: q.questionText,
                        questionType: q.questionType,
                        options: q.options ? JSON.parse(q.options) : ['', '', '', ''],
                        correctAnswer: '',
                        codeTemplate: q.codeTemplate || '',
                        testCases: '',
                        points: q.points
                    })));
                }
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setLoading(false);
        }
    };

    const handleSaveAssessment = async () => {
        if (!title.trim() || !courseId) {
            setError(t('assessments.form.enterNameAndCourse'));
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const data = {
                title: title.trim(),
                description: description.trim() || undefined,
                assessmentType,
                courseId,
                timeLimitMinutes: timeLimitMinutes || undefined,
                passingScore
            };

            const saved = await assessmentService.create(data);
            setAssessment(saved);
            navigate(`/assessments/${saved.id}/edit`, { replace: true });
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setSaving(false);
        }
    };

    const handleAddQuestion = () => {
        setQuestions([...questions, { ...emptyQuestion }]);
        setActiveQuestion(questions.length);
    };

    const handleRemoveQuestion = async (index: number) => {
        const question = questions[index];

        if (question.id) {
            try {
                await assessmentService.deleteQuestion(question.id);
            } catch (err) {
                setError(err instanceof Error ? err.message : t('errors.generic'));
                return;
            }
            setQuestions(prev => prev.filter(q => q.id !== question.id));
        } else {
            setQuestions(prev => prev.filter((_, i) => i !== index));
        }

        setActiveQuestion(null);
    };

    const handleQuestionChange = (index: number, field: keyof QuestionForm, value: unknown) => {
        setQuestions(questions.map((q, i) =>
            i === index ? { ...q, [field]: value } : q
        ));
    };

    const handleOptionChange = (questionIndex: number, optionIndex: number, value: string) => {
        setQuestions(questions.map((q, i) => {
            if (i !== questionIndex) return q;
            const newOptions = [...q.options];
            newOptions[optionIndex] = value;
            return { ...q, options: newOptions };
        }));
    };

    const handleSaveQuestion = async (index: number) => {
        const question = questions[index];

        if (!question.questionText.trim()) {
            setError(t('assessments.questionForm.enterQuestionText'));
            return;
        }

        if (!assessment) {
            setError(t('assessments.questionForm.saveAssessmentFirst'));
            return;
        }

        setSaving(true);
        setError(null);

        try {
            const data = {
                assessmentId: assessment.id,
                questionText: question.questionText.trim(),
                questionType: question.questionType,
                options: question.questionType === 'MULTIPLE_CHOICE'
                    ? JSON.stringify(question.options.filter(o => o.trim()))
                    : undefined,
                correctAnswer: question.correctAnswer,
                codeTemplate: question.questionType === 'CODE' ? question.codeTemplate : undefined,
                testCases: question.questionType === 'CODE' ? question.testCases : undefined,
                points: question.points,
                orderIndex: index
            };

            await assessmentService.addQuestion(data);
            await loadData();
            setActiveQuestion(null);
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
                    onClick={() => navigate('/assessments/manage')}
                    className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800"
                >
                    <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                </button>
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {isEditing ? t('assessments.editAssessment') : t('assessments.newAssessment')}
                    </h1>
                    <p className="text-sm text-zinc-500 mt-1">
                        {t('assessments.subtitle')}
                    </p>
                </div>
            </div>

            {error && (
                <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
                    <p className="text-red-700 dark:text-red-300">{error}</p>
                </div>
            )}

            <Card>
                <CardHeader>
                    <CardTitle>{t('assessments.form.title')}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="grid gap-4 sm:grid-cols-2">
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('assessments.form.testName')} *
                            </label>
                            <input
                                type="text"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                placeholder={t('assessments.form.testNamePlaceholder')}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                disabled={Boolean(assessment)}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('assessments.form.course')} *
                            </label>
                            <select
                                value={courseId}
                                onChange={(e) => setCourseId(e.target.value)}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                disabled={Boolean(assessment)}
                            >
                                <option value="">{t('assessments.form.selectCourse')}</option>
                                {courses.map(course => (
                                    <option key={course.id} value={course.id}>
                                        {course.name}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                            {t('assessments.form.description')}
                        </label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={2}
                            placeholder={t('assessments.form.descriptionPlaceholder')}
                            className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            disabled={Boolean(assessment)}
                        />
                    </div>

                    <div className="grid gap-4 sm:grid-cols-3">
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('assessments.form.testType')} *
                            </label>
                            <select
                                value={assessmentType}
                                onChange={(e) => setAssessmentType(e.target.value as 'PRETEST' | 'POSTTEST')}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                disabled={Boolean(assessment)}
                            >
                                <option value="PRETEST">{t('assessments.pretest')}</option>
                                <option value="POSTTEST">{t('assessments.posttest')}</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('assessments.form.timeLimit')}
                            </label>
                            <input
                                type="number"
                                value={timeLimitMinutes}
                                onChange={(e) => setTimeLimitMinutes(e.target.value ? parseInt(e.target.value) : '')}
                                placeholder={t('assessments.form.timeLimitPlaceholder')}
                                min={1}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                disabled={Boolean(assessment)}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('assessments.form.passingScore')}
                            </label>
                            <input
                                type="number"
                                value={passingScore}
                                onChange={(e) => setPassingScore(parseInt(e.target.value) || 60)}
                                min={0}
                                max={100}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                disabled={Boolean(assessment)}
                            />
                        </div>
                    </div>

                    {!assessment && (
                        <div className="flex justify-end pt-4">
                            <Button onClick={handleSaveAssessment} isLoading={saving} className="gap-2">
                                <Save className="w-4 h-4" />
                                {t('assessments.form.saveAndContinue')}
                            </Button>
                        </div>
                    )}
                </CardContent>
            </Card>

            {assessment && (
                <Card>
                    <CardHeader>
                        <div className="flex items-center justify-between">
                            <CardTitle>{t('assessments.questionForm.title')} ({questions.length})</CardTitle>
                            <Button onClick={handleAddQuestion} className="gap-2">
                                <Plus className="w-4 h-4" />
                                {t('assessments.questionForm.addQuestion')}
                            </Button>
                        </div>
                    </CardHeader>
                    <CardContent>
                        {questions.length === 0 ? (
                            <div className="text-center py-8 text-zinc-500">
                                {t('assessments.questionForm.noQuestions')}
                            </div>
                        ) : (
                            <div className="space-y-4">
                                {questions.map((question, index) => (
                                    <QuestionItem
                                        key={question.id || index}
                                        question={question}
                                        index={index}
                                        isActive={activeQuestion === index}
                                        saving={saving}
                                        onToggle={() => setActiveQuestion(activeQuestion === index ? null : index)}
                                        onRemove={() => handleRemoveQuestion(index)}
                                        onChange={(field, value) => handleQuestionChange(index, field, value)}
                                        onOptionChange={(optIndex, value) => handleOptionChange(index, optIndex, value)}
                                        onSave={() => handleSaveQuestion(index)}
                                        t={t}
                                    />
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>
            )}
        </div>
    );
}

interface QuestionItemProps {
    question: QuestionForm;
    index: number;
    isActive: boolean;
    saving: boolean;
    onToggle: () => void;
    onRemove: () => void;
    onChange: (field: keyof QuestionForm, value: unknown) => void;
    onOptionChange: (optIndex: number, value: string) => void;
    onSave: () => void;
    t: (key: string) => string;
}

function QuestionItem({ question, index, isActive, saving, onToggle, onRemove, onChange, onOptionChange, onSave, t }: QuestionItemProps) {
    return (
        <div className={`border rounded-lg ${isActive ? 'border-blue-500' : 'border-zinc-200 dark:border-zinc-700'}`}>
            <div className="flex items-center justify-between p-4 cursor-pointer" onClick={onToggle}>
                <div className="flex items-center gap-3">
                    <GripVertical className="w-5 h-5 text-zinc-400" />
                    <span className="font-medium text-zinc-900 dark:text-white">
                        {index + 1}. {question.questionText || t('assessments.questionForm.newQuestion')}
                    </span>
                </div>
                <div className="flex items-center gap-2">
                    <Badge variant="default">
                        {question.questionType === 'MULTIPLE_CHOICE' ? t('assessments.questionForm.multipleChoice') :
                            question.questionType === 'TRUE_FALSE' ? t('assessments.questionForm.trueFalse') : t('assessments.questionForm.code')}
                    </Badge>
                    <Badge variant="info">{question.points} {t('assessments.point')}</Badge>
                    <button
                        onClick={(e) => { e.stopPropagation(); onRemove(); }}
                        className="p-1 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded"
                    >
                        <Trash2 className="w-4 h-4" />
                    </button>
                </div>
            </div>

            {isActive && (
                <div className="p-4 border-t border-zinc-200 dark:border-zinc-700 space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                            {t('assessments.questionForm.questionText')} *
                        </label>
                        <textarea
                            value={question.questionText}
                            onChange={(e) => onChange('questionText', e.target.value)}
                            rows={2}
                            className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                        />
                    </div>

                    <div className="grid gap-4 sm:grid-cols-3">
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('assessments.questionForm.questionType')}
                            </label>
                            <select
                                value={question.questionType}
                                onChange={(e) => onChange('questionType', e.target.value)}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            >
                                <option value="MULTIPLE_CHOICE">{t('assessments.questionForm.multipleChoice')}</option>
                                <option value="TRUE_FALSE">{t('assessments.questionForm.trueFalse')}</option>
                                <option value="CODE">{t('assessments.questionForm.code')}</option>
                                <option value="SHORT_ANSWER">{t('assessments.questionForm.shortAnswer')}</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('assessments.points')}
                            </label>
                            <input
                                type="number"
                                value={question.points}
                                onChange={(e) => onChange('points', parseInt(e.target.value) || 1)}
                                min={1}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                {t('assessments.questionForm.correctAnswer')} *
                            </label>
                            {question.questionType === 'TRUE_FALSE' ? (
                                <select
                                    value={question.correctAnswer}
                                    onChange={(e) => onChange('correctAnswer', e.target.value)}
                                    className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                >
                                    <option value="">{t('assessments.questionForm.select')}</option>
                                    <option value="Točno">{t('assessments.questionForm.true')}</option>
                                    <option value="Netočno">{t('assessments.questionForm.false')}</option>
                                </select>
                            ) : question.questionType === 'MULTIPLE_CHOICE' ? (
                                <select
                                    value={question.correctAnswer}
                                    onChange={(e) => onChange('correctAnswer', e.target.value)}
                                    className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                >
                                    <option value="">{t('assessments.questionForm.select')}</option>
                                    {question.options
                                        .filter(o => o.trim())
                                        .map((opt, i) => (
                                            <option key={i} value={opt}>{opt}</option>
                                        ))}
                                </select>
                            ) : (
                                <input
                                    type="text"
                                    value={question.correctAnswer}
                                    onChange={(e) => onChange('correctAnswer', e.target.value)}
                                    className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                />
                            )}
                        </div>
                    </div>

                    {question.questionType === 'MULTIPLE_CHOICE' && (
                        <div>
                            <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                                {t('assessments.questionForm.options')}
                            </label>
                            <div className="space-y-2">
                                {question.options.map((option, optIndex) => (
                                    <input
                                        key={optIndex}
                                        type="text"
                                        value={option}
                                        onChange={(e) => onOptionChange(optIndex, e.target.value)}
                                        placeholder={`${t('assessments.questionForm.optionPlaceholder')} ${optIndex + 1}`}
                                        className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                    />
                                ))}
                            </div>
                            <p className="text-xs text-zinc-500 mt-2">
                                Nakon unosa opcija, odaberite točnu iznad u polju "{t('assessments.questionForm.correctAnswer')}".
                            </p>
                        </div>
                    )}

                    {question.questionType === 'CODE' && (
                        <>
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    {t('assessments.questionForm.codeTemplate')}
                                </label>
                                <textarea
                                    value={question.codeTemplate}
                                    onChange={(e) => onChange('codeTemplate', e.target.value)}
                                    rows={4}
                                    className="w-full px-3 py-2 font-mono text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-1">
                                    Test primjeri (JSON)
                                </label>
                                <textarea
                                    value={question.testCases}
                                    onChange={(e) => onChange('testCases', e.target.value)}
                                    rows={4}
                                    placeholder='[{"input": "5 3", "expectedOutput": "8"}]'
                                    className="w-full px-3 py-2 font-mono text-sm border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                                />
                                <p className="text-xs text-zinc-500 mt-1">
                                    Format: JSON lista objekata s poljima "input" i "expectedOutput". Bez test primjera pitanje se ne može automatski ocijeniti.
                                </p>
                            </div>
                        </>
                    )}

                    <div className="flex justify-end pt-2">
                        <Button onClick={onSave} isLoading={saving}>
                            {t('assessments.questionForm.saveQuestion')}
                        </Button>
                    </div>
                </div>
            )}
        </div>
    );
}