import React, { useState, useRef, useEffect } from 'react';
import { cn } from '@/lib/utils';
import { ChevronDown } from 'lucide-react';

export interface DropdownItem {
  id: string;
  label: string;
  icon?: React.ReactNode;
  badge?: string;
  danger?: boolean;
  onClick: () => void;
}

export interface DropdownProps {
  trigger: React.ReactNode;
  items: DropdownItem[];
  align?: 'left' | 'right';
  className?: string;
}

export const Dropdown: React.FC<DropdownProps> = ({
  trigger,
  items,
  align = 'right',
  className,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  return (
    <div className={cn('relative inline-block text-left', className)} ref={dropdownRef}>
      <div onClick={() => setIsOpen(!isOpen)} className="cursor-pointer">
        {trigger}
      </div>

      {isOpen && (
        <div
          className={cn(
            'absolute z-50 mt-1.5 min-w-[180px] bg-background-elevated border border-border-strong rounded-sm shadow-card py-1 animate-in fade-in zoom-in-95 duration-100',
            align === 'right' ? 'right-0' : 'left-0'
          )}
        >
          {items.map((item) => (
            <button
              key={item.id}
              onClick={() => {
                item.onClick();
                setIsOpen(false);
              }}
              className={cn(
                'w-full flex items-center justify-between px-3.5 py-2 text-xs font-mono tracking-tight text-left transition-colors hover:bg-background-subtle',
                item.danger
                  ? 'text-status-error hover:bg-status-error-bg'
                  : 'text-foreground-secondary hover:text-foreground'
              )}
            >
              <div className="flex items-center gap-2">
                {item.icon && <span className="w-4 h-4 text-foreground-muted">{item.icon}</span>}
                <span>{item.label}</span>
              </div>
              {item.badge && (
                <span className="text-[10px] px-1.5 py-0.2 bg-background text-foreground-muted border border-border">
                  {item.badge}
                </span>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

export const SelectDropdown: React.FC<{
  label?: string;
  value: string;
  options: { label: string; value: string }[];
  onChange: (val: string) => void;
  className?: string;
}> = ({ label, value, options, onChange, className }) => {
  return (
    <div className={cn('space-y-1', className)}>
      {label && (
        <label className="block text-xs font-mono uppercase tracking-tech text-foreground-secondary">
          {label}
        </label>
      )}
      <div className="relative">
        <select
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full appearance-none bg-background-secondary border border-border text-foreground text-xs font-mono rounded-sm px-3 py-2 pr-8 focus:outline-none focus:border-accent"
        >
          {options.map((opt) => (
            <option key={opt.value} value={opt.value} className="bg-background-elevated">
              {opt.label}
            </option>
          ))}
        </select>
        <ChevronDown className="absolute right-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-foreground-muted pointer-events-none" />
      </div>
    </div>
  );
};

