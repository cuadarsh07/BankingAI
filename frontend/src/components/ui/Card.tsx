import React from 'react';
import { cn } from '@/lib/utils';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'elevated' | 'subtle' | 'accent';
  withCrosshairs?: boolean;
}

export const Card = React.forwardRef<HTMLDivElement, CardProps>(
  ({ className, variant = 'default', withCrosshairs = false, children, ...props }, ref) => {
    const variantStyles = {
      default: 'bg-background-secondary border-border',
      elevated: 'bg-background-elevated border-border-strong shadow-card',
      subtle: 'bg-background border-border',
      accent: 'bg-background-secondary border-accent/40 shadow-glow',
    };

    return (
      <div
        ref={ref}
        className={cn(
          'relative border rounded-sm transition-all',
          variantStyles[variant],
          withCrosshairs && 'corner-plus',
          className
        )}
        {...props}
      >
        {withCrosshairs && (
          <>
            <span className="absolute -top-1.5 -left-1.5 text-[10px] font-mono text-foreground-muted select-none pointer-events-none">
              +
            </span>
            <span className="absolute -top-1.5 -right-1.5 text-[10px] font-mono text-foreground-muted select-none pointer-events-none">
              +
            </span>
            <span className="absolute -bottom-1.5 -left-1.5 text-[10px] font-mono text-foreground-muted select-none pointer-events-none">
              +
            </span>
            <span className="absolute -bottom-1.5 -right-1.5 text-[10px] font-mono text-foreground-muted select-none pointer-events-none">
              +
            </span>
          </>
        )}
        {children}
      </div>
    );
  }
);

Card.displayName = 'Card';

export const CardHeader: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div
    className={cn('px-5 py-4 border-b border-border flex items-center justify-between', className)}
    {...props}
  >
    {children}
  </div>
);

export const CardTitle: React.FC<React.HTMLAttributes<HTMLHeadingElement>> = ({
  className,
  children,
  ...props
}) => (
  <h3
    className={cn(
      'font-heading font-semibold text-foreground tracking-tight text-base sm:text-lg flex items-center gap-2',
      className
    )}
    {...props}
  >
    {children}
  </h3>
);

export const CardDescription: React.FC<React.HTMLAttributes<HTMLParagraphElement>> = ({
  className,
  children,
  ...props
}) => (
  <p className={cn('text-xs text-foreground-secondary font-mono tracking-tight mt-0.5', className)} {...props}>
    {children}
  </p>
);

export const CardContent: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div className={cn('p-5', className)} {...props}>
    {children}
  </div>
);

export const CardFooter: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => (
  <div
    className={cn('px-5 py-3.5 border-t border-border bg-background/50 flex items-center justify-between text-xs font-mono', className)}
    {...props}
  >
    {children}
  </div>
);

