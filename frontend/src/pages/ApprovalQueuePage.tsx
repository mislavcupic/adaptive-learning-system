import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { UserPlus, Check, X, Users } from 'lucide-react';
import { useFetch } from '../hooks';
import { apiClient } from '../api/client';
import { classService } from '../services';
import {
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    Button,
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
    TableCell,
    Modal,
    ModalFooter
} from '../components/ui';
import { formatDate } from '../utils';
import type { User, SchoolClass } from '../types';

export function ApprovalQueuePage() {
    const { t } = useTranslation();
    const [processingId, setProcessingId] = useState<string | null>(null);
    const [approveModal, setApproveModal] = useState<User | null>(null);
    const [selectedClassId, setSelectedClassId] = useState<string>('');
    const [includeInResearch, setIncludeInResearch] = useState(false);

    const { data: pendingUsers, loading, error, refetch } = useFetch<User[]>(
        async () => {
            const res = await apiClient.get('/teacher/pending-registrations');
            return (res as { data: User[] }).data;
        },
        []
    );

    const { data: classes } = useFetch<SchoolClass[]>(
        () => classService.getAll(),
        []
    );

    const handleApprove = async () => {
        if (!approveModal) return;

        setProcessingId(approveModal.id);
        try {
            await apiClient.put(`/teacher/approve/${approveModal.id}`, {
                schoolClassId: selectedClassId || null,
                includeInResearch
            });
            setApproveModal(null);
            setSelectedClassId('');
            setIncludeInResearch(false);
            refetch();
            window.dispatchEvent(new CustomEvent('pendingCountChanged'));
        } catch (err) {
            console.error('Failed to approve user:', err);
        } finally {
            setProcessingId(null);
        }
    };

    const handleReject = async (userId: string) => {
        if (!confirm(t('approvals.confirmReject'))) return;

        setProcessingId(userId);
        try {
            await apiClient.delete(`/teacher/reject/${userId}`);
            refetch();
            window.dispatchEvent(new CustomEvent('pendingCountChanged'));
        } catch (err) {
            console.error('Failed to reject user:', err);
        } finally {
            setProcessingId(null);
        }
    };

    const handleCloseModal = () => {
        setApproveModal(null);
        setSelectedClassId('');
        setIncludeInResearch(false);
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={refetch} />;

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex items-center gap-3">
                <div className="p-2 bg-amber-100 dark:bg-amber-900/30 rounded-lg">
                    <UserPlus className="w-6 h-6 text-amber-600 dark:text-amber-400" />
                </div>
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {t('approvals.title')}
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400">
                        {pendingUsers?.length || 0} {t('approvals.pending')}
                    </p>
                </div>
            </div>

            {/* Content */}
            {!pendingUsers || pendingUsers.length === 0 ? (
                <Card>
                    <CardContent className="py-12">
                        <EmptyState
                            icon={<Users className="w-12 h-12" />}
                            title={t('approvals.noPending')}
                            description={t('approvals.noPendingDesc')}
                        />
                    </CardContent>
                </Card>
            ) : (
                <Card>
                    <CardHeader>
                        <CardTitle>{t('approvals.newRegistrations')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>{t('common.user')}</TableHead>
                                    <TableHead>Email</TableHead>
                                    <TableHead>{t('approvals.registeredAt')}</TableHead>
                                    <TableHead className="text-right">{t('common.actions')}</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {pendingUsers.map((user) => (
                                    <TableRow key={user.id}>
                                        <TableCell>
                                            <div className="flex items-center gap-3">
                                                <Avatar
                                                    firstName={user.firstName}
                                                    lastName={user.lastName}
                                                    size="sm"
                                                />
                                                <div>
                                                    <p className="font-medium text-zinc-900 dark:text-white">
                                                        {user.firstName} {user.lastName}
                                                    </p>
                                                    <Badge variant="default" className="mt-1">
                                                        {t('approvals.guest')}
                                                    </Badge>
                                                </div>
                                            </div>
                                        </TableCell>
                                        <TableCell className="text-zinc-500">
                                            {user.email}
                                        </TableCell>
                                        <TableCell className="text-zinc-500">
                                            {formatDate(user.createdAt)}
                                        </TableCell>
                                        <TableCell>
                                            <div className="flex items-center justify-end gap-2">
                                                <Button
                                                    variant="ghost"
                                                    size="sm"
                                                    onClick={() => handleReject(user.id)}
                                                    disabled={processingId === user.id}
                                                    className="text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20"
                                                >
                                                    <X className="w-4 h-4 mr-1" />
                                                    {t('approvals.reject')}
                                                </Button>
                                                <Button
                                                    size="sm"
                                                    onClick={() => setApproveModal(user)}
                                                    disabled={processingId === user.id}
                                                    className="bg-emerald-600 hover:bg-emerald-700"
                                                >
                                                    <Check className="w-4 h-4 mr-1" />
                                                    {t('approvals.approve')}
                                                </Button>
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </CardContent>
                </Card>
            )}

            {/* Approve Modal */}
            <Modal
                isOpen={!!approveModal}
                onClose={handleCloseModal}
                title={t('approvals.approveUser')}
            >
                <div className="space-y-4">
                    <p className="text-zinc-600 dark:text-zinc-400">
                        {t('approvals.approveConfirm', {
                            name: `${approveModal?.firstName} ${approveModal?.lastName}`
                        })}
                    </p>

                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                            {t('approvals.assignToClass')}
                        </label>
                        <select
                            value={selectedClassId}
                            onChange={(e) => setSelectedClassId(e.target.value)}
                            className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-zinc-500"
                        >
                            <option value="">{t('approvals.noClass')}</option>
                            {classes?.map((cls) => (
                                <option key={cls.id} value={cls.id}>
                                    {cls.name} ({cls.academicYear})
                                </option>
                            ))}
                        </select>
                        <p className="text-xs text-zinc-500 dark:text-zinc-400 mt-1">
                            {t('approvals.classHint')}
                        </p>
                    </div>

                    <div className="flex items-center gap-2">
                        <input
                            type="checkbox"
                            id="includeInResearch"
                            checked={includeInResearch}
                            onChange={(e) => setIncludeInResearch(e.target.checked)}
                            className="rounded border-zinc-300 dark:border-zinc-600"
                        />
                        <label
                            htmlFor="includeInResearch"
                            className="text-sm text-zinc-700 dark:text-zinc-300"
                        >
                            Uključi u istraživanje
                        </label>
                    </div>
                </div>

                <ModalFooter>
                    <Button variant="ghost" onClick={handleCloseModal}>
                        {t('common.cancel')}
                    </Button>
                    <Button
                        onClick={handleApprove}
                        isLoading={processingId === approveModal?.id}
                        className="bg-emerald-600 hover:bg-emerald-700"
                    >
                        {t('approvals.confirmApprove')}
                    </Button>
                </ModalFooter>
            </Modal>
        </div>
    );
}