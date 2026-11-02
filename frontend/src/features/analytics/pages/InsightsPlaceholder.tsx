import React from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { BarChart3, Clock } from 'lucide-react';

export const InsightsPlaceholder: React.FC = () => {
  return (
    <div className="space-y-8">
      <PageHeader
        index="[05]"
        tag="PREDICTIVE INTELLIGENCE"
        title="PORTFOLIO INSIGHTS"
        subtitle="CAPITAL ALLOCATION MODELING AND ALGORITHMIC CASH FLOW FORECASTING"
        statusBadge={
          <Badge variant="warning" size="sm" className="flex items-center gap-1">
            <Clock className="w-3 h-3" />
            <span>PHASE 02 PLANNED</span>
          </Badge>
        }
      />

      <Card withCrosshairs className="p-12 text-center border-dashed border-border bg-background-secondary/40">
        <div className="w-12 h-12 rounded-sm bg-background-elevated border border-border flex items-center justify-center text-accent mx-auto mb-4">
          <BarChart3 className="w-6 h-6" />
        </div>
        <h2 className="text-xl font-heading font-extrabold uppercase text-foreground">
          ANALYTICS ENGINE RESERVED
        </h2>
        <p className="text-xs font-mono text-foreground-secondary max-w-md mx-auto mt-2 leading-relaxed">
          Deep liquidity insights, counterparty spending analytics, and automated tax reporting
          will be enabled in Phase 02 after the transaction core stabilizes.
        </p>
      </Card>
    </div>
  );
};

