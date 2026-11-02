import React from 'react';
import { cn } from '@/lib/utils';

export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'rectangular' | 'circular' | 'text';
}

export const Skeleton: React.FC<SkeletonProps> = ({
  className,
  variant = 'rectangular',
  ...props
}) => {
  return (
    <div
      className={cn(
        'animate-pulse bg-background-subtle/80',
        variant === 'circular' && 'rounded-full',
        variant === 'text' && 'h-4 rounded-sm w-3/4',
        variant === 'rectangular' && 'rounded-sm',
        className
      )}
      {...props}
    />
  );
};

