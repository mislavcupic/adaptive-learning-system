import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { 
    BookOpen, Plus, Search, Filter, MoreVertical, Edit, Trash2, Eye, Code
} from 'lucide-react';
import { Card, CardContent } from '../../components';
import { courseService } from '../../services';
import type { Course, LanguageType } from '../../types';

export const CoursesPage = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [courses, setCourses] = useState<Course[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [languageFilter, setLanguageFilter] = useState<LanguageType | ''>('');
    const [showCreateModal, setShowCreateModal] = useState(false);

    useEffect(() => {
        fetchCourses();
    }, []);

    const fetchCourses = async () => {
        try {
            setLoading(true);
            const data = await courseService.getAll();
            setCourses(data);
            setError(null);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load courses');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id: string) => {
        if (!confirm(t('courses.confirmDelete', 'Jeste li sigurni da želite obrisati ovaj kolegij?'))) {
            return;
        }
        try {
            await courseService.delete(id);
            setCourses(courses.filter(c => c.id !== id));
        } catch (err) {
            alert(err instanceof Error ? err.message : 'Failed to delete course');
        }
    };

    const filteredCourses = courses.filter(course => {
        const matchesSearch = course.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
            course.description?.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesLanguage = !languageFilter || course.languageType === languageFilter;
        return matchesSearch && matchesLanguage;
    });

    if (loading) return <PageSkeleton />;

    if (error) {
        return (
            <div className="p-6 text-center">
                <p className="text-red-500">{error}</p>
                <button 
                    onClick={fetchCourses}
                    className="mt-4 px-4 py-2 bg-violet-600 text-white rounded-lg hover:bg-violet-700"
                >
                    Pokušaj ponovno
                </button>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                        {t('courses.title', 'Kolegiji')}
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">
                        {t('courses.subtitle', 'Upravljajte kolegijima i ishodima učenja')}
                    </p>
                </div>
                <button
                    onClick={() => setShowCreateModal(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-violet-600 text-white rounded-lg hover:bg-violet-700 transition-colors"
                >
                    <Plus className="w-5 h-5" />
                    {t('courses.create', 'Novi kolegij')}
                </button>
            </div>

            {/* Filters */}
            <div className="flex flex-col sm:flex-row gap-4">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                        type="text"
                        placeholder={t('common.search', 'Pretraži...')}
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-violet-500 focus:border-transparent"
                    />
                </div>
                <div className="flex items-center gap-2">
                    <Filter className="w-5 h-5 text-gray-400" />
                    <select
                        value={languageFilter}
                        onChange={(e) => setLanguageFilter(e.target.value as LanguageType | '')}
                        className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-violet-500"
                    >
                        <option value="">{t('courses.allLanguages', 'Svi jezici')}</option>
                        <option value="C">C</option>
                        <option value="CSHARP">C#</option>
                    </select>
                </div>
            </div>

            {/* Courses grid */}
            {filteredCourses.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {filteredCourses.map((course) => (
                        <CourseCard 
                            key={course.id} 
                            course={course}
                            onView={() => navigate(`/courses/${course.id}`)}
                            onEdit={() => navigate(`/courses/${course.id}/edit`)}
                            onDelete={() => handleDelete(course.id)}
                        />
                    ))}
                </div>
            ) : (
                <div className="text-center py-12">
                    <BookOpen className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-500 dark:text-gray-400">
                        {searchTerm || languageFilter 
                            ? t('courses.noResults', 'Nema rezultata za vašu pretragu')
                            : t('courses.noCourses', 'Još nema kolegija')}
                    </p>
                </div>
            )}

            {/* Create Modal */}
            {showCreateModal && (
                <CreateCourseModal 
                    onClose={() => setShowCreateModal(false)}
                    onCreated={(newCourse) => {
                        setCourses([...courses, newCourse]);
                        setShowCreateModal(false);
                    }}
                />
            )}
        </div>
    );
};

// Course Card
interface CourseCardProps {
    course: Course;
    onView: () => void;
    onEdit: () => void;
    onDelete: () => void;
}

const CourseCard = ({ course, onView, onEdit, onDelete }: CourseCardProps) => {
    const [showMenu, setShowMenu] = useState(false);

    return (
        <Card className="hover:border-violet-500 transition-colors">
            <CardContent>
                <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-3">
                        <div className={`p-3 rounded-lg ${
                            course.languageType === 'C' 
                                ? 'bg-blue-100 dark:bg-blue-900' 
                                : 'bg-purple-100 dark:bg-purple-900'
                        }`}>
                            <Code className={`w-6 h-6 ${
                                course.languageType === 'C' 
                                    ? 'text-blue-600 dark:text-blue-400' 
                                    : 'text-purple-600 dark:text-purple-400'
                            }`} />
                        </div>
                        <div>
                            <h3 className="font-semibold text-gray-900 dark:text-white">
                                {course.name}
                            </h3>
                            <span className={`text-xs px-2 py-0.5 rounded ${
                                course.languageType === 'C'
                                    ? 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300'
                                    : 'bg-purple-100 text-purple-700 dark:bg-purple-900 dark:text-purple-300'
                            }`}>
                                {course.languageType === 'CSHARP' ? 'C#' : course.languageType}
                            </span>
                        </div>
                    </div>
                    <div className="relative">
                        <button 
                            onClick={() => setShowMenu(!showMenu)}
                            className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                        >
                            <MoreVertical className="w-5 h-5 text-gray-500" />
                        </button>
                        {showMenu && (
                            <div className="absolute right-0 mt-1 w-40 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg z-10">
                                <button
                                    onClick={() => { onView(); setShowMenu(false); }}
                                    className="flex items-center gap-2 w-full px-4 py-2 text-left text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                                >
                                    <Eye className="w-4 h-4" /> Pregledaj
                                </button>
                                <button
                                    onClick={() => { onEdit(); setShowMenu(false); }}
                                    className="flex items-center gap-2 w-full px-4 py-2 text-left text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
                                >
                                    <Edit className="w-4 h-4" /> Uredi
                                </button>
                                <button
                                    onClick={() => { onDelete(); setShowMenu(false); }}
                                    className="flex items-center gap-2 w-full px-4 py-2 text-left text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                                >
                                    <Trash2 className="w-4 h-4" /> Obriši
                                </button>
                            </div>
                        )}
                    </div>
                </div>

                {course.description && (
                    <p className="text-sm text-gray-600 dark:text-gray-400 mb-4 line-clamp-2">
                        {course.description}
                    </p>
                )}

                <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-500">
                        {course.outcomeCount} ishoda učenja
                    </span>
                    <span className={`px-2 py-0.5 rounded ${
                        course.isActive 
                            ? 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300'
                            : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
                    }`}>
                        {course.isActive ? 'Aktivan' : 'Neaktivan'}
                    </span>
                </div>

                <button
                    onClick={onView}
                    className="mt-4 w-full py-2 text-center text-violet-600 hover:text-violet-700 font-medium"
                >
                    Otvori kolegij →
                </button>
            </CardContent>
        </Card>
    );
};

// Create Course Modal
interface CreateCourseModalProps {
    onClose: () => void;
    onCreated: (course: Course) => void;
}

const CreateCourseModal = ({ onClose, onCreated }: CreateCourseModalProps) => {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [languageType, setLanguageType] = useState<LanguageType>('C');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!name.trim()) {
            setError('Naziv je obavezan');
            return;
        }

        try {
            setLoading(true);
            const newCourse = await courseService.create({ name, description, languageType });
            onCreated(newCourse);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to create course');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white dark:bg-gray-800 rounded-lg w-full max-w-md p-6 m-4">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    Novi kolegij
                </h2>

                <form onSubmit={handleSubmit} className="space-y-4">
                    {error && (
                        <div className="p-3 bg-red-100 text-red-700 rounded-lg text-sm">
                            {error}
                        </div>
                    )}

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Naziv *
                        </label>
                        <input
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-violet-500"
                            placeholder="npr. Uvod u programiranje"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Opis
                        </label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={3}
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-violet-500"
                            placeholder="Kratak opis kolegija..."
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Programski jezik *
                        </label>
                        <select
                            value={languageType}
                            onChange={(e) => setLanguageType(e.target.value as LanguageType)}
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-violet-500"
                        >
                            <option value="C">C</option>
                            <option value="CSHARP">C#</option>
                        </select>
                    </div>

                    <div className="flex gap-3 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
                        >
                            Odustani
                        </button>
                        <button
                            type="submit"
                            disabled={loading}
                            className="flex-1 px-4 py-2 bg-violet-600 text-white rounded-lg hover:bg-violet-700 disabled:opacity-50"
                        >
                            {loading ? 'Spremanje...' : 'Spremi'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

const PageSkeleton = () => (
    <div className="space-y-6 animate-pulse">
        <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-1/4"></div>
        <div className="flex gap-4">
            <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded flex-1"></div>
            <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded w-32"></div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
                <div key={i} className="h-48 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
            ))}
        </div>
    </div>
);

export default CoursesPage;
