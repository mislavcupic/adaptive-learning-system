import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Search, Eye, UserX, UserCheck } from 'lucide-react';
import { useFetch } from '../hooks';
import { userService } from '../services';
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
import { formatDate } from '../utils';
import type { User } from '../types';

export function StudentsPage() {
    const { t } = useTranslation();
    const [search, setSearch] = useState('');

    const { data: students, loading, error, refetch } = useFetch<User[]>(
        () => userService.getStudents(),
        []
    );

    const [updatingId, setUpdatingId] = useState<string | null>(null);

    const toggleResearch = async (student: User) => {
        const name = `${student.firstName} ${student.lastName}`;
        const exclude = !student.excludedFromResearch;
        const question = exclude
            ? t('students.excludeConfirm', { name })
            : t('students.includeConfirm', { name });
        if (!window.confirm(question)) return;

        setUpdatingId(student.id);
        try {
            await userService.setResearchExclusion(student.id, exclude);
            await refetch();
        } finally {
            setUpdatingId(null);
        }
    };

    const filteredStudents = students?.filter(student =>
        student.firstName.toLowerCase().includes(search.toLowerCase()) ||
        student.lastName.toLowerCase().includes(search.toLowerCase()) ||
        student.email.toLowerCase().includes(search.toLowerCase())
    ) ?? [];

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={refetch} />;

    return (
        <div className="space-y-6 animate-fade-in">
            <div>
                <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                    {t('students.title')}
                </h1>
                <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                    {students?.length || 0} studenata
                </p>
            </div>

            <div className="relative max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400" />
                <Input
                    placeholder={t('common.search')}
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="pl-10"
                />
            </div>

            {filteredStudents.length === 0 ? (
                <EmptyState
                    title={t('students.noStudents')}
                    description={search ? 'Nema rezultata za pretragu' : t('students.noStudentsDesc')}
                />
            ) : (
                <Card>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Student</TableHead>
                                <TableHead>Email</TableHead>
                                <TableHead>Razred</TableHead>
                                <TableHead>Istraživačka grupa</TableHead>
                                <TableHead>{t('common.status')}</TableHead>
                                <TableHead>Registriran</TableHead>
                                <TableHead></TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {filteredStudents.map((student) => (
                                <TableRow key={student.id}>
                                    <TableCell>
                                        <div className="flex items-center gap-3">
                                            <Avatar
                                                firstName={student.firstName}
                                                lastName={student.lastName}
                                                size="sm"
                                            />
                                            <p className="font-medium text-zinc-900 dark:text-white">
                                                {student.firstName} {student.lastName}
                                            </p>
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-zinc-500">
                                        {student.email}
                                    </TableCell>
                                    <TableCell className="text-zinc-500">
                                        {student.schoolClassName || '—'}
                                    </TableCell>
                                    <TableCell>
                                        {student.excludedFromResearch ? (
                                            <Badge variant="warning">{t('students.excluded')}</Badge>
                                        ) : student.researchGroup && student.researchGroup !== 'NOT_ASSIGNED' ? (
                                            <Badge variant={student.researchGroup === 'EXPERIMENTAL' ? 'info' : 'default'}>
                                                {student.researchGroup === 'EXPERIMENTAL'
                                                    ? t('students.experimental')
                                                    : t('students.control')}
                                            </Badge>
                                        ) : (
                                            <span className="text-zinc-400 text-sm">—</span>
                                        )}
                                    </TableCell>
                                    <TableCell>
                                        <Badge variant={student.isActive ? 'success' : 'default'}>
                                            {student.isActive ? t('common.active') : t('common.inactive')}
                                        </Badge>
                                    </TableCell>
                                    <TableCell className="text-zinc-500">
                                        {formatDate(student.createdAt)}
                                    </TableCell>
                                    <TableCell>
                                        <div className="flex items-center gap-1">
                                            <Link to={`/students/${student.id}`}>
                                                <Button variant="ghost" size="sm" className="gap-2">
                                                    <Eye className="w-4 h-4" />
                                                    Pregledaj
                                                </Button>
                                            </Link>
                                            <Button
                                                variant="ghost"
                                                size="sm"
                                                className="gap-2"
                                                isLoading={updatingId === student.id}
                                                onClick={() => toggleResearch(student)}
                                            >
                                                {student.excludedFromResearch
                                                    ? <UserCheck className="w-4 h-4" />
                                                    : <UserX className="w-4 h-4" />}
                                                {student.excludedFromResearch
                                                    ? t('students.include')
                                                    : t('students.exclude')}
                                            </Button>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </Card>
            )}
        </div>
    );
}