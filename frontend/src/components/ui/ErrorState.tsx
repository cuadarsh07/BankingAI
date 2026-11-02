import React from 'react';
import { cn } from '@/lib/utils';
import { Button } from './Button';
import { AlertCircle, RefreshCw } from 'lucide-react';

export interface ErrorStateProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  className?: string;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'System Error Encountered',
  message,
  onRetry,
  className,
}) => {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center p-8 text-center border border-status-error-border rounded-sm bg-status-error-bg/30 max-w-lg mx-auto',
        className
      )}
    >
      <div className="w-10 h-10 flex items-center justify-center rounded-sm bg-status-error-bg text-status-error mb-3 border border-status-error-border">
        <AlertCircle className="w-5 h-5" />
      </div>
      <h3 className="font-heading font-semibold text-foreground text-sm tracking-tight mb-1">
        {title}
      </h3>
      <p className="text-xs font-mono text-status-error max-w-md mb-4 leading-relaxed">
        {message}
      </p>
      {onRetry && (
        <Button
          variant="outline"
          size="sm"
          onClick={onRetry}
          leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
        >
          Retry Operation
        </Button>
      )}
    </div>
  );
};

