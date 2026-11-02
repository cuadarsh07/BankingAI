import React from 'react';
import { cn } from '@/lib/utils';
import { Loader2 } from 'lucide-react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon,
      rightIcon,
      children,
      disabled,
      ...props
    },
    ref
  ) => {
    const baseStyles =
      'inline-flex items-center justify-center font-medium transition-all duration-150 focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-accent disabled:opacity-50 disabled:pointer-events-none select-none active:translate-y-[0.5px]';

    const variantStyles = {
      primary:
        'bg-accent text-white hover:bg-accent-hover active:bg-accent-active shadow-subtle font-semibold tracking-wide border border-transparent',
      secondary:
        'bg-background-elevated text-foreground hover:bg-background-subtle border border-border hover:border-border-strong',
      outline:
        'bg-transparent text-foreground hover:bg-background-elevated border border-border hover:border-accent hover:text-accent',
      ghost:
        'bg-transparent text-foreground-secondary hover:text-foreground hover:bg-background-subtle',
      danger:
        'bg-status-error text-white hover:bg-red-600 active:bg-red-700 font-semibold',
    };

    const sizeStyles = {
      sm: 'h-8 px-3 text-xs tracking-wide rounded-sm gap-1.5',
      md: 'h-10 px-4 text-sm tracking-wide rounded-sm gap-2',
      lg: 'h-12 px-6 text-base tracking-wide rounded-sm gap-2.5',
    };

    return (
      <button
        ref={ref}
        disabled={disabled || isLoading}
        className={cn(baseStyles, variantStyles[variant], sizeStyles[size], className)}
        {...props}
      >
        {isLoading ? (
          <Loader2 className="w-4 h-4 animate-spin text-current" />
        ) : (
          leftIcon && <span className="inline-flex shrink-0">{leftIcon}</span>
        )}
        <span>{children}</span>
        {!isLoading && rightIcon && (
          <span className="inline-flex shrink-0">{rightIcon}</span>
        )}
      </button>
    );
  }
);

Button.displayName = 'Button';

