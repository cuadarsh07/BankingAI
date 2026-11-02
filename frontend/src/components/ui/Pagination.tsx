import React from 'react';
import { cn } from '@/lib/utils';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export interface PaginationProps {
  currentPage: number; // 0-indexed as per Spring Data Page
  totalPages: number;
  totalElements?: number;
  pageSize?: number;
  onPageChange: (newPage: number) => void;
  className?: string;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  totalElements,
  pageSize,
  onPageChange,
  className,
}) => {
  if (totalPages <= 1 && !totalElements) return null;

  const startElement = pageSize !== undefined ? currentPage * pageSize + 1 : undefined;
  const endElement =
    pageSize !== undefined && totalElements !== undefined
      ? Math.min((currentPage + 1) * pageSize, totalElements)
      : undefined;

  return (
    <div
      className={cn(
        'flex flex-col sm:flex-row items-center justify-between gap-3 px-4 py-3 border-t border-border bg-background-secondary/50 text-xs font-mono',
        className
      )}
    >
      <div className="text-foreground-secondary">
        {totalElements !== undefined && startElement !== undefined && endElement !== undefined ? (
          <span>
            SHOWING <strong className="text-foreground">{startElement}</strong> TO{' '}
            <strong className="text-foreground">{endElement}</strong> OF{' '}
            <strong className="text-foreground">{totalElements}</strong> ENTRIES
          </span>
        ) : (
          <span>
            PAGE <strong className="text-foreground">{currentPage + 1}</strong> OF{' '}
            <strong className="text-foreground">{Math.max(totalPages, 1)}</strong>
          </span>
        )}
      </div>

      <div className="flex items-center space-x-1">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 0}
          className="p-1.5 rounded-sm border border-border text-foreground-secondary hover:text-foreground hover:bg-background-elevated disabled:opacity-40 disabled:pointer-events-none transition-colors"
          aria-label="Previous Page"
        >
          <ChevronLeft className="w-4 h-4" />
        </button>

        {Array.from({ length: totalPages }).map((_, idx) => {
          // Show up to 5 pages around current
          if (
            totalPages > 7 &&
            idx !== 0 &&
            idx !== totalPages - 1 &&
            Math.abs(idx - currentPage) > 1
          ) {
            if (idx === 1 || idx === totalPages - 2) {
              return (
                <span key={idx} className="px-2 text-foreground-muted select-none">
                  ...
                </span>
              );
            }
            return null;
          }

          const isCurrent = idx === currentPage;
          return (
            <button
              key={idx}
              onClick={() => onPageChange(idx)}
              className={cn(
                'min-w-7 h-7 px-2 flex items-center justify-center rounded-sm text-xs font-mono transition-colors border',
                isCurrent
                  ? 'bg-accent text-white border-accent font-semibold'
                  : 'border-border text-foreground-secondary hover:text-foreground hover:bg-background-elevated'
              )}
            >
              {idx + 1}
            </button>
          );
        })}

        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          className="p-1.5 rounded-sm border border-border text-foreground-secondary hover:text-foreground hover:bg-background-elevated disabled:opacity-40 disabled:pointer-events-none transition-colors"
          aria-label="Next Page"
        >
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

