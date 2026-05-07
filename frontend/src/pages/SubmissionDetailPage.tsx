import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Editor from '@monaco-editor/react';
import {
    ArrowLeft,
    User,
    FileCode,
    Clock,
    CheckCircle,
    AlertTriangle,
    MessageSquare,
    Bot,
    Send
} from 'lucide-react';
import { useAuth } from '../context';
import { useFetch } from '../hooks';
import { submissionService } from '../services';
import {
    Card,
    Button,
    Badge,
    LoadingScreen,
    ErrorState
} from '../components/ui';
import { formatRelativeTime, formatSubmissionStatus } from '../utils';
import type { Submission } from '../types';

export function SubmissionDetailPage() {
    const { t } = useTranslation();
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { user } = useAuth();

    const [teacherFeedback, setTeacherFeedback] = useState('');
    const [teacherScore, setTeacherScore] = useState<number | ''>('');
    const [submitting, setSubmitting] = useState(false);

    const { data: submission, loading, error, refetch } = useFetch<Submission>(
        () => submissionService.getById(id!),
        [id]
    );

    const isTeacherOrAdmin = user?.role === 'TEACHER' || user?.role === 'ADMIN';

    const handleAddFeedback = async () => {
        if (!teacherFeedback.trim() || !id) return;

        setSubmitting(true);
        try {
            await submissionService.addFeedback(id, teacherFeedback, teacherScore || undefined);
            setTeacherFeedback('');
            setTeacherScore('');
            refetch();
        } catch (err) {
            console.error('Failed to add feedback:', err);
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={refetch} />;
    if (!submission) return <ErrorState description="Submission not found" />;

    const status = formatSubmissionStatus(submission.status);

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
                        <ArrowLeft className="w-4 h-4 mr-2" />
                        {t('common.back')}
                    </Button>
                    <div>
                        <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                            {submission.taskTitle}
                        </h1>
                        <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                            {formatRelativeTime(submission.createdAt)}
                        </p>
                    </div>
                </div>
                <Badge
                    variant={
                        status.color === 'green' ? 'success' :
                            status.color === 'red' ? 'danger' :
                                status.color === 'amber' ? 'warning' : 'default'
                    }
                    className="text-sm px-3 py-1"
                >
                    {status.label}
                </Badge>
            </div>

            {/* Info Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <Card className="p-4">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                            <User className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                        </div>
                        <div>
                            <p className="text-sm text-zinc-500 dark:text-zinc-400">Student</p>
                            <p className="font-medium text-zinc-900 dark:text-white">
                                {submission.studentName}
                            </p>
                        </div>
                    </div>
                </Card>

                <Card className="p-4">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-green-100 dark:bg-green-900/30 rounded-lg">
                            <CheckCircle className="w-5 h-5 text-green-600 dark:text-green-400" />
                        </div>
                        <div>
                            <p className="text-sm text-zinc-500 dark:text-zinc-400">Testovi</p>
                            <p className="font-medium text-zinc-900 dark:text-white">
                                {submission.testsPassed ?? 0}/{submission.testsTotal ?? 0}
                            </p>
                        </div>
                    </div>
                </Card>

                <Card className="p-4">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-amber-100 dark:bg-amber-900/30 rounded-lg">
                            <Clock className="w-5 h-5 text-amber-600 dark:text-amber-400" />
                        </div>
                        <div>
                            <p className="text-sm text-zinc-500 dark:text-zinc-400">Vrijeme</p>
                            <p className="font-medium text-zinc-900 dark:text-white">
                                {submission.executionTimeMs ? `${submission.executionTimeMs}ms` : '-'}
                            </p>
                        </div>
                    </div>
                </Card>

                <Card className="p-4">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
                            <FileCode className="w-5 h-5 text-purple-600 dark:text-purple-400" />
                        </div>
                        <div>
                            <p className="text-sm text-zinc-500 dark:text-zinc-400">Bodovi</p>
                            <p className="font-medium text-zinc-900 dark:text-white">
                                {submission.finalScore ?? submission.teacherScore ?? submission.aiScore ?? '-'}
                            </p>
                        </div>
                    </div>
                </Card>
            </div>

            {/* Code */}
            <Card className="overflow-hidden">
                <div className="px-4 py-3 border-b border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800/50">
                    <h2 className="font-medium text-zinc-900 dark:text-white flex items-center gap-2">
                        <FileCode className="w-4 h-4" />
                        Predani kod
                    </h2>
                </div>
                <div className="h-80">
                    <Editor
                        height="100%"
                        language="c"
                        value={submission.submittedCode || '// Nema koda'}
                        theme="vs-dark"
                        options={{
                            readOnly: true,
                            minimap: { enabled: false },
                            fontSize: 14,
                            lineNumbers: 'on',
                            scrollBeyondLastLine: false,
                        }}
                    />
                </div>
            </Card>

            {/* Compiler Output */}
            {submission.compilerOutput && (
                <Card className="overflow-hidden">
                    <div className="px-4 py-3 border-b border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800/50">
                        <h2 className="font-medium text-zinc-900 dark:text-white flex items-center gap-2">
                            <AlertTriangle className="w-4 h-4 text-amber-500" />
                            Compiler Output
                        </h2>
                    </div>
                    <pre className="p-4 text-sm font-mono text-zinc-700 dark:text-zinc-300 overflow-x-auto bg-zinc-900 text-green-400">
                        {submission.compilerOutput}
                    </pre>
                </Card>
            )}

            {/* Test Results */}
            {submission.testResults && (
                <Card className="overflow-hidden">
                    <div className="px-4 py-3 border-b border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800/50">
                        <h2 className="font-medium text-zinc-900 dark:text-white flex items-center gap-2">
                            <CheckCircle className="w-4 h-4 text-green-500" />
                            Test Results
                        </h2>
                    </div>
                    <pre className="p-4 text-sm font-mono text-zinc-700 dark:text-zinc-300 overflow-x-auto">
                        {submission.testResults}
                    </pre>
                </Card>
            )}

            {/* Valgrind Output */}
            {submission.valgrindOutput && (
                <Card className="overflow-hidden">
                    <div className="px-4 py-3 border-b border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800/50">
                        <h2 className="font-medium text-zinc-900 dark:text-white flex items-center gap-2">
                            <AlertTriangle className="w-4 h-4 text-red-500" />
                            Valgrind (Memory Analysis)
                        </h2>
                    </div>
                    <pre className="p-4 text-sm font-mono bg-zinc-900 text-red-400 overflow-x-auto">
                        {submission.valgrindOutput}
                    </pre>
                </Card>
            )}

            {/* AI Feedback */}
            {submission.aiFeedback && (
                <Card className="overflow-hidden">
                    <div className="px-4 py-3 border-b border-zinc-200 dark:border-zinc-700 bg-gradient-to-r from-purple-50 to-blue-50 dark:from-purple-900/20 dark:to-blue-900/20">
                        <h2 className="font-medium text-zinc-900 dark:text-white flex items-center gap-2">
                            <Bot className="w-4 h-4 text-purple-500" />
                            AI Feedback
                            {submission.aiScore && (
                                <Badge variant="default" className="ml-2">
                                    {submission.aiScore} bodova
                                </Badge>
                            )}
                        </h2>
                    </div>
                    <div className="p-4 prose prose-sm dark:prose-invert max-w-none">
                        <p className="whitespace-pre-wrap">{submission.aiFeedback}</p>
                    </div>
                </Card>
            )}

            {/* Teacher Feedback */}
            {submission.teacherFeedback && (
                <Card className="overflow-hidden">
                    <div className="px-4 py-3 border-b border-zinc-200 dark:border-zinc-700 bg-gradient-to-r from-amber-50 to-orange-50 dark:from-amber-900/20 dark:to-orange-900/20">
                        <h2 className="font-medium text-zinc-900 dark:text-white flex items-center gap-2">
                            <MessageSquare className="w-4 h-4 text-amber-500" />
                            Nastavnički komentar
                            {submission.teacherScore && (
                                <Badge variant="warning" className="ml-2">
                                    {submission.teacherScore} bodova
                                </Badge>
                            )}
                        </h2>
                    </div>
                    <div className="p-4 prose prose-sm dark:prose-invert max-w-none">
                        <p className="whitespace-pre-wrap">{submission.teacherFeedback}</p>
                    </div>
                </Card>
            )}

            {/* Add Teacher Feedback Form */}
            {isTeacherOrAdmin && !submission.teacherFeedback && (
                <Card className="overflow-hidden">
                    <div className="px-4 py-3 border-b border-zinc-200 dark:border-zinc-700 bg-zinc-50 dark:bg-zinc-800/50">
                        <h2 className="font-medium text-zinc-900 dark:text-white flex items-center gap-2">
                            <MessageSquare className="w-4 h-4" />
                            Dodaj komentar
                        </h2>
                    </div>
                    <div className="p-4 space-y-4">
                        <textarea
                            value={teacherFeedback}
                            onChange={(e) => setTeacherFeedback(e.target.value)}
                            placeholder="Unesite komentar za studenta..."
                            className="w-full h-32 px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-amber-500 dark:bg-zinc-800 dark:text-white"
                        />
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <label className="text-sm text-zinc-600 dark:text-zinc-400">
                                    Bodovi:
                                </label>
                                <input
                                    type="number"
                                    min="0"
                                    max="100"
                                    value={teacherScore}
                                    onChange={(e) => setTeacherScore(e.target.value ? parseInt(e.target.value) : '')}
                                    className="w-20 px-3 py-2 border border-zinc-300 dark:border-zinc-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500 dark:bg-zinc-800 dark:text-white"
                                />
                            </div>
                            <Button
                                onClick={handleAddFeedback}
                                disabled={!teacherFeedback.trim() || submitting}
                            >
                                <Send className="w-4 h-4 mr-2" />
                                {submitting ? 'Slanje...' : 'Pošalji'}
                            </Button>
                        </div>
                    </div>
                </Card>
            )}
        </div>
    );
}