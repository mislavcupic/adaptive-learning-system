import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Editor from '@monaco-editor/react';
import { ArrowLeft, Clock, CheckCircle, XCircle, Send } from 'lucide-react';
import { useTheme } from '../context';
import { assessmentService } from '../services';
import {
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    Button,
    Badge,
    LoadingScreen,
    ErrorState
} from '../components/ui';
import type { Assessment, Question, AssessmentAttempt } from '../types';

export function AssessmentSolvePage() {
    const { t } = useTranslation();
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { theme } = useTheme();

    const [assessment, setAssessment] = useState<Assessment | null>(null);
    const [answers, setAnswers] = useState<Record<string, string>>({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const [result, setResult] = useState<AssessmentAttempt | null>(null);
    const [timeLeft, setTimeLeft] = useState<number | null>(null);

    useEffect(() => {
        if (!id) return;

        const loadAssessment = async () => {
            try {
                const data = await assessmentService.getById(id);
                setAssessment(data);

                await assessmentService.startAttempt(id);

                if (data.timeLimitMinutes) {
                    setTimeLeft(data.timeLimitMinutes * 60);
                }
            } catch (err) {
                setError(err instanceof Error ? err.message : t('errors.generic'));
            } finally {
                setLoading(false);
            }
        };

        loadAssessment();
    }, [id, t]);

    useEffect(() => {
        if (timeLeft === null || timeLeft <= 0 || result) return;

        const timer = setInterval(() => {
            setTimeLeft(prev => {
                if (prev === null || prev <= 1) {
                    clearInterval(timer);
                    handleSubmit();
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);

        return () => clearInterval(timer);
    }, [timeLeft, result]);

    const handleAnswerChange = (questionId: string, value: string) => {
        setAnswers(prev => ({ ...prev, [questionId]: value }));
    };

    const handleSubmit = async () => {
        if (!id || submitting) return;

        setSubmitting(true);
        try {
            const attemptResult = await assessmentService.submitAttempt({
                assessmentId: id,
                answers
            });
            setResult(attemptResult);
        } catch (err) {
            setError(err instanceof Error ? err.message : t('errors.generic'));
        } finally {
            setSubmitting(false);
        }
    };

    const formatTime = (seconds: number): string => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    const parseOptions = (optionsJson?: string): string[] => {
        if (!optionsJson) return [];
        try {
            return JSON.parse(optionsJson);
        } catch {
            return [];
        }
    };

    if (loading) return <LoadingScreen />;
    if (error && !assessment) return <ErrorState description={error} onRetry={() => navigate(-1)} />;
    if (!assessment) return null;

    if (result) {
        return (
            <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">
                <Card>
                    <CardHeader>
                        <CardTitle className="text-center">
                            {result.passed ? `🎉 ${t('assessments.congratulations')}` : t('assessments.testCompleted')}
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        <div className="text-center">
                            <div className={`inline-flex items-center justify-center w-24 h-24 rounded-full ${
                                result.passed
                                    ? 'bg-green-100 dark:bg-green-900/30'
                                    : 'bg-red-100 dark:bg-red-900/30'
                            }`}>
                                {result.passed ? (
                                    <CheckCircle className="w-12 h-12 text-green-600 dark:text-green-400" />
                                ) : (
                                    <XCircle className="w-12 h-12 text-red-600 dark:text-red-400" />
                                )}
                            </div>
                        </div>

                        <div className="grid grid-cols-3 gap-4 text-center">
                            <div>
                                <p className="text-3xl font-bold text-zinc-900 dark:text-white">
                                    {result.score}
                                </p>
                                <p className="text-sm text-zinc-500">{t('assessments.score')}</p>
                            </div>
                            <div>
                                <p className="text-3xl font-bold text-zinc-900 dark:text-white">
                                    {result.maxScore}
                                </p>
                                <p className="text-sm text-zinc-500">{t('assessments.maxScore')}</p>
                            </div>
                            <div>
                                <p className={`text-3xl font-bold ${
                                    result.passed ? 'text-green-600' : 'text-red-600'
                                }`}>
                                    {result.percentage.toFixed(0)}%
                                </p>
                                <p className="text-sm text-zinc-500">{t('assessments.percentage')}</p>
                            </div>
                        </div>

                        <div className="flex justify-center gap-4 pt-4">
                            <Button variant="secondary" onClick={() => navigate('/dashboard')}>
                                {t('assessments.backToDashboard')}
                            </Button>
                            <Button onClick={() => navigate('/tasks')}>
                                {t('assessments.continueWithTasks')}
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="space-y-6 animate-fade-in">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate(-1)}
                        className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800"
                    >
                        <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                    </button>
                    <div>
                        <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {assessment.title}
                        </h1>
                        <div className="flex items-center gap-2 mt-1">
                            <Badge variant={assessment.assessmentType === 'PRETEST' ? 'warning' : 'success'}>
                                {assessment.assessmentType === 'PRETEST'
                                    ? t('assessments.pretest')
                                    : t('assessments.posttest')}
                            </Badge>
                            <span className="text-sm text-zinc-500">
                                {assessment.questionCount} {t('assessments.questions')}
                            </span>
                        </div>
                    </div>
                </div>

                {timeLeft !== null && (
                    <div className={`flex items-center gap-2 px-4 py-2 rounded-lg ${
                        timeLeft < 60
                            ? 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300'
                            : 'bg-zinc-100 dark:bg-zinc-800 text-zinc-700 dark:text-zinc-300'
                    }`}>
                        <Clock className="w-5 h-5" />
                        <span className="font-mono text-lg font-semibold">
                            {formatTime(timeLeft)}
                        </span>
                    </div>
                )}
            </div>

            {assessment.description && (
                <Card>
                    <CardContent className="py-4">
                        <p className="text-zinc-600 dark:text-zinc-400">{assessment.description}</p>
                    </CardContent>
                </Card>
            )}

            <div className="space-y-6">
                {assessment.questions?.map((question, index) => (
                    <QuestionCard
                        key={question.id}
                        question={question}
                        index={index}
                        answer={answers[question.id] || ''}
                        onAnswerChange={(value) => handleAnswerChange(question.id, value)}
                        theme={theme}
                        parseOptions={parseOptions}
                        t={t}
                    />
                ))}
            </div>

            <div className="flex justify-end">
                <Button
                    onClick={handleSubmit}
                    disabled={submitting}
                    isLoading={submitting}
                    className="gap-2"
                    size="lg"
                >
                    <Send className="w-5 h-5" />
                    {t('assessments.submitTest')}
                </Button>
            </div>
        </div>
    );
}

interface QuestionCardProps {
    question: Question;
    index: number;
    answer: string;
    onAnswerChange: (value: string) => void;
    theme: string;
    parseOptions: (options?: string) => string[];
    t: (key: string) => string;
}

function QuestionCard({ question, index, answer, onAnswerChange, theme, parseOptions, t }: QuestionCardProps) {
    const options = parseOptions(question.options);

    return (
        <Card>
            <CardHeader>
                <div className="flex items-start justify-between">
                    <CardTitle className="text-base">
                        <span className="text-zinc-400 mr-2">{index + 1}.</span>
                        {question.questionText}
                    </CardTitle>
                    <Badge variant="default">{question.points} {t('assessments.point')}</Badge>
                </div>
            </CardHeader>
            <CardContent>
                {question.questionType === 'MULTIPLE_CHOICE' && (
                    <div className="space-y-2">
                        {options.map((option, optIndex) => (
                            <label
                                key={optIndex}
                                className={`flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors ${
                                    answer === option
                                        ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                                        : 'border-zinc-200 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-800'
                                }`}
                            >
                                <input
                                    type="radio"
                                    name={`question-${question.id}`}
                                    value={option}
                                    checked={answer === option}
                                    onChange={(e) => onAnswerChange(e.target.value)}
                                    className="w-4 h-4 text-blue-600"
                                />
                                <span className="text-zinc-700 dark:text-zinc-300">{option}</span>
                            </label>
                        ))}
                    </div>
                )}

                {question.questionType === 'TRUE_FALSE' && (
                    <div className="flex gap-4">
                        {[t('assessments.questionForm.true'), t('assessments.questionForm.false')].map((option) => (
                            <label
                                key={option}
                                className={`flex-1 flex items-center justify-center gap-2 p-4 rounded-lg border cursor-pointer transition-colors ${
                                    answer === option
                                        ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                                        : 'border-zinc-200 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-800'
                                }`}
                            >
                                <input
                                    type="radio"
                                    name={`question-${question.id}`}
                                    value={option}
                                    checked={answer === option}
                                    onChange={(e) => onAnswerChange(e.target.value)}
                                    className="w-4 h-4 text-blue-600"
                                />
                                <span className="font-medium text-zinc-700 dark:text-zinc-300">
                                    {option}
                                </span>
                            </label>
                        ))}
                    </div>
                )}

                {question.questionType === 'CODE' && (
                    <div className="h-[300px] border border-zinc-200 dark:border-zinc-700 rounded-lg overflow-hidden">
                        <Editor
                            height="100%"
                            language="c"
                            value={answer || question.codeTemplate || ''}
                            onChange={(value) => onAnswerChange(value || '')}
                            theme={theme === 'dark' ? 'vs-dark' : 'light'}
                            options={{
                                minimap: { enabled: false },
                                fontSize: 14,
                                lineNumbers: 'on',
                                scrollBeyondLastLine: false,
                                automaticLayout: true,
                            }}
                        />
                    </div>
                )},
                {question.questionType === 'SHORT_ANSWER' && (
                    <input
                        type="text"
                        value={answer}
                        onChange={(e) => onAnswerChange(e.target.value)}
                        placeholder={t('assessments.questionForm.shortAnswerPlaceholder')}
                        className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-700 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                    />
                )}
            </CardContent>
        </Card>
    );
}