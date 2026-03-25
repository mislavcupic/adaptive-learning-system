import { apiClient, ENDPOINTS } from '../api';
import type { Task, Exam } from '../types';

export const taskService = {
    getAll: async (outcomeId?: string): Promise<Task[]> => {
        const url = outcomeId ? `${ENDPOINTS.TASKS.BASE}?outcomeId=${outcomeId}` : ENDPOINTS.TASKS.BASE;
        return await apiClient.get<Task[]>(url);
    },

    getById: async (id: string): Promise<Task> => {
        return await apiClient.get<Task>(ENDPOINTS.TASKS.BY_ID(id));
    },

    getByOutcome: async (outcomeId: string): Promise<Task[]> => {
        return await apiClient.get<Task[]>(`${ENDPOINTS.TASKS.BASE}?outcomeId=${outcomeId}`);
    },

    create: async (data: Partial<Task>): Promise<Task> => {
        return await apiClient.post<Task>(ENDPOINTS.TASKS.BASE, data);
    },

    update: async (id: string, data: Partial<Task>): Promise<Task> => {
        return await apiClient.put<Task>(ENDPOINTS.TASKS.BY_ID(id), data);
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete(ENDPOINTS.TASKS.BY_ID(id));
    },

    duplicate: async (id: string): Promise<Task> => {
        return await apiClient.post<Task>(`${ENDPOINTS.TASKS.BY_ID(id)}/duplicate`);
    },

    getExams: async (): Promise<Exam[]> => {
        return await apiClient.get<Exam[]>(`${ENDPOINTS.TASKS.BASE}/exams`);
    },

    getExamById: async (id: string): Promise<Exam> => {
        return await apiClient.get<Exam>(`${ENDPOINTS.TASKS.BASE}/exams/${id}`);
    },

    createExam: async (data: Partial<Exam>): Promise<Exam> => {
        return await apiClient.post<Exam>(`${ENDPOINTS.TASKS.BASE}/exams`, data);
    },

    updateExam: async (id: string, data: Partial<Exam>): Promise<Exam> => {
        return await apiClient.put<Exam>(`${ENDPOINTS.TASKS.BASE}/exams/${id}`, data);
    },

    deleteExam: async (id: string): Promise<void> => {
        await apiClient.delete(`${ENDPOINTS.TASKS.BASE}/exams/${id}`);
    },
};