import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Search, Eye, MessageSquare } from 'lucide-react';
import { useAuth } from '../context';
import { useFetch } from '../hooks';
import { submissionService } from '../services';
import { 
    Card,
    Button,
    Input,
    Badge,
    Avatar,
    LoadingScreen,
    ErrorState,
    EmptyState,
    Table,
    TableHeader,
    TableBody,
    TableRow,
    TableHead,
    TableCell
} from '../components/ui';
import { formatRelativeTime, formatSubmissionStatus } from '../utils';
import type { Submission } from '../types';

export function SubmissionsPage() {
    const { t } = useTranslation();
    const { user } = useAuth();
    const [search, setSearch] = useState('');

    const isStudent = user?.role === 'STUDENT';

    const { data: submissions, loading, error, refetch } = useFetch<Submission[]>(
        () => isStudent ? submissionService.getMy() : submissionService.getAll(),
        [isStudent]
    );

    const filteredSubmissions = submissions?.filter(sub =>
        sub.taskTitle?.toLowerCase().includes(search.toLowerCase()) ||
        sub.studentName?.toLowerCase().includes(search.toLowerCase())
    ) ?? [];

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={refetch} />;

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div>
                <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                    {isStudent ? t('submissions.mySubmissions') : t('submissions.title')}
                </h1>
                <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                    {submissions?.length || 0} predaja
                </p>
            </div>

            {/* Search */}
            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400" />
                <Input
                    placeholder={t('common.search')}
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="pl-10"
                />
            </div>

            {/* Submissions Table */}
            {filteredSubmissions.length === 0 ? (
                <EmptyState
                    title={t('submissions.noSubmissions')}
                    description={search ? 'Nema rezultata za pretragu' : t('submissions.noSubmissionsDesc')}
                />
            ) : (
                <Card>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                {!isStudent && <TableHead>{t('students.title')}</TableHead>}
                                <TableHead>{t('tasks.title')}</TableHead>
                                <TableHead>{t('common.status')}</TableHead>
                                <TableHead>Testovi</TableHead>
                                <TableHead>Bodovi</TableHead>
                                <TableHead>{t('common.date')}</TableHead>
                                <TableHead></TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {filteredSubmissions.map((submission) => {
                                const status = formatSubmissionStatus(submission.status);
                                return (
                                    <TableRow key={submission.id}>
                                        {!isStudent && (
                                            <TableCell>
                                                <div className="flex items-center gap-2">
                                                    <Avatar
                                                        firstName={submission.studentName?.split(' ')[0]}
                                                        lastName={submission.studentName?.split(' ')[1]}
                                                        size="sm"
                                                    />
                                                    <span className="font-medium">
                                                        {submission.studentName}
                                                    </span>
                                                </div>
                                            </TableCell>
                                        )}
                                        <TableCell>
                                            <span className="font-medium">{submission.taskTitle}</span>
                                        </TableCell>
                                        <TableCell>
                                            <Badge
                                                variant={
                                                    status.color === 'green' ? 'success' :
                                                    status.color === 'red' ? 'danger' :
                                                    status.color === 'amber' ? 'warning' : 'default'
                                                }
                                            >
                                                {status.label}
                                            </Badge>
                                        </TableCell>
                                        <TableCell>
                                            {submission.testCasesPassed}/{submission.testCasesTotal}
                                        </TableCell>
                                        <TableCell>
                                            {submission.score ?? '-'}
                                        </TableCell>
                                        <TableCell className="text-zinc-500">
                                            {formatRelativeTime(submission.createdAt)}
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex items-center gap-2">
                                                <Link to={`/submissions/${submission.id}`}>
                                                    <Button variant="ghost" size="sm">
                                                        <Eye className="w-4 h-4" />
                                                    </Button>
                                                </Link>
                                                {!isStudent && !submission.teacherFeedback && (
                                                    <Link to={`/submissions/${submission.id}`}>
                                                        <Button variant="ghost" size="sm" className="text-amber-500">
                                                            <MessageSquare className="w-4 h-4" />
                                                        </Button>
                                                    </Link>
                                                )}
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </Card>
            )}
        </div>
    );
}
