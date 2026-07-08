// types/audit.ts
// Dodaj u types barrel (types/index.ts):  export * from './audit';

export interface AuditLog {
    id: string;
    userId: string | null;
    userEmail: string | null;
    userRole: string | null;
    action: string;
    httpMethod: string | null;
    endpoint: string | null;
    success: boolean;
    errorMessage: string | null;
    durationMs: number | null;
    ipAddress: string | null;
    createdAt: string;
}