import React from 'react';
import { cn } from '@/lib/utils';

export interface PageHeaderProps {
  index?: string; // e.g. "[01]" or "01"
  tag?: string; // e.g. "OVERVIEW" or "LEDGER"
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
  statusBadge?: React.ReactNode;
  className?: string;
}

export const PageHeader: React.FC<PageHeaderProps> = ({
  index,
  tag,
  title,
  subtitle,
  actions,
  statusBadge,
  className,
}) => {
  return (
    <div className={cn('relative pb-6 border-b border-border mb-8', className)}>
      {/* Top index metadata tag */}
      <div className="flex items-center justify-between mb-3 text-xs font-mono text-foreground-secondary">
        <div className="flex items-center gap-2">
          {index && <span className="text-accent">{index}</span>}
          {tag && <span className="tracking-tech uppercase">{tag}</span>}
        </div>
        {statusBadge && <div>{statusBadge}</div>}
      </div>

      {/* Main Title and Action Row */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl sm:text-4xl md:text-5xl font-heading font-extrabold text-foreground tracking-tighter uppercase">
            {title}
          </h1>
          {subtitle && (
            <p className="mt-2 text-xs sm:text-sm text-foreground-secondary font-mono max-w-2xl leading-relaxed">
              {subtitle}
            </p>
          )}
        </div>
        {actions && <div className="flex items-center gap-3 shrink-0">{actions}</div>}
      </div>
    </div>
  );
};

