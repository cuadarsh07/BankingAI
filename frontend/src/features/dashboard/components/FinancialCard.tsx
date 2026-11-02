import React from 'react';
import { maskAccountNumber } from '@/lib/utils';
import { Wifi, Cpu } from 'lucide-react';

interface FinancialCardProps {
  cardHolder: string;
  accountNumber: string;
  balance?: number;
  expiryDate?: string;
}

export const FinancialCard: React.FC<FinancialCardProps> = ({
  cardHolder,
  accountNumber,
  expiryDate = '12/29',
}) => {
  return (
    <div className="relative w-full max-w-sm aspect-[1.586/1] rounded-lg p-5 sm:p-6 overflow-hidden border border-border-strong bg-gradient-to-br from-[#262420] via-[#1A1917] to-[#121110] shadow-card flex flex-col justify-between text-foreground select-none group">
      {/* Subtle corner crosshairs */}
      <span className="absolute top-2 left-2 text-[10px] font-mono text-accent opacity-60">+</span>
      <span className="absolute top-2 right-2 text-[10px] font-mono text-accent opacity-60">+</span>

      {/* Top Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 bg-accent rounded-sm inline-block" />
          <span className="font-heading font-bold text-xs uppercase tracking-wider text-[#F2EEE8]">
            AETHEL BLACK
          </span>
        </div>
        <Wifi className="w-5 h-5 text-foreground-muted rotate-90" />
      </div>

      {/* Chip & Masked ID */}
      <div className="flex items-center gap-3 my-auto">
        <div className="w-10 h-7 rounded-sm bg-[#A98E64]/30 border border-[#A98E64]/60 flex items-center justify-center">
          <Cpu className="w-5 h-5 text-[#E6C687]" />
        </div>
        <span className="text-xs font-mono text-foreground-secondary tracking-widest">
          {maskAccountNumber(accountNumber)}
        </span>
      </div>

      {/* Bottom Info */}
      <div className="flex items-end justify-between pt-2 border-t border-border/40">
        <div>
          <p className="text-[9px] font-mono uppercase tracking-tech text-foreground-muted">
            CARDHOLDER
          </p>
          <p className="text-xs sm:text-sm font-mono uppercase font-semibold text-[#F2EEE8] tracking-wider">
            {cardHolder || 'ELENA ROSTOVA'}
          </p>
        </div>

        <div className="text-right">
          <p className="text-[9px] font-mono uppercase tracking-tech text-foreground-muted">
            VALID THRU
          </p>
          <p className="text-xs font-mono text-[#F2EEE8]">{expiryDate}</p>
        </div>

        <div className="flex -space-x-2">
          <div className="w-6 h-6 rounded-full bg-accent/90" />
          <div className="w-6 h-6 rounded-full bg-status-warning/90" />
        </div>
      </div>
    </div>
  );
};

