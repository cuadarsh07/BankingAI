import React from 'react';

export const Footer: React.FC = () => {
  return (
    <footer className="w-full border-t border-border bg-background py-8 px-4 sm:px-8 mt-auto">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4 text-xs font-mono text-foreground-secondary">
        <div className="flex items-center gap-3">
          <span className="w-2 h-2 bg-accent rounded-sm inline-block" />
          <span className="text-foreground font-semibold">AETHEL BANKING ENGINE</span>
          <span className="text-foreground-muted">/</span>
          <span className="text-foreground-muted">VERSION 2.4.0 (STAGE: PROD)</span>
        </div>

        <div className="flex items-center gap-6 text-[11px] text-foreground-muted">
          <span>SPRING BOOT 3.x REST CORE</span>
          <span>•</span>
          <span>STRICT TYPE COUPLING</span>
          <span>•</span>
          <span>© 2026 AETHEL FINTECH</span>
        </div>
      </div>
    </footer>
  );
};

