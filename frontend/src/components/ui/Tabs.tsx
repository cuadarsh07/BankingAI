import React from 'react';
import { cn } from '@/lib/utils';

export interface TabItem {
  id: string;
  label: string;
  number?: string;
  count?: number;
}

export interface TabsProps {
  tabs: TabItem[];
  activeTab: string;
  onChange: (id: string) => void;
  className?: string;
}

export const Tabs: React.FC<TabsProps> = ({ tabs, activeTab, onChange, className }) => {
  return (
    <div className={cn('flex items-center space-x-1 border-b border-border overflow-x-auto no-scrollbar', className)}>
      {tabs.map((tab, idx) => {
        const isActive = activeTab === tab.id;
        const indexNumber = tab.number || `0${idx + 1}`;

        return (
          <button
            key={tab.id}
            onClick={() => onChange(tab.id)}
            className={cn(
              'relative py-3 px-4 text-xs font-mono tracking-tech uppercase transition-all duration-150 flex items-center gap-2 whitespace-nowrap',
              isActive
                ? 'text-foreground font-semibold'
                : 'text-foreground-secondary hover:text-foreground hover:bg-background-secondary/40'
            )}
          >
            <span
              className={cn(
                'text-[10px] transition-colors',
                isActive ? 'text-accent' : 'text-foreground-muted'
              )}
            >
              {indexNumber}
            </span>
            <span>{tab.label}</span>
            {tab.count !== undefined && (
              <span
                className={cn(
                  'text-[10px] px-1.5 py-0.5 rounded-sm font-mono',
                  isActive
                    ? 'bg-accent/15 text-accent'
                    : 'bg-background-elevated text-foreground-muted'
                )}
              >
                {tab.count}
              </span>
            )}
            {/* Active orange underline indicator */}
            {isActive && (
              <span className="absolute bottom-0 left-0 right-0 h-[2px] bg-accent" />
            )}
          </button>
        );
      })}
    </div>
  );
};

