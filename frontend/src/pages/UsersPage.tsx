import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Search } from 'lucide-react';
import { useFetch } from '../hooks';
import { apiClient } from '../api/client';
import {
    Card, Input, Avatar,
    LoadingScreen, ErrorState, EmptyState,
    Table, TableHeader, TableBody, TableRow, TableHead, TableCell
} from '../components/ui';
import { formatDate } from '../utils';
import type { User, UserRole } from '../types';

export function UsersPage() {
    const { t } = useTranslation();
    const [search, setSearch] = useState('');

    const { data: users, loading, error, refetch } = useFetch<User[]>(
        async () => {
            const res = await apiClient.get('/admin/users');
            return (res as { data: User[] }).data;
        },
        []
    );

    const handleRoleChange = async (userId: string, role: UserRole) => {
        try {
            await apiClient.put(`/admin/users/${userId}/role`, { role });
            refetch();
        } catch (err) {
            console.error('Failed to update role:', err);
        }
    };

    const handleStatusToggle = async (userId: string, currentStatus: boolean) => {
        try {
            await apiClient.put(`/admin/users/${userId}/status`, { isActive: !currentStatus });
            refetch();
        } catch (err) {
            console.error('Failed to update status:', err);
        }
    };

    const filteredUsers = users?.filter(user =>
        user.firstName.toLowerCase().includes(search.toLowerCase()) ||
        user.lastName.toLowerCase().includes(search.toLowerCase()) ||
        user.email.toLowerCase().includes(search.toLowerCase())
    ) ?? [];

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={refetch} />;

    return (
        <div className="space-y-6 animate-fade-in">
            <div>
                <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                    Korisnici
                </h1>
                <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                    {users?.length || 0} korisnika u sustavu
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

            {filteredUsers.length === 0 ? (
                <EmptyState
                    title="Nema korisnika"
                    description={search ? 'Nema rezultata za pretragu' : 'Još nema registriranih korisnika'}
                />
            ) : (
                <Card>
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Korisnik</TableHead>
                                <TableHead>Email</TableHead>
                                <TableHead>Uloga</TableHead>
                                <TableHead>Status</TableHead>
                                <TableHead>Registriran</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {filteredUsers.map((user) => (
                                <TableRow key={user.id}>
                                    <TableCell>
                                        <div className="flex items-center gap-3">
                                            <Avatar firstName={user.firstName} lastName={user.lastName} size="sm" />
                                            <span className="font-medium text-zinc-900 dark:text-white">
                                                {user.firstName} {user.lastName}
                                            </span>
                                        </div>
                                    </TableCell>
                                    <TableCell className="text-zinc-500">
                                        {user.email}
                                    </TableCell>
                                    <TableCell>
                                        <select
                                            value={user.role}
                                            onChange={(e) => handleRoleChange(user.id, e.target.value as UserRole)}
                                            className="px-3 py-1.5 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-500"
                                        >
                                            <option value="ADMIN">Admin</option>
                                            <option value="TEACHER">Nastavnik</option>
                                            <option value="STUDENT">Student</option>
                                            <option value="GUEST">Gost</option>
                                        </select>
                                    </TableCell>
                                    <TableCell>
                                        <button
                                            onClick={() => handleStatusToggle(user.id, user.isActive)}
                                            className={`px-3 py-1 rounded-full text-sm font-medium transition-colors ${
                                                user.isActive
                                                    ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 hover:bg-emerald-200 dark:hover:bg-emerald-900/50'
                                                    : 'bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400 hover:bg-zinc-200 dark:hover:bg-zinc-700'
                                            }`}
                                        >
                                            {user.isActive ? 'Aktivan' : 'Neaktivan'}
                                        </button>
                                    </TableCell>
                                    <TableCell className="text-zinc-500">
                                        {formatDate(user.createdAt)}
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