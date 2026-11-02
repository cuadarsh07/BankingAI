import React from 'react';
import { cn } from '@/lib/utils';
import { Button } from './Button';
import { Inbox } from 'lucide-react';

export interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  className,
}) => {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center p-8 sm:p-12 text-center border border-dashed border-border rounded-sm bg-background-secondary/30',
        className
      )}
    >
      <div className="w-12 h-12 flex items-center justify-center rounded-sm bg-background-elevated border border-border text-foreground-muted mb-4">
        {icon || <Inbox className="w-6 h-6 text-foreground-muted" />}
      </div>
      <h3 className="font-heading font-semibold text-foreground text-base tracking-tight mb-1">
        {title}
      </h3>
      <p className="text-xs font-mono text-foreground-secondary max-w-sm mb-6 leading-relaxed">
        {description}
      </p>
      {actionLabel && onAction && (
        <Button variant="primary" size="sm" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
};

