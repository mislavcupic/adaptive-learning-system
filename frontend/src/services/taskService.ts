import { apiClient, ENDPOINTS } from '../api';
import type { Task, Exam } from '../types';

interface ApiResponse<T> {
    success: boolean;
    message: string | null;
    data: T;
}

export const taskService = {
    getAll: async (outcomeId?: string): Promise<Task[]> => {
        const url = outcomeId 
            ? `${ENDPOINTS.TASKS.BASE}?outcomeId=${outcomeId}` 
            : ENDPOINTS.TASKS.BASE;
        const response = await apiClient.get<ApiResponse<Task[]>>(url);
        return response.data;
    },

    getById: async (id: string): Promise<Task> => {
        const response = await apiClient.get<ApiResponse<Task>>(ENDPOINTS.TASKS.BY_ID(id));
        return response.data;
    },

    getByOutcome: async (outcomeId: string): Promise<Task[]> => {
        const response = await apiClient.get<ApiResponse<Task[]>>(
            `${ENDPOINTS.TASKS.BASE}?outcomeId=${outcomeId}`
        );
        return response.data;
    },

    getPending: async (): Promise<Task[]> => {
        const response = await apiClient.get<ApiResponse<Task[]>>('/tasks/pending');
        return response.data;
    },

    create: async (data: Partial<Task>): Promise<Task> => {
        const response = await apiClient.post<ApiResponse<Task>>(ENDPOINTS.TASKS.BASE, data);
        return response.data;
    },

    update: async (id: string, data: Partial<Task>): Promise<Task> => {
        const response = await apiClient.put<ApiResponse<Task>>(ENDPOINTS.TASKS.BY_ID(id), data);
        return response.data;
    },

    delete: async (id: string): Promise<void> => {
        await apiClient.delete(ENDPOINTS.TASKS.BY_ID(id));
    },

    duplicate: async (id: string): Promise<Task> => {
        const response = await apiClient.post<ApiResponse<Task>>(
            `${ENDPOINTS.TASKS.BY_ID(id)}/duplicate`
        );
        return response.data;
    },

    // Exams
    getExams: async (): Promise<Exam[]> => {
        const response = await apiClient.get<ApiResponse<Exam[]>>(`${ENDPOINTS.TASKS.BASE}/exams`);
        return response.data;
    },

    getExamById: async (id: string): Promise<Exam> => {
        const response = await apiClient.get<ApiResponse<Exam>>(`${ENDPOINTS.TASKS.BASE}/exams/${id}`);
        return response.data;
    },

    createExam: async (data: Partial<Exam>): Promise<Exam> => {
        const response = await apiClient.post<ApiResponse<Exam>>(`${ENDPOINTS.TASKS.BASE}/exams`, data);
        return response.data;
    },

    updateExam: async (id: string, data: Partial<Exam>): Promise<Exam> => {
        const response = await apiClient.put<ApiResponse<Exam>>(`${ENDPOINTS.TASKS.BASE}/exams/${id}`, data);
        return response.data;
    },

    deleteExam: async (id: string): Promise<void> => {
        await apiClient.delete(`${ENDPOINTS.TASKS.BASE}/exams/${id}`);
    },
};
