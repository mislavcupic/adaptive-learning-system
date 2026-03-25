import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { 
    Users, Search, Filter, MoreVertical, Eye, TrendingUp, Mail, 
    UserCheck, UserX, ChevronLeft, ChevronRight
} from 'lucide-react';
import { Card, CardContent } from '../../components';
import { userService, classService } from '../../services';
import type { Student, SchoolClass, PaginatedResponse, GroupType } from '../../types';

export const StudentsPage = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();

    const [students, setStudents] = useState<Student[]>([]);
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    
    // Filters & Pagination
    const [searchTerm, setSearchTerm] = useState('');
    const [classFilter, setClassFilter] = useState('');
    const [groupFilter, setGroupFilter] = useState<GroupType | ''>('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const pageSize = 20;

    useEffect(() => {
        fetchClasses();
    }, []);

    useEffect(() => {
        fetchStudents();
    }, [page, classFilter, groupFilter]);

    const fetchClasses = async () => {
        try {
            const data = await classService.getAll();
            setClasses(data);
        } catch (err) {
            console.error('Failed to fetch classes:', err);
        }
    };

    const fetchStudents = async () => {
        try {
            setLoading(true);
            const params: any = { page, size: pageSize };
            if (classFilter) params.classId = classFilter;
            if (groupFilter) params.groupType = groupFilter;

            const response = await userService.getStudents(params);
            setStudents(response.content);
            setTotalPages(response.totalPages);
            setTotalElements(response.totalElements);
            setError(null);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load students');
        } finally {
            setLoading(false);
        }
    };

    const filteredStudents = students.filter(student => {
        const fullName = `${student.firstName} ${student.lastName}`.toLowerCase();
        return fullName.includes(searchTerm.toLowerCase()) ||
               student.email.toLowerCase().includes(searchTerm.toLowerCase());
    });

    const getMasteryColor = (mastery: number) => {
        if (mastery >= 80) return 'text-green-600 bg-green-100 dark:text-green-400 dark:bg-green-900';
        if (mastery >= 60) return 'text-yellow-600 bg-yellow-100 dark:text-yellow-400 dark:bg-yellow-900';
        if (mastery >= 40) return 'text-orange-600 bg-orange-100 dark:text-orange-400 dark:bg-orange-900';
        return 'text-red-600 bg-red-100 dark:text-red-400 dark:bg-red-900';
    };

    if (loading && students.length === 0) return <PageSkeleton />;

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                        {t('students.title', 'Studenti')}
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">
                        {totalElements} studenata ukupno
                    </p>
                </div>
            </div>

            {/* Filters */}
            <div className="flex flex-col sm:flex-row gap-4">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                        type="text"
                        placeholder="Pretraži po imenu ili emailu..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800"
                    />
                </div>
                <select
                    value={classFilter}
                    onChange={(e) => { setClassFilter(e.target.value); setPage(0); }}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800"
                >
                    <option value="">Svi razredi</option>
                    {classes.map(c => (
                        <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                </select>
                <select
                    value={groupFilter}
                    onChange={(e) => { setGroupFilter(e.target.value as GroupType | ''); setPage(0); }}
                    className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800"
                >
                    <option value="">Sve grupe</option>
                    <option value="EXPERIMENTAL">Eksperimentalna</option>
                    <option value="CONTROL">Kontrolna</option>
                </select>
            </div>

            {error && (
                <div className="p-4 bg-red-100 text-red-700 rounded-lg">
                    {error}
                </div>
            )}

            {/* Students Table */}
            {filteredStudents.length > 0 ? (
                <Card>
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-gray-50 dark:bg-gray-800">
                                <tr>
                                    <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">
                                        Student
                                    </th>
                                    <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">
                                        Razred
                                    </th>
                                    <th className="px-4 py-3 text-left text-sm font-medium text-gray-500">
                                        Grupa
                                    </th>
                                    <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">
                                        Razumijevanje
                                    </th>
                                    <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">
                                        Predaje
                                    </th>
                                    <th className="px-4 py-3 text-center text-sm font-medium text-gray-500">
                                        Status
                                    </th>
                                    <th className="px-4 py-3 text-right text-sm font-medium text-gray-500">
                                        Akcije
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                                {filteredStudents.map((student) => (
                                    <tr key={student.id} className="hover:bg-gray-50 dark:hover:bg-gray-800">
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-3">
                                                <div className="w-10 h-10 rounded-full bg-violet-100 dark:bg-violet-900 flex items-center justify-center">
                                                    <span className="text-violet-600 dark:text-violet-400 font-medium">
                                                        {student.firstName[0]}{student.lastName[0]}
                                                    </span>
                                                </div>
                                                <div>
                                                    <p className="font-medium text-gray-900 dark:text-white">
                                                        {student.firstName} {student.lastName}
                                                    </p>
                                                    <p className="text-sm text-gray-500">{student.email}</p>
                                                </div>
                                            </div>
                                        </td>
                                        <td className="px-4 py-3 text-gray-600 dark:text-gray-400">
                                            {student.className || '-'}
                                        </td>
                                        <td className="px-4 py-3">
                                            {student.groupType && (
                                                <span className={`text-xs px-2 py-1 rounded ${
                                                    student.groupType === 'EXPERIMENTAL'
                                                        ? 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300'
                                                        : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                                                }`}>
                                                    {student.groupType === 'EXPERIMENTAL' ? 'Eksp.' : 'Kontr.'}
                                                </span>
                                            )}
                                        </td>
                                        <td className="px-4 py-3 text-center">
                                            <span className={`text-sm font-medium px-2 py-1 rounded ${
                                                getMasteryColor(student.averageMastery || 0)
                                            }`}>
                                                {(student.averageMastery || 0).toFixed(0)}%
                                            </span>
                                        </td>
                                        <td className="px-4 py-3 text-center text-gray-600 dark:text-gray-400">
                                            {student.totalSubmissions || 0}
                                        </td>
                                        <td className="px-4 py-3 text-center">
                                            {student.isActive ? (
                                                <span className="flex items-center justify-center gap-1 text-green-600">
                                                    <UserCheck className="w-4 h-4" />
                                                </span>
                                            ) : (
                                                <span className="flex items-center justify-center gap-1 text-gray-400">
                                                    <UserX className="w-4 h-4" />
                                                </span>
                                            )}
                                        </td>
                                        <td className="px-4 py-3 text-right">
                                            <div className="flex items-center justify-end gap-2">
                                                <button
                                                    onClick={() => navigate(`/students/${student.id}`)}
                                                    className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                                                    title="Pregledaj"
                                                >
                                                    <Eye className="w-4 h-4 text-gray-500" />
                                                </button>
                                                <button
                                                    onClick={() => navigate(`/students/${student.id}/progress`)}
                                                    className="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                                                    title="Napredak"
                                                >
                                                    <TrendingUp className="w-4 h-4 text-gray-500" />
                                                </button>
                                            </div>
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
                    <Users className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-500">
                        {searchTerm || classFilter || groupFilter 
                            ? 'Nema rezultata za vašu pretragu'
                            : 'Još nema studenata'}
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

export default StudentsPage;
