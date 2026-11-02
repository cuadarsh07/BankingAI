import React from 'react';
import { cn } from '@/lib/utils';

export interface SectionLabelProps extends React.HTMLAttributes<HTMLDivElement> {
  index?: string;
  label: string;
  badge?: React.ReactNode;
}

export const SectionLabel: React.FC<SectionLabelProps> = ({
  index,
  label,
  badge,
  className,
  ...props
}) => {
  return (
    <div
      className={cn(
        'flex items-center justify-between text-xs font-mono tracking-tech uppercase py-2 mb-3 border-b border-border/60 text-foreground-secondary',
        className
      )}
      {...props}
    >
      <div className="flex items-center gap-2">
        {index && <span className="text-accent">{index}</span>}
        <span className="text-foreground font-semibold">{label}</span>
      </div>
      {badge && <div>{badge}</div>}
    </div>
  );
};

