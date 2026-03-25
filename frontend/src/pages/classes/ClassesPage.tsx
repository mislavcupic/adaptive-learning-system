import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { 
    Users, Plus, Search, MoreVertical, Edit, Trash2, Eye, UserPlus
} from 'lucide-react';
import { Card, CardContent } from '../../components';
import { classService } from '../../services';
import type { SchoolClass } from '../../types';

export const ClassesPage = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [classes, setClasses] = useState<SchoolClass[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [showCreateModal, setShowCreateModal] = useState(false);

    useEffect(() => {
        fetchClasses();
    }, []);

    const fetchClasses = async () => {
        try {
            setLoading(true);
            const data = await classService.getAll();
            setClasses(data);
            setError(null);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to load classes');
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id: string) => {
        if (!confirm('Jeste li sigurni da želite obrisati ovaj razred?')) return;
        try {
            await classService.delete(id);
            setClasses(classes.filter(c => c.id !== id));
        } catch (err) {
            alert(err instanceof Error ? err.message : 'Failed to delete class');
        }
    };

    const filteredClasses = classes.filter(cls =>
        cls.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        cls.academicYear.toLowerCase().includes(searchTerm.toLowerCase())
    );

    if (loading) return <PageSkeleton />;

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                        {t('classes.title', 'Razredi')}
                    </h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">
                        Upravljajte razredima i studentima
                    </p>
                </div>
                <button
                    onClick={() => setShowCreateModal(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-violet-600 text-white rounded-lg hover:bg-violet-700"
                >
                    <Plus className="w-5 h-5" />
                    Novi razred
                </button>
            </div>

            {/* Search */}
            <div className="relative max-w-md">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                    type="text"
                    placeholder="Pretraži razrede..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800"
                />
            </div>

            {error && (
                <div className="p-4 bg-red-100 text-red-700 rounded-lg">{error}</div>
            )}

            {/* Classes Grid */}
            {filteredClasses.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {filteredClasses.map((cls) => (
                        <ClassCard
                            key={cls.id}
                            schoolClass={cls}
                            onView={() => navigate(`/classes/${cls.id}`)}
                            onEdit={() => navigate(`/classes/${cls.id}/edit`)}
                            onDelete={() => handleDelete(cls.id)}
                        />
                    ))}
                </div>
            ) : (
                <div className="text-center py-12">
                    <Users className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-500">
                        {searchTerm ? 'Nema rezultata' : 'Još nema razreda'}
                    </p>
                </div>
            )}

            {/* Create Modal */}
            {showCreateModal && (
                <CreateClassModal
                    onClose={() => setShowCreateModal(false)}
                    onCreated={(newClass) => {
                        setClasses([...classes, newClass]);
                        setShowCreateModal(false);
                    }}
                />
            )}
        </div>
    );
};

interface ClassCardProps {
    schoolClass: SchoolClass;
    onView: () => void;
    onEdit: () => void;
    onDelete: () => void;
}

const ClassCard = ({ schoolClass, onView, onEdit, onDelete }: ClassCardProps) => {
    const [showMenu, setShowMenu] = useState(false);

    return (
        <Card className="hover:border-violet-500 transition-colors">
            <CardContent>
                <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-3">
                        <div className="p-3 bg-violet-100 dark:bg-violet-900 rounded-lg">
                            <Users className="w-6 h-6 text-violet-600 dark:text-violet-400" />
                        </div>
                        <div>
                            <h3 className="font-semibold text-gray-900 dark:text-white">
                                {schoolClass.name}
                            </h3>
                            <span className="text-sm text-gray-500">
                                {schoolClass.academicYear}
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
                                    className="flex items-center gap-2 w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700"
                                >
                                    <Eye className="w-4 h-4" /> Pregledaj
                                </button>
                                <button
                                    onClick={() => { onEdit(); setShowMenu(false); }}
                                    className="flex items-center gap-2 w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700"
                                >
                                    <Edit className="w-4 h-4" /> Uredi
                                </button>
                                <button
                                    onClick={() => { onDelete(); setShowMenu(false); }}
                                    className="flex items-center gap-2 w-full px-4 py-2 text-left text-red-600 hover:bg-red-50"
                                >
                                    <Trash2 className="w-4 h-4" /> Obriši
                                </button>
                            </div>
                        )}
                    </div>
                </div>

                {schoolClass.description && (
                    <p className="text-sm text-gray-600 dark:text-gray-400 mb-4 line-clamp-2">
                        {schoolClass.description}
                    </p>
                )}

                <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                        <p className="text-gray-500">Studenata</p>
                        <p className="font-semibold text-gray-900 dark:text-white">
                            {schoolClass.studentCount}
                        </p>
                    </div>
                    <div>
                        <p className="text-gray-500">Kolegija</p>
                        <p className="font-semibold text-gray-900 dark:text-white">
                            {schoolClass.courseCount}
                        </p>
                    </div>
                </div>

                <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                    <p className="text-xs text-gray-500">
                        Nastavnik: {schoolClass.teacherName}
                    </p>
                </div>

                <button
                    onClick={onView}
                    className="mt-4 w-full py-2 text-center text-violet-600 hover:text-violet-700 font-medium"
                >
                    Otvori razred →
                </button>
            </CardContent>
        </Card>
    );
};

interface CreateClassModalProps {
    onClose: () => void;
    onCreated: (cls: SchoolClass) => void;
}

const CreateClassModal = ({ onClose, onCreated }: CreateClassModalProps) => {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [academicYear, setAcademicYear] = useState('2025/2026');
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
            const newClass = await classService.create({ name, description, academicYear });
            onCreated(newClass);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to create class');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white dark:bg-gray-800 rounded-lg w-full max-w-md p-6 m-4">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4">
                    Novi razred
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
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700"
                            placeholder="npr. 3.A"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Školska godina
                        </label>
                        <input
                            type="text"
                            value={academicYear}
                            onChange={(e) => setAcademicYear(e.target.value)}
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700"
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
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700"
                        />
                    </div>

                    <div className="flex gap-3 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
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
        <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded w-64"></div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
                <div key={i} className="h-64 bg-gray-200 dark:bg-gray-700 rounded-lg"></div>
            ))}
        </div>
    </div>
);

export default ClassesPage;
