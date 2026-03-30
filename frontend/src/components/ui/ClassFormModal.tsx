import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
    Modal,
    ModalFooter,
    Button,
    Input
} from '../../components/ui';
import type { SchoolClass } from '../../types';

interface ClassFormModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (data: ClassFormData) => Promise<void>;
    initialData?: SchoolClass | null;
}

export interface ClassFormData {
    name: string;
    description: string;
    academicYear: string;
}

export function ClassFormModal({ isOpen, onClose, onSubmit, initialData }: ClassFormModalProps) {
    const { t } = useTranslation();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [formData, setFormData] = useState<ClassFormData>({
        name: '',
        description: '',
        academicYear: '',
    });

    useEffect(() => {
        if (initialData) {
            setFormData({
                name: initialData.name || '',
                description: initialData.description || '',
                academicYear: initialData.academicYear || '',
            });
        } else {
            setFormData({
                name: '',
                description: '',
                academicYear: new Date().getFullYear() + '/' + (new Date().getFullYear() + 1),
            });
        }
    }, [initialData, isOpen]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSubmitting(true);
        try {
            await onSubmit(formData);
            onClose();
        } catch (err) {
            console.error('Submit failed:', err);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Modal
            isOpen={isOpen}
            onClose={onClose}
            title={initialData ? t('classes.editClass') : t('classes.createClass')}
        >
            <form onSubmit={handleSubmit} className="space-y-4">
                <Input
                    label={t('classes.name')}
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="npr. Razred 1A"
                    required
                />
                <Input
                    label={t('classes.academicYear')}
                    value={formData.academicYear}
                    onChange={(e) => setFormData({ ...formData, academicYear: e.target.value })}
                    placeholder="npr. 2025/2026"
                    required
                />
                <Input
                    label={t('classes.description')}
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Opis razreda (opcionalno)"
                />
                <ModalFooter>
                    <Button type="button" variant="ghost" onClick={onClose}>
                        {t('common.cancel')}
                    </Button>
                    <Button type="submit" isLoading={isSubmitting}>
                        {initialData ? t('common.save') : t('common.create')}
                    </Button>
                </ModalFooter>
            </form>
        </Modal>
    );
}