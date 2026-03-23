import { api, ENDPOINTS } from '../api';
import type { StudentReminder, Task } from '../types';

export const reminderService = {
    /**
     *
     */
    getReminders: async (studentId: string): Promise<StudentReminder> => {
        const response = await api.get<StudentReminder>(
            ENDPOINTS.REMINDERS.BY_STUDENT(studentId)
        );
        return response.data;
    },

    /**
     *
     */
    getSuggestedTasks: async (studentId: string): Promise<Task[]> => {
        const response = await api.get<Task[]>(
            `${ENDPOINTS.REMINDERS.BY_STUDENT(studentId)}/suggested-tasks`
        );
        return response.data;
    },

    /**
     *
     */
    markAsRead: async (reminderId: string): Promise<void> => {
        await api.patch(`${ENDPOINTS.REMINDERS.BY_STUDENT('')}/${reminderId}/read`, {});
    },
};