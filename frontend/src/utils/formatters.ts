// Date formatting
export const formatDate = (dateStr: string | null | undefined): string => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('hr-HR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
    });
};

export const formatDateTime = (dateStr: string | null | undefined): string => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('hr-HR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
};

export const formatRelativeTime = (dateStr: string | null | undefined): string => {
    if (!dateStr) return '-';
    
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);
    
    if (diffSec < 60) return 'Upravo sada';
    if (diffMin < 60) return `Prije ${diffMin} min`;
    if (diffHour < 24) return `Prije ${diffHour}h`;
    if (diffDay < 7) return `Prije ${diffDay} dana`;
    
    return formatDate(dateStr);
};

// Number formatting
export const formatPercent = (value: number | null | undefined, decimals = 0): string => {
    if (value === null || value === undefined) return '-';
    return `${(value * 100).toFixed(decimals)}%`;
};

export const formatNumber = (value: number | null | undefined): string => {
    if (value === null || value === undefined) return '-';
    return value.toLocaleString('hr-HR');
};

// Status formatting
export const formatSubmissionStatus = (status: string): { label: string; color: string } => {
    const statusMap: Record<string, { label: string; color: string }> = {
        PENDING: { label: 'Na čekanju', color: 'amber' },
        COMPILING: { label: 'Kompilira se', color: 'blue' },
        RUNNING: { label: 'Izvršava se', color: 'blue' },
        COMPLETED: { label: 'Završeno', color: 'green' },
        COMPILE_ERROR: { label: 'Greška kompilacije', color: 'red' },
        RUNTIME_ERROR: { label: 'Greška izvršavanja', color: 'red' },
        TIMEOUT: { label: 'Isteklo vrijeme', color: 'red' },
        FAILED: { label: 'Neuspješno', color: 'red' },
    };
    
    return statusMap[status] || { label: status, color: 'gray' };
};

export const formatLanguageType = (type: string): string => {
    const languageMap: Record<string, string> = {
        C: 'C',
        CSHARP: 'C#',
    };
    return languageMap[type] || type;
};

// Truncate text
export const truncate = (text: string | null | undefined, maxLength: number): string => {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return `${text.slice(0, maxLength)}...`;
};

// Name formatting
export const formatFullName = (firstName?: string, lastName?: string): string => {
    return [firstName, lastName].filter(Boolean).join(' ') || '-';
};

export const getInitials = (firstName?: string, lastName?: string): string => {
    const first = firstName?.charAt(0)?.toUpperCase() || '';
    const last = lastName?.charAt(0)?.toUpperCase() || '';
    return first + last || '?';
};
