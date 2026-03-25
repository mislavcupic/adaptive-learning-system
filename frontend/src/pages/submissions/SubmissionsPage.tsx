import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { 
    FileText, Search, Filter, Eye, CheckCircle, XCircle, Clock, 
    AlertTriangle, Loader2, ChevronLeft, ChevronRight, MessageSquare
} from 'lucide-react';
import { Card } from '../../components';
import { submissionService, taskService } from '../../services';
import type { Submission, Task, SubmissionStatus } from '../../types';

export const SubmissionsPage = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const taskIdFilter = searchParams.get('taskId');
    const studentIdFilter = searchParams.get('studentId');

    const [submissions, setSubmissions] = useState<Submission[]>([]);
    const [tasks, setTasks] = useState<Task[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    
    // Filters & Pagination
    const [searchTerm, setSearchTerm] = useState('');
    const [taskFilter, setTaskFilter] = useState(taskIdFilter || '');
    const [statusFilter, setStatusFilter] = useState<SubmissionStatus | ''>('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const pageSize = 20;

    useEffect(() => {
        fetchTasks();
    }, []);

    useEffect(() => {
        fetchSubmissions();
    }, [page, taskFilter, studentIdFilter]);

    const fetchTasks = async () => {
        try {
            const data = await taskService.getAll();
            setTasks(data);
        } catch (err) {
            console.error('Failed to fetch tasks:', err);
        }
    };

    const fetchSubmissions = async () => {
        try {
            setLoading(true);
            let response;
            
            if (taskFilter) {
                response = await submissionService.getByTask(taskFilter, { page, size: pageSize });
            } else if (studentIdFilter) {
                response = await submissionService.getByStudent(studentIdFilter, { page, size: pageSize });
            } else {
                // Dohvati sve predaje - za nastavnika
                const mySubmissions = await submissionService.getMy();
                setSubmissions(mySubmissions);
                setTotalPages(1);
                setTotalElements(mySubmissions.length);
                setLoading(false);
                return;
            }
            
            setSubmissions(response.content);
            setTotalPages(response.totalPages);
            setTotalElements(response.totalElements);
            setError(null);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load submissions');
        } finally {
            setLoading(false);
        }
    };

    const filteredSubmissions = submissions.filter(sub => {
        const matchesSearch = sub.taskTitle.toLowerCase().includes(searchTerm.toLowerCase()) ||
                            sub.studentName.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesStatus = !statusFilter || sub.status === statusFilter;
        return matchesSearch && matchesStatus;
    });

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
            case 'PENDING':
            case 'COMPILING':
            case 'RUNNING':
                return <Loader2 className="w-5 h-5 text-blue-500 animate-spin" />;
            default:
                return <Clock className="w-5 h-5 text-gray-400" />;
        }
    };

    const getStatusBadge = (status: SubmissionStatus) => {
        const styles: Record<SubmissionStatus, string> = {
            COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300',
            COMPILE_ERROR: 'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300',
            RUNTIME_ERROR: 'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300',
            TIMEOUT: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900 dark:text-yellow-300',
            FAILED: 'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300',
            PENDING: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
            COMPILING: 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300',
            RUNNING: 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300',
        };

        const labels: Record<SubmissionStatus, string> = {
            COMPLETED: 'Uspješno',
            COMPILE_ERROR: 'Greška kompilacije',
            RUNTIME_ERROR: 'Runtime greška',
            TIMEOUT: 'Timeout',
            FAILED: 'Neuspješno',
            PENDING: 'Na čekanju',
            COMPILING: 'Kompilira se',
            RUNNING: 'Izvršava se',
        };

        return (
            <span className={`text-xs px-2 py-1 rounded ${styles[status]}`}>
                {labels[status]}
            </span>
        );
    };

    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('hr-HR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    if (loading && submissions.length === 0) return <PageSkeleton />;

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                        {t('submissions.title', 'Predaje')}
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">
                        {totalElements} predaja ukupno
                    </p>
                </div>
            </div>

            {/* Filters */}
            <div className="flex flex-col sm:flex-row gap-4">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                        type="text"
                        placeholder="Pretraži po zadatku ili studentu..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800"
                    />
                </div>
                <select
                    value={taskFilter}
                    onChange={(e) => { setTaskFilter(e.target.value); setPage(0); }}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800"
                >
                    <option value="">Svi zadaci</option>
                    {tasks.map(t => (
                        <option key={t.id} value={t.id}>{t.title}</option>
                    ))}
                </select>
                <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value as SubmissionStatus | '')}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800"
                >
                    <option value="">Svi statusi</option>
                    <option value="COMPLETED">Uspješno</option>
                    <option value="COMPILE_ERROR">Greška kompilacije</option>
                    <option value="RUNTIME_ERROR">Runtime greška</option>
                    <option value="TIMEOUT">Timeout</option>
                    <option value="PENDING">Na čekanju</option>
                </select>
            </div>

            {error && (
                <div className="p-4 bg-red-100 text-red-700 rounded-lg">
                    {error}
                </div>
            )}

            {/* Submissions Table */}
            {filteredSubmissions.length > 0 ? (
                <Card>
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-gray-50 dark:bg-gray-800">
                                <tr>
                                    <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">
                                        Status
                                    </th>
                                    <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">
                                        Zadatak
                                    </th>
                                    <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">
                                        Student
                                    </th>
                                    <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">
                                        Testovi
                                    </th>
                                    <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">
                                        Bodovi
                                    </th>
                                    <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">
                                        Vrijeme
                                    </th>
                                    <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">
                                        Feedback
                                    </th>
                                    <th className="px-4 py-3 text-right text-sm font-medium text-gray-500">
                                        Akcije
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                                {filteredSubmissions.map((submission) => (
                                    <tr key={submission.id} className="hover:bg-gray-50 dark:hover:bg-gray-800">
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-2">
                                                {getStatusIcon(submission.status)}
                                                {getStatusBadge(submission.status)}
                                            </div>
                                        </td>
                                        <td className="px-4 py-3">
                                            <p className="font-medium text-gray-900 dark:text-white">
                                                {submission.taskTitle}
                                            </p>
                                        </td>
                                        <td className="px-4 py-3 text-gray-600 dark:text-gray-400">
                                            {submission.studentName}
                                        </td>
                                        <td className="px-4 py-3 text-center">
                                            <span className={`font-medium ${
                                                submission.testCasesPassed === submission.testCasesTotal
                                                    ? 'text-green-600'
                                                    : 'text-gray-600 dark:text-gray-400'
                                            }`}>
                                                {submission.testCasesPassed}/{submission.testCasesTotal}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3 text-center">
                                            {submission.score !== null ? (
                                                <span className="font-medium text-gray-900 dark:text-white">
                                                    {submission.score}
                                                </span>
                                            ) : (
                                                <span className="text-gray-400">-</span>
                                            )}
                                        </td>
                                        <td className="px-4 py-3 text-sm text-gray-500">
                                            {formatDate(submission.createdAt)}
                                        </td>
                                        <td className="px-4 py-3 text-center">
                                            {submission.aiFeedback ? (
                                                <span className="text-violet-600" title="Ima AI feedback">
                                                    <MessageSquare className="w-5 h-5 mx-auto" />
                                                </span>
                                            ) : (
                                                <span className="text-gray-300">-</span>
                                            )}
                                        </td>
                                        <td className="px-4 py-3 text-right">
                                            <button
                                                onClick={() => navigate(`/submissions/${submission.id}`)}
                                                className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                                                title="Pregledaj"
                                            >
                                                <Eye className="w-4 h-4 text-gray-500" />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Pagination */}
                    {totalPages > 1 && (
                        <div className="flex items-center justify-between px-4 py-3 border-t border-gray-200 dark:border-gray-700">
                            <p className="text-sm text-gray-500">
                                Stranica {page + 1} od {totalPages}
                            </p>
                            <div className="flex gap-2">
                                <button
                                    onClick={() => setPage(p => Math.max(0, p - 1))}
                                    disabled={page === 0}
                                    className="p-2 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-50"
                                >
                                    <ChevronLeft className="w-4 h-4" />
                                </button>
                                <button
                                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                                    disabled={page >= totalPages - 1}
                                    className="p-2 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-50"
                                >
                                    <ChevronRight className="w-4 h-4" />
                                </button>
                            </div>
                        </div>
                    )}
                </Card>
            ) : (
                <div className="text-center py-12">
                    <FileText className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-500">
                        {searchTerm || taskFilter || statusFilter 
                            ? 'Nema rezultata za vašu pretragu'
                            : 'Još nema predaja'}
                    </p>
                </div>
            )}
        </div>
    );
};

const PageSkeleton = () => (
    <div className="space-y-6 animate-pulse">
        <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-1/4"></div>
        <div className="flex gap-4">
            <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded flex-1"></div>
            <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded w-40"></div>
            <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded w-40"></div>
        </div>
        <div className="h-96 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
    </div>
);

export default SubmissionsPage;
