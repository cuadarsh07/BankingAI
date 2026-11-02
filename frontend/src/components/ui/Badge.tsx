import React from 'react';
import { cn } from '@/lib/utils';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'accent' | 'success' | 'error' | 'warning' | 'neutral' | 'outline' | 'editorial';
  size?: 'sm' | 'md';
}

export const Badge: React.FC<BadgeProps> = ({
  className,
  variant = 'neutral',
  size = 'md',
  children,
  ...props
}) => {
  const variantStyles = {
    accent: 'bg-accent/10 text-accent border-accent/30',
    success: 'bg-status-success-bg text-status-success border-status-success-border',
    error: 'bg-status-error-bg text-status-error border-status-error-border',
    warning: 'bg-status-warning-bg text-status-warning border-status-warning-border',
    neutral: 'bg-background-elevated text-foreground-secondary border-border',
    outline: 'bg-transparent text-foreground-secondary border-border',
    editorial: 'bg-transparent text-foreground-secondary font-mono border-transparent p-0',
  };

  const sizeStyles = {
    sm: 'text-[10px] px-2 py-0.5 font-mono uppercase tracking-wider',
    md: 'text-xs px-2.5 py-1 font-mono uppercase tracking-tech',
  };

  return (
    <span
      className={cn(
        'inline-flex items-center justify-center font-medium border rounded-sm transition-colors',
        variantStyles[variant],
        variant !== 'editorial' && sizeStyles[size],
        className
      )}
      {...props}
    >
      {variant === 'editorial' ? `[${children}]` : children}
    </span>
  );
};

