import { apiClient, ENDPOINTS } from '../api';
import type { ApiResponse, AuditLog } from '../types';

export const auditService = {
    getLogs: async (): Promise<AuditLog[]> => {
        const response = await apiClient.get<ApiResponse<AuditLog[]>>(
            ENDPOINTS.AUDIT.LOGS
        );
        return response.data;
    },
};