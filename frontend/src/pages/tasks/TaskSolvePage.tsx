import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { 
    ArrowLeft, Play, Send, Clock, CheckCircle, XCircle, Loader2, Code,
    AlertTriangle, Terminal, MessageSquare
} from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../../components';
import { taskService, submissionService } from '../../services';
import type { Task, Submission, SubmissionStatus } from '../../types';

// Monaco Editor - lazy load
import Editor from '@monaco-editor/react';

export const TaskSolvePage = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { t } = useTranslation();

    const [task, setTask] = useState<Task | null>(null);
    const [code, setCode] = useState('');
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [submission, setSubmission] = useState<Submission | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (id) fetchTask();
    }, [id]);

    const fetchTask = async () => {
        try {
            setLoading(true);
            const data = await taskService.getById(id!);
            setTask(data);
            setCode(data.starterCode || getDefaultCode(data.languageType));
            setError(null);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load task');
        } finally {
            setLoading(false);
        }
    };

    const getDefaultCode = (lang: string): string => {
        if (lang === 'CSHARP') {
            return `using System;

class Program
{
    static void Main(string[] args)
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
    };

    const handleSubmit = async () => {
        if (!code.trim()) {
            alert('Unesite kod prije predaje!');
            return;
        }

        try {
            setSubmitting(true);
            setSubmission(null);
            const result = await submissionService.submit(id!, code);
            setSubmission(result);
            
            // Polling za status ako je PENDING/COMPILING/RUNNING
            if (['PENDING', 'COMPILING', 'RUNNING'].includes(result.status)) {
                pollSubmissionStatus(result.id);
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to submit');
        } finally {
            setSubmitting(false);
        }
    };

    const pollSubmissionStatus = async (submissionId: string) => {
        const maxAttempts = 30;
        let attempts = 0;

        const poll = async () => {
            try {
                const updated = await submissionService.getById(submissionId);
                setSubmission(updated);

                if (['PENDING', 'COMPILING', 'RUNNING'].includes(updated.status) && attempts < maxAttempts) {
                    attempts++;
                    setTimeout(poll, 2000);
                }
            } catch (err) {
                console.error('Polling error:', err);
            }
        };

        setTimeout(poll, 2000);
    };

    const getStatusIcon = (status: SubmissionStatus) => {
        switch (status) {
            case 'COMPLETED':
                return <CheckCircle className="w-5 h-5 text-green-500" />;
            case 'COMPILE_ERROR':
            case 'RUNTIME_ERROR':
            case 'FAILED':
                return <XCircle className="w-5 h-5 text-red-500" />;
            case 'TIMEOUT':
                return <AlertTriangle className="w-5 h-5 text-yellow-500" />;
            default:
                return <Loader2 className="w-5 h-5 text-blue-500 animate-spin" />;
        }
    };

    const getStatusText = (status: SubmissionStatus): string => {
        const statusMap: Record<SubmissionStatus, string> = {
            PENDING: 'Čeka na obradu...',
            COMPILING: 'Kompilacija...',
            RUNNING: 'Izvršavanje...',
            COMPLETED: 'Uspješno izvršeno',
            COMPILE_ERROR: 'Greška pri kompilaciji',
            RUNTIME_ERROR: 'Greška pri izvršavanju',
            TIMEOUT: 'Prekoračeno vrijeme',
            FAILED: 'Neuspješno'
        };
        return statusMap[status] || status;
    };

    const getMonacoLanguage = (lang: string): string => {
        return lang === 'CSHARP' ? 'csharp' : 'c';
    };

    if (loading) return <PageSkeleton />;

    if (error || !task) {
        return (
            <div className="p-6 text-center">
                <p className="text-red-500">{error || 'Task not found'}</p>
                <button onClick={() => navigate('/tasks')} className="mt-4 px-4 py-2 bg-violet-600 text-white rounded-lg">
                    Natrag na zadatke
                </button>
            </div>
        );
    }

    return (
        <div className="h-[calc(100vh-4rem)] flex flex-col">
            {/* Header */}
            <div className="flex items-center gap-4 p-4 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800">
                <button
                    onClick={() => navigate('/tasks')}
                    className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
                >
                    <ArrowLeft className="w-5 h-5" />
                </button>
                <div className="flex-1">
                    <div className="flex items-center gap-2">
                        <h1 className="text-lg font-bold text-gray-900 dark:text-white">
                            {task.title}
                        </h1>
                        <span className={`text-xs px-2 py-0.5 rounded ${
                            task.languageType === 'C'
                                ? 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300'
                                : 'bg-purple-100 text-purple-700 dark:bg-purple-900 dark:text-purple-300'
                        }`}>
                            {task.languageType === 'CSHARP' ? 'C#' : task.languageType}
                        </span>
                    </div>
                    <p className="text-sm text-gray-500">{task.courseName} • {task.outcomeName}</p>
                </div>
                <div className="flex items-center gap-2 text-sm text-gray-500">
                    <Clock className="w-4 h-4" />
                    {task.timeLimitSeconds}s
                </div>
                <button
                    onClick={handleSubmit}
                    disabled={submitting}
                    className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
                >
                    {submitting ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                        <Send className="w-4 h-4" />
                    )}
                    Predaj
                </button>
            </div>

            {/* Main content */}
            <div className="flex-1 flex overflow-hidden">
                {/* Left panel - Instructions */}
                <div className="w-1/3 border-r border-gray-200 dark:border-gray-700 overflow-y-auto p-4 bg-gray-50 dark:bg-gray-900">
                    <div className="space-y-4">
                        <div>
                            <h2 className="font-semibold text-gray-900 dark:text-white mb-2">
                                Upute
                            </h2>
                            <div className="prose prose-sm dark:prose-invert max-w-none">
                                {task.instructions}
                            </div>
                        </div>

                        {task.description && (
                            <div>
                                <h2 className="font-semibold text-gray-900 dark:text-white mb-2">
                                    Opis
                                </h2>
                                <p className="text-sm text-gray-600 dark:text-gray-400">
                                    {task.description}
                                </p>
                            </div>
                        )}

                        {task.gradingCriteria && (
                            <div>
                                <h2 className="font-semibold text-gray-900 dark:text-white mb-2">
                                    Kriteriji ocjenjivanja
                                </h2>
                                <p className="text-sm text-gray-600 dark:text-gray-400">
                                    {task.gradingCriteria}
                                </p>
                            </div>
                        )}
                    </div>
                </div>

                {/* Right panel - Editor & Results */}
                <div className="flex-1 flex flex-col">
                    {/* Code Editor */}
                    <div className="flex-1 relative">
                        <Editor
                            height="100%"
                            language={getMonacoLanguage(task.languageType)}
                            value={code}
                            onChange={(value) => setCode(value || '')}
                            theme="vs-dark"
                            options={{
                                minimap: { enabled: false },
                                fontSize: 14,
                                lineNumbers: 'on',
                                scrollBeyondLastLine: false,
                                automaticLayout: true,
                                tabSize: 4,
                                insertSpaces: true,
                            }}
                        />
                    </div>

                    {/* Results Panel */}
                    {submission && (
                        <div className="h-64 border-t border-gray-200 dark:border-gray-700 overflow-y-auto bg-gray-900">
                            <div className="p-4">
                                {/* Status */}
                                <div className="flex items-center gap-2 mb-4">
                                    {getStatusIcon(submission.status)}
                                    <span className="font-medium text-white">
                                        {getStatusText(submission.status)}
                                    </span>
                                    {submission.status === 'COMPLETED' && (
                                        <span className="ml-auto text-sm text-gray-400">
                                            {submission.testCasesPassed}/{submission.testCasesTotal} testova
                                            {submission.score !== null && ` • ${submission.score} bodova`}
                                        </span>
                                    )}
                                </div>

                                {/* Compiler Output */}
                                {submission.compilerOutput && (
                                    <div className="mb-4">
                                        <div className="flex items-center gap-2 text-sm text-gray-400 mb-2">
                                            <Terminal className="w-4 h-4" />
                                            Compiler Output
                                        </div>
                                        <pre className="text-sm text-red-400 bg-gray-800 p-3 rounded overflow-x-auto">
                                            {submission.compilerOutput}
                                        </pre>
                                    </div>
                                )}

                                {/* Execution Output */}
                                {submission.executionOutput && (
                                    <div className="mb-4">
                                        <div className="flex items-center gap-2 text-sm text-gray-400 mb-2">
                                            <Code className="w-4 h-4" />
                                            Output
                                        </div>
                                        <pre className="text-sm text-green-400 bg-gray-800 p-3 rounded overflow-x-auto">
                                            {submission.executionOutput}
                                        </pre>
                                    </div>
                                )}

                                {/* AI Feedback */}
                                {submission.aiFeedback && (
                                    <div>
                                        <div className="flex items-center gap-2 text-sm text-gray-400 mb-2">
                                            <MessageSquare className="w-4 h-4" />
                                            AI Feedback
                                        </div>
                                        <div className="text-sm text-gray-300 bg-gray-800 p-3 rounded">
                                            {submission.aiFeedback}
                                        </div>
                                    </div>
                                )}

                                {/* Execution Stats */}
                                {submission.executionTimeMs !== null && (
                                    <div className="mt-4 flex gap-4 text-sm text-gray-500">
                                        <span>Vrijeme: {submission.executionTimeMs}ms</span>
                                        {submission.memoryUsedKb !== null && (
                                            <span>Memorija: {(submission.memoryUsedKb / 1024).toFixed(2)}MB</span>
                                        )}
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

const PageSkeleton = () => (
    <div className="h-screen flex flex-col animate-pulse">
        <div className="h-16 bg-gray-200 dark:bg-gray-700"></div>
        <div className="flex-1 flex">
            <div className="w-1/3 bg-gray-100 dark:bg-gray-800"></div>
            <div className="flex-1 bg-gray-200 dark:bg-gray-700"></div>
        </div>
    </div>
);

export default TaskSolvePage;
