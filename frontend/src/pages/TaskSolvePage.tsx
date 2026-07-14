import { useState, useRef, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Editor from '@monaco-editor/react';
import { Play, Clock, Award, ArrowLeft, Loader2, AlertTriangle } from 'lucide-react';
import { useTheme } from '../context';
import { taskService, submissionService } from '../services';
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
import { formatLanguageType, formatSubmissionStatus } from '../utils';
import type { Task, Submission } from '../types';

export function TaskSolvePage() {
    const { t } = useTranslation();
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { theme } = useTheme();

    const [task, setTask] = useState<Task | null>(null);
    const [code, setCode] = useState('');
    const [textAnswer, setTextAnswer] = useState('');
    const [selectedOption, setSelectedOption] = useState('');
    const [checkedOptions, setCheckedOptions] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const [submission, setSubmission] = useState<Submission | null>(null);

    useEffect(() => {
        if (!id) return;

        const loadTask = async () => {
            try {
                const data = await taskService.getById(id);
                setTask(data);
                if (data.taskType === 'CODE' || !data.taskType) {
                    setCode(data.starterCode || getDefaultCode(data.languageType));
                }
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to load task');
            } finally {
                setLoading(false);
            }
        };

        loadTask();
    }, [id]);

    const submittingRef = useRef(false);

    const getAnswer = (): string => {
        if (!task) return '';

        switch (task.taskType) {
            case 'TEXT':
                return textAnswer;
            case 'MULTIPLE_CHOICE':
                return selectedOption;
            case 'CHECKLIST':
                return JSON.stringify(checkedOptions);
            case 'CODE':
            default:
                return code;
        }
    };

    const isAnswerValid = (): boolean => {
        if (!task) return false;

        switch (task.taskType) {
            case 'TEXT':
                return textAnswer.trim().length > 0;
            case 'MULTIPLE_CHOICE':
                return selectedOption.length > 0;
            case 'CHECKLIST':
                return checkedOptions.length > 0;
            case 'CODE':
            default:
                return code.trim().length > 0;
        }
    };

    const handleSubmit = async () => {
        if (!id || !isAnswerValid() || submittingRef.current) return;
        submittingRef.current = true;
        setSubmitting(true);
        setSubmission(null);
        setError(null);
        try {
            const result = await submissionService.submit({ taskId: id, code: getAnswer() });
            setSubmission(result);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Submission failed');
        } finally {
            setSubmitting(false);
            submittingRef.current = false;
        }
    };

    const handleCheckboxChange = (option: string) => {
        setCheckedOptions(prev =>
            prev.includes(option)
                ? prev.filter(o => o !== option)
                : [...prev, option]
        );
    };

    const parseOptions = (optionsJson?: string | null): string[] => {
        if (!optionsJson) return [];
        try {
            return JSON.parse(optionsJson);
        } catch {
            return [];
        }
    };

    if (loading) return <LoadingScreen />;
    if (error && !task) return <ErrorState description={error} onRetry={() => navigate(-1)} />;
    if (!task) return null;

    const taskType = task.taskType || 'CODE';
    const options = parseOptions(task.options);
    const language = task.languageType === 'CSHARP' ? 'csharp'
        : task.languageType === 'PYTHON' ? 'python'
            : 'c';

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center gap-4">
                <button
                    onClick={() => navigate(-1)}
                    className="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800"
                >
                    <ArrowLeft className="w-5 h-5 text-zinc-600 dark:text-zinc-400" />
                </button>
                <div className="flex-1">
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {task.title}
                    </h1>
                    <div className="flex items-center gap-4 mt-1 text-sm text-zinc-500 dark:text-zinc-400">
                        <span className="flex items-center gap-1">
                            <Award className="w-4 h-4" />
                            {task.maxScore} {t('assessments.points')}
                        </span>
                        {taskType === 'CODE' && (
                            <>
                                <span className="flex items-center gap-1">
                                    <Clock className="w-4 h-4" />
                                    {task.timeLimitSeconds}s
                                </span>
                                <Badge>{formatLanguageType(task.languageType || 'C')}</Badge>
                            </>
                        )}
                        <Badge variant="info">
                            {taskType === 'CODE' ? t('tasks.form.typeCode') :
                                taskType === 'TEXT' ? t('tasks.form.typeText') :
                                    taskType === 'MULTIPLE_CHOICE' ? t('tasks.form.typeMultipleChoice') :
                                        t('tasks.form.typeChecklist')}
                        </Badge>
                    </div>
                </div>
                <Button
                    onClick={handleSubmit}
                    disabled={submitting || !isAnswerValid()}
                    isLoading={submitting}
                    className="gap-2"
                >
                    <Play className="w-4 h-4" />
                    {t('tasks.submitCode')}
                </Button>
            </div>

            {/* Submit error */}
            {error && task && (
                <Card>
                    <CardContent className="py-4">
                        <div className="flex items-center gap-3 text-red-600 dark:text-red-400">
                            <AlertTriangle className="w-5 h-5 flex-shrink-0" />
                            <p className="text-sm font-medium">{error}</p>
                        </div>
                    </CardContent>
                </Card>
            )}

            <div className="grid lg:grid-cols-2 gap-6">
                {/* Instructions */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('tasks.instructions')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="prose prose-zinc dark:prose-invert max-w-none">
                            <p className="whitespace-pre-wrap">{task.instructions}</p>
                        </div>
                        {task.description && (
                            <div className="mt-4 pt-4 border-t border-zinc-100 dark:border-zinc-800">
                                <p className="text-sm text-zinc-500 dark:text-zinc-400">
                                    {task.description}
                                </p>
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* Answer Area - depends on task type */}
                <Card className="overflow-hidden">
                    <CardHeader>
                        <CardTitle>
                            {taskType === 'CODE' ? t('tasks.code') : t('assessments.questionForm.correctAnswer')}
                        </CardTitle>
                    </CardHeader>

                    {/* CODE type */}
                    {taskType === 'CODE' && (
                        <div className="h-[400px] border-t border-zinc-100 dark:border-zinc-800">
                            <Editor
                                height="100%"
                                language={language}
                                value={code}
                                onChange={(value) => setCode(value || '')}
                                theme={theme === 'dark' ? 'vs-dark' : 'light'}
                                options={{
                                    minimap: { enabled: false },
                                    fontSize: 14,
                                    lineNumbers: 'on',
                                    scrollBeyondLastLine: false,
                                    automaticLayout: true,
                                    tabSize: 4,
                                    wordWrap: 'on',
                                }}
                            />
                        </div>
                    )}

                    {/* TEXT type */}
                    {taskType === 'TEXT' && (
                        <CardContent>
                            <textarea
                                value={textAnswer}
                                onChange={(e) => setTextAnswer(e.target.value)}
                                rows={12}
                                placeholder={t('tasks.form.expectedAnswerHint')}
                                className="w-full px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white resize-none"
                            />
                        </CardContent>
                    )}

                    {/* MULTIPLE_CHOICE type */}
                    {taskType === 'MULTIPLE_CHOICE' && (
                        <CardContent className="space-y-3">
                            {options.map((option, index) => (
                                <label
                                    key={index}
                                    className={`flex items-center gap-3 p-4 rounded-lg border cursor-pointer transition-colors ${
                                        selectedOption === option
                                            ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                                            : 'border-zinc-200 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-800'
                                    }`}
                                >
                                    <input
                                        type="radio"
                                        name="task-option"
                                        value={option}
                                        checked={selectedOption === option}
                                        onChange={(e) => setSelectedOption(e.target.value)}
                                        className="w-4 h-4 text-blue-600"
                                    />
                                    <span className="text-zinc-700 dark:text-zinc-300">{option}</span>
                                </label>
                            ))}
                        </CardContent>
                    )}

                    {/* CHECKLIST type */}
                    {taskType === 'CHECKLIST' && (
                        <CardContent className="space-y-3">
                            <p className="text-sm text-zinc-500 mb-4">
                                {t('tasks.form.checklistHint')}
                            </p>
                            {options.map((option, index) => (
                                <label
                                    key={index}
                                    className={`flex items-center gap-3 p-4 rounded-lg border cursor-pointer transition-colors ${
                                        checkedOptions.includes(option)
                                            ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                                            : 'border-zinc-200 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-800'
                                    }`}
                                >
                                    <input
                                        type="checkbox"
                                        checked={checkedOptions.includes(option)}
                                        onChange={() => handleCheckboxChange(option)}
                                        className="w-4 h-4 text-blue-600 rounded"
                                    />
                                    <span className="text-zinc-700 dark:text-zinc-300">{option}</span>
                                </label>
                            ))}
                        </CardContent>
                    )}
                </Card>
            </div>

            {/* Submission Result */}
            {submitting && (
                <Card>
                    <CardContent className="py-8">
                        <div className="flex items-center justify-center gap-3">
                            <Loader2 className="w-6 h-6 animate-spin text-zinc-400" />
                            <span className="text-zinc-600 dark:text-zinc-400">
                                {taskType === 'CODE' ? t('common.loading') : t('common.submit')}...
                            </span>
                        </div>
                    </CardContent>
                </Card>
            )}

            {submission && (
                <Card>
                    <CardHeader>
                        <div className="flex items-center justify-between">
                            <CardTitle>{t('submissions.submissionDetails')}</CardTitle>
                            <Badge
                                variant={
                                    submission.status === 'COMPLETED' ? 'success' :
                                        submission.status.includes('ERROR') ? 'danger' : 'warning'
                                }
                            >
                                {formatSubmissionStatus(submission.status).label}
                            </Badge>
                        </div>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        {/* Test Results - only for CODE */}
                        {taskType === 'CODE' && (
                            <div className="flex items-center gap-4">
                                <div className="text-center">
                                    <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                        {submission.testsPassed ?? 0}/{submission.testsTotal ?? 0}
                                    </p>
                                    <p className="text-sm text-zinc-500">{t('submissions.testsPassed')}</p>
                                </div>
                                {(submission.finalScore ?? submission.aiScore) != null && (
                                    <div className="text-center">
                                        <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                            {submission.finalScore ?? submission.aiScore ?? 0}/{task.maxScore}
                                        </p>
                                        <p className="text-sm text-zinc-500">{t('assessments.points')}</p>
                                    </div>
                                )}
                                {submission.executionTimeMs && (
                                    <div className="text-center">
                                        <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                            {submission.executionTimeMs}ms
                                        </p>
                                        <p className="text-sm text-zinc-500">{t('submissions.executionTime')}</p>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Score for non-CODE types */}
                        {taskType !== 'CODE' && (submission.finalScore ?? submission.aiScore ?? submission.teacherScore) != null && (
                            <div className="flex items-center gap-4">
                                <div className="text-center">
                                    <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                        {submission.finalScore ?? submission.teacherScore ?? submission.aiScore ?? 0}/{task.maxScore}
                                    </p>
                                    <p className="text-sm text-zinc-500">{t('assessments.points')}</p>
                                </div>
                            </div>
                        )}

                        {/* Pending review notice for non-CODE types */}
                        {taskType !== 'CODE' && submission.teacherScore == null && (
                            <div className="p-4 bg-amber-50 dark:bg-amber-900/20 rounded-lg border border-amber-200 dark:border-amber-800">
                                <p className="text-sm text-amber-700 dark:text-amber-300">
                                    Vaš odgovor je zaprimljen. Nastavnik će ga ručno pregledati i ocijeniti.
                                </p>
                            </div>
                        )}

                        {/* Compiler Output */}
                        {submission.compilerOutput && (
                            <div>
                                <p className="text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                                    {t('submissions.compilerOutput')}
                                </p>
                                <pre className="p-3 bg-zinc-100 dark:bg-zinc-800 rounded-lg text-sm overflow-x-auto">
                                    {submission.compilerOutput}
                                </pre>
                            </div>
                        )}

                        {/* Execution Output */}
                        {submission.executionOutput && (
                            <div>
                                <p className="text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                                    {t('submissions.output')}
                                </p>
                                <pre className="p-3 bg-zinc-100 dark:bg-zinc-800 rounded-lg text-sm overflow-x-auto">
                                    {submission.executionOutput}
                                </pre>
                            </div>
                        )}

                        {/* AI Feedback */}
                        {submission.aiFeedback && (
                            <div className="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-100 dark:border-blue-800">
                                <p className="text-sm font-medium text-blue-700 dark:text-blue-300 mb-2">
                                    {t('submissions.aiFeedback')}
                                </p>
                                <p className="text-sm text-blue-600 dark:text-blue-400 whitespace-pre-wrap">
                                    {submission.aiFeedback}
                                </p>
                            </div>
                        )}

                        {/* Teacher Feedback */}
                        {submission.teacherFeedback && (
                            <div className="p-4 bg-emerald-50 dark:bg-emerald-900/20 rounded-lg border border-emerald-100 dark:border-emerald-800">
                                <p className="text-sm font-medium text-emerald-700 dark:text-emerald-300 mb-2">
                                    {t('submissions.teacherFeedback')}
                                </p>
                                <p className="text-sm text-emerald-600 dark:text-emerald-400 whitespace-pre-wrap">
                                    {submission.teacherFeedback}
                                </p>
                            </div>
                        )}
                    </CardContent>
                </Card>
            )}
        </div>
    );
}

function getDefaultCode(languageType?: string): string {
    if (languageType === 'CSHARP') {
        return `using System;

class Program
{
    static void Main()
    {
        // Vaš kod ovdje
        Console.WriteLine("Hello, World!");
    }
}`;
    }

    if (languageType === 'PYTHON') {
        return `# Vaš kod ovdje
print("Hello, World!")`;
    }

    return `#include <stdio.h>

int main() {
    // Vaš kod ovdje
    printf("Hello, World!\\n");
    return 0;
}`;
}