import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, Search, Users, MoreVertical, Trash2 } from 'lucide-react';
import { useFetch } from '../hooks';
import { classService } from '../services';
import {
    Card,
    CardContent,
    Button,
    Input,
    LoadingScreen,
    ErrorState,
    EmptyState,
    Modal,
    ModalFooter
} from '../components/ui';
import { formatDate } from '../utils';
import type { SchoolClass } from '../types';

const PREDEFINED_CLASSES = ['2.A', '2.B', '2.C', '2.D', '2.E', '3.A', '3.B', '3.C', '3.D', '3.E'];

const generateAcademicYears = () => {
    const currentYear = new Date().getFullYear();
    return [
        `${currentYear - 1}/${currentYear}`,
        `${currentYear}/${currentYear + 1}`,
        `${currentYear + 1}/${currentYear + 2}`,
    ];
};

export function ClassesPage() {
    const { t } = useTranslation();
    const [search, setSearch] = useState('');
    const [deleteModal, setDeleteModal] = useState<SchoolClass | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);
    const [createModal, setCreateModal] = useState(false);
    const [isCreating, setIsCreating] = useState(false);
    const [newClass, setNewClass] = useState({ name: '', academicYear: '', description: '' });

    const academicYears = generateAcademicYears();

    const { data: classes, loading, error, refetch } = useFetch<SchoolClass[]>(
        () => classService.getAll(),
        []
    );

    const filteredClasses = classes?.filter(cls =>
        cls.name.toLowerCase().includes(search.toLowerCase()) ||
        cls.academicYear.toLowerCase().includes(search.toLowerCase())
    ) ?? [];

    // Filter out existing classes
    const availableClasses = PREDEFINED_CLASSES.filter(className => {
        return !classes?.some(c => c.name === className && c.academicYear === newClass.academicYear);
    });

    const handleDelete = async () => {
        if (!deleteModal) return;
        setIsDeleting(true);
        try {
            await classService.delete(deleteModal.id);
            setDeleteModal(null);
            refetch();
        } catch (err) {
            console.error('Delete failed:', err);
        } finally {
            setIsDeleting(false);
        }
    };

    const handleCreate = async () => {
        if (!newClass.name || !newClass.academicYear) return;
        setIsCreating(true);
        try {
            await classService.create(newClass);
            setCreateModal(false);
            setNewClass({ name: '', academicYear: '', description: '' });
            refetch();
        } catch (err) {
            console.error('Create failed:', err);
        } finally {
            setIsCreating(false);
        }
    };

    const openCreateModal = () => {
        setNewClass({ name: '', academicYear: academicYears[1], description: '' });
        setCreateModal(true);
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={refetch} />;

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {t('classes.title')}
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                        {classes?.length || 0} razreda
                    </p>
                </div>
                <Button className="gap-2" onClick={openCreateModal}>
                    <Plus className="w-4 h-4" />
                    {t('classes.createClass')}
                </Button>
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

            {/* Classes Grid */}
            {filteredClasses.length === 0 ? (
                <EmptyState
                    title={t('classes.noClasses')}
                    description={search ? 'Nema rezultata za pretragu' : t('classes.noClassesDesc')}
                    action={!search && <Button onClick={openCreateModal}>{t('classes.createClass')}</Button>}
                />
            ) : (
                <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {filteredClasses.map((cls) => (
                        <ClassCard
                            key={cls.id}
                            schoolClass={cls}
                            onDelete={() => setDeleteModal(cls)}
                        />
                    ))}
                </div>
            )}

            {/* Create Modal */}
            <Modal
                isOpen={createModal}
                onClose={() => setCreateModal(false)}
                title={t('classes.createClass')}
            >
                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                            Akademska godina *
                        </label>
                        <select
                            value={newClass.academicYear}
                            onChange={(e) => setNewClass({ ...newClass, academicYear: e.target.value, name: '' })}
                            className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                        >
                            {academicYears.map((year) => (
                                <option key={year} value={year}>{year}</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-zinc-700 dark:text-zinc-300 mb-2">
                            Razred *
                        </label>
                        <select
                            value={newClass.name}
                            onChange={(e) => setNewClass({ ...newClass, name: e.target.value })}
                            className="w-full px-3 py-2 rounded-lg border border-zinc-300 dark:border-zinc-600 bg-white dark:bg-zinc-800 text-zinc-900 dark:text-white"
                        >
                            <option value="">-- Odaberi razred --</option>
                            {availableClasses.map((name) => (
                                <option key={name} value={name}>{name}</option>
                            ))}
                        </select>
                        {availableClasses.length === 0 && (
                            <p className="text-xs text-amber-600 mt-1">Svi razredi za ovu godinu već postoje.</p>
                        )}
                    </div>
                    <Input
                        label="Opis (opcionalno)"
                        value={newClass.description}
                        onChange={(e) => setNewClass({ ...newClass, description: e.target.value })}
                        placeholder="Npr. Napredna grupa..."
                    />
                </div>
                <ModalFooter>
                    <Button variant="ghost" onClick={() => setCreateModal(false)}>
                        {t('common.cancel')}
                    </Button>
                    <Button
                        onClick={handleCreate}
                        isLoading={isCreating}
                        disabled={!newClass.name || !newClass.academicYear}
                    >
                        {t('common.create')}
                    </Button>
                </ModalFooter>
            </Modal>

            {/* Delete Modal */}
            <Modal
                isOpen={!!deleteModal}
                onClose={() => setDeleteModal(null)}
                title={t('common.confirm')}
            >
                <p className="text-zinc-600 dark:text-zinc-400">
                    Razred "{deleteModal?.name}" će biti trajno obrisan.
                </p>
                <ModalFooter>
                    <Button variant="ghost" onClick={() => setDeleteModal(null)}>
                        {t('common.cancel')}
                    </Button>
                    <Button variant="danger" onClick={handleDelete} isLoading={isDeleting}>
                        {t('common.delete')}
                    </Button>
                </ModalFooter>
            </Modal>
        </div>
    );
}

function ClassCard({ schoolClass, onDelete }: { schoolClass: SchoolClass; onDelete: () => void }) {
    const { t } = useTranslation();
    const [menuOpen, setMenuOpen] = useState(false);

    return (
        <Card hoverable>
            <CardContent className="pt-4">
                <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                        <h3 className="font-medium text-zinc-900 dark:text-white">
                            {schoolClass.name}
                        </h3>
                        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1">
                            {schoolClass.academicYear}
                        </p>
                    </div>
                    <div className="relative">
                        <button
                            onClick={() => setMenuOpen(!menuOpen)}
                            className="p-1 rounded hover:bg-zinc-100 dark:hover:bg-zinc-800"
                        >
                            <MoreVertical className="w-4 h-4 text-zinc-400" />
                        </button>
                        {menuOpen && (
                            <>
                                <div className="fixed inset-0 z-10" onClick={() => setMenuOpen(false)} />
                                <div className="absolute right-0 top-8 z-20 w-40 bg-white dark:bg-zinc-800 rounded-lg shadow-lg border border-zinc-200 dark:border-zinc-700 py-1">
                                    <button
                                        onClick={() => {
                                            setMenuOpen(false);
                                            onDelete();
                                        }}
                                        className="flex items-center gap-2 px-3 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 w-full"
                                    >
                                        <Trash2 className="w-4 h-4" />
                                        {t('common.delete')}
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>

                {schoolClass.description && (
                    <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-2 line-clamp-2">
                        {schoolClass.description}
                    </p>
                )}

                <div className="flex items-center justify-between mt-4 pt-4 border-t border-zinc-100 dark:border-zinc-800">
                    <div className="flex items-center gap-2 text-sm text-zinc-500 dark:text-zinc-400">
                        <Users className="w-4 h-4" />
                        {schoolClass.studentCount || 0} studenata
                    </div>
                    <span className="text-sm text-zinc-400">
                        {formatDate(schoolClass.createdAt)}
                    </span>
                </div>
            </CardContent>
        </Card>
    );
}