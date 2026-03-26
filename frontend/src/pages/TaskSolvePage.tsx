import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Editor from '@monaco-editor/react';
import { Play, Clock, Award, ArrowLeft, Loader2 } from 'lucide-react';
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
                setCode(data.starterCode || getDefaultCode(data.languageType));
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to load task');
            } finally {
                setLoading(false);
            }
        };

        loadTask();
    }, [id]);

    const handleSubmit = async () => {
        if (!id || !code.trim()) return;
        
        setSubmitting(true);
        setSubmission(null);
        
        try {
            const result = await submissionService.submit({
                taskId: id,
                code: code,
            });
            setSubmission(result);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Submission failed');
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) return <LoadingScreen />;
    if (error && !task) return <ErrorState description={error} onRetry={() => navigate(-1)} />;
    if (!task) return null;

    const language = task.languageType === 'CSHARP' ? 'csharp' : 'c';

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
                            {task.maxScore} bodova
                        </span>
                        <span className="flex items-center gap-1">
                            <Clock className="w-4 h-4" />
                            {task.timeLimitSeconds}s
                        </span>
                        <Badge>{formatLanguageType(task.languageType || 'C')}</Badge>
                    </div>
                </div>
                <Button
                    onClick={handleSubmit}
                    disabled={submitting || !code.trim()}
                    isLoading={submitting}
                    className="gap-2"
                >
                    <Play className="w-4 h-4" />
                    {t('tasks.submitCode')}
                </Button>
            </div>

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

                {/* Code Editor */}
                <Card className="overflow-hidden">
                    <CardHeader>
                        <CardTitle>Kod</CardTitle>
                    </CardHeader>
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
                </Card>
            </div>

            {/* Submission Result */}
            {submitting && (
                <Card>
                    <CardContent className="py-8">
                        <div className="flex items-center justify-center gap-3">
                            <Loader2 className="w-6 h-6 animate-spin text-zinc-400" />
                            <span className="text-zinc-600 dark:text-zinc-400">
                                Izvršavanje koda...
                            </span>
                        </div>
                    </CardContent>
                </Card>
            )}

            {submission && (
                <Card>
                    <CardHeader>
                        <div className="flex items-center justify-between">
                            <CardTitle>Rezultat</CardTitle>
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
                        {/* Test Results */}
                        <div className="flex items-center gap-4">
                            <div className="text-center">
                                <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                    {submission.testCasesPassed}/{submission.testCasesTotal}
                                </p>
                                <p className="text-sm text-zinc-500">testova prošlo</p>
                            </div>
                            {submission.score !== null && (
                                <div className="text-center">
                                    <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                        {submission.score}/{task.maxScore}
                                    </p>
                                    <p className="text-sm text-zinc-500">bodova</p>
                                </div>
                            )}
                            {submission.executionTimeMs && (
                                <div className="text-center">
                                    <p className="text-2xl font-semibold text-zinc-900 dark:text-white">
                                        {submission.executionTimeMs}ms
                                    </p>
                                    <p className="text-sm text-zinc-500">vrijeme</p>
                                </div>
                            )}
                        </div>

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
    
    return `#include <stdio.h>

int main() {
    // Vaš kod ovdje
    printf("Hello, World!\\n");
    return 0;
}`;
}
