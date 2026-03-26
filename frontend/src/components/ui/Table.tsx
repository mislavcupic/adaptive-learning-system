import { HTMLAttributes, TdHTMLAttributes, ThHTMLAttributes, forwardRef } from 'react';
import { cn } from '../../utils/cn';

export const Table = forwardRef<HTMLTableElement, HTMLAttributes<HTMLTableElement>>(
    ({ className, ...props }, ref) => (
        <div className="w-full overflow-auto">
            <table
                ref={ref}
                className={cn('w-full caption-bottom text-sm', className)}
                {...props}
            />
        </div>
    )
);
Table.displayName = 'Table';

export const TableHeader = forwardRef<HTMLTableSectionElement, HTMLAttributes<HTMLTableSectionElement>>(
    ({ className, ...props }, ref) => (
        <thead ref={ref} className={cn('[&_tr]:border-b', className)} {...props} />
    )
);
TableHeader.displayName = 'TableHeader';

export const TableBody = forwardRef<HTMLTableSectionElement, HTMLAttributes<HTMLTableSectionElement>>(
    ({ className, ...props }, ref) => (
        <tbody
            ref={ref}
            className={cn('[&_tr:last-child]:border-0', className)}
            {...props}
        />
    )
);
TableBody.displayName = 'TableBody';

export const TableRow = forwardRef<HTMLTableRowElement, HTMLAttributes<HTMLTableRowElement>>(
    ({ className, ...props }, ref) => (
        <tr
            ref={ref}
            className={cn(
                'border-b border-zinc-100 dark:border-zinc-800 transition-colors',
                'hover:bg-zinc-50 dark:hover:bg-zinc-800/50',
                className
            )}
            {...props}
        />
    )
);
TableRow.displayName = 'TableRow';

export const TableHead = forwardRef<HTMLTableCellElement, ThHTMLAttributes<HTMLTableCellElement>>(
    ({ className, ...props }, ref) => (
        <th
            ref={ref}
            className={cn(
                'h-10 px-4 text-left align-middle font-medium',
                'text-zinc-500 dark:text-zinc-400 text-xs uppercase tracking-wider',
                '[&:has([role=checkbox])]:pr-0',
                className
            )}
            {...props}
        />
    )
);
TableHead.displayName = 'TableHead';

export const TableCell = forwardRef<HTMLTableCellElement, TdHTMLAttributes<HTMLTableCellElement>>(
    ({ className, ...props }, ref) => (
        <td
            ref={ref}
            className={cn(
                'px-4 py-3 align-middle text-zinc-700 dark:text-zinc-300',
                '[&:has([role=checkbox])]:pr-0',
                className
            )}
            {...props}
        />
    )
);
TableCell.displayName = 'TableCell';
