import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, Search, MoreVertical, Pencil, Trash2, Eye } from 'lucide-react';
import { useFetch } from '../hooks';
import { courseService } from '../services';
import { 
    Card, 
    CardContent,
    Button,
    Input,
    Badge,
    LoadingScreen,
    ErrorState,
    EmptyState,
    Modal,
    ModalFooter
} from '../components/ui';
import { formatDate, formatLanguageType } from '../utils';
import type { Course } from '../types';

export function CoursesPage() {
    const { t } = useTranslation();
    const [search, setSearch] = useState('');
    const [deleteModal, setDeleteModal] = useState<Course | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    const { data: courses, loading, error, refetch } = useFetch<Course[]>(
        () => courseService.getAll(),
        []
    );

    const filteredCourses = courses?.filter(course =>
        course.name.toLowerCase().includes(search.toLowerCase()) ||
        course.description?.toLowerCase().includes(search.toLowerCase())
    ) ?? [];

    const handleDelete = async () => {
        if (!deleteModal) return;
        setIsDeleting(true);
        try {
            await courseService.delete(deleteModal.id);
            setDeleteModal(null);
            refetch();
        } catch (err) {
            console.error('Delete failed:', err);
        } finally {
            setIsDeleting(false);
        }
    };

    if (loading) return <LoadingScreen />;
    if (error) return <ErrorState description={error} onRetry={refetch} />;

    return (
        <div className="space-y-6 animate-fade-in">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-semibold text-zinc-900 dark:text-white">
                        {t('courses.title')}
                    </h1>
                    <p className="text-zinc-500 dark:text-zinc-400 mt-1">
                        {courses?.length || 0} kolegija
                    </p>
                </div>
                <Link to="/courses/new">
                    <Button className="gap-2">
                        <Plus className="w-4 h-4" />
                        {t('courses.createCourse')}
                    </Button>
                </Link>
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

            {/* Courses Grid */}
            {filteredCourses.length === 0 ? (
                <EmptyState
                    title={t('courses.noCourses')}
                    description={search ? 'Nema rezultata za pretragu' : t('courses.noCoursesDesc')}
                    action={
                        !search && (
                            <Link to="/courses/new">
                                <Button>{t('courses.createCourse')}</Button>
                            </Link>
                        )
                    }
                />
            ) : (
                <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {filteredCourses.map((course) => (
                        <CourseCard
                            key={course.id}
                            course={course}
                            onDelete={() => setDeleteModal(course)}
                        />
                    ))}
                </div>
            )}

            {/* Delete Modal */}
            <Modal
                isOpen={!!deleteModal}
                onClose={() => setDeleteModal(null)}
                title={t('common.confirm')}
                description={t('courses.deleteConfirm')}
            >
                <p className="text-zinc-600 dark:text-zinc-400">
                    Kolegij "{deleteModal?.name}" će biti trajno obrisan.
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

function CourseCard({ course, onDelete }: { course: Course; onDelete: () => void }) {
    const { t } = useTranslation();
    const [menuOpen, setMenuOpen] = useState(false);

    return (
        <Card hoverable>
            <CardContent className="pt-4">
                <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                        <Link to={`/courses/${course.id}`}>
                            <h3 className="font-medium text-zinc-900 dark:text-white hover:underline truncate">
                                {course.name}
                            </h3>
                        </Link>
                        <p className="text-sm text-zinc-500 dark:text-zinc-400 mt-1 line-clamp-2">
                            {course.description || 'Nema opisa'}
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
                                <div 
                                    className="fixed inset-0 z-10" 
                                    onClick={() => setMenuOpen(false)} 
                                />
                                <div className="absolute right-0 top-8 z-20 w-40 bg-white dark:bg-zinc-800 rounded-lg shadow-lg border border-zinc-200 dark:border-zinc-700 py-1">
                                    <Link
                                        to={`/courses/${course.id}`}
                                        className="flex items-center gap-2 px-3 py-2 text-sm text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-700"
                                    >
                                        <Eye className="w-4 h-4" />
                                        Pregledaj
                                    </Link>
                                    <Link
                                        to={`/courses/${course.id}/edit`}
                                        className="flex items-center gap-2 px-3 py-2 text-sm text-zinc-700 dark:text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-700"
                                    >
                                        <Pencil className="w-4 h-4" />
                                        {t('common.edit')}
                                    </Link>
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

                <div className="flex items-center gap-2 mt-4">
                    <Badge>{formatLanguageType(course.languageType)}</Badge>
                    <Badge variant={course.isActive ? 'success' : 'default'}>
                        {course.isActive ? t('common.active') : t('common.inactive')}
                    </Badge>
                </div>

                <div className="flex items-center justify-between mt-4 pt-4 border-t border-zinc-100 dark:border-zinc-800">
                    <span className="text-sm text-zinc-500 dark:text-zinc-400">
                        {course.outcomeCount || 0} ishoda
                    </span>
                    <span className="text-sm text-zinc-400">
                        {formatDate(course.createdAt)}
                    </span>
                </div>
            </CardContent>
        </Card>
    );
}
