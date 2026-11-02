import React from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Settings, Clock } from 'lucide-react';

export const SettingsPlaceholder: React.FC = () => {
  return (
    <div className="space-y-8">
      <PageHeader
        index="[07]"
        tag="CONFIGURATION"
        title="SYSTEM SETTINGS"
        subtitle="APPLICATION PREFERENCES, API KEYS, AND NOTIFICATION HOOKS"
        statusBadge={
          <Badge variant="warning" size="sm" className="flex items-center gap-1">
            <Clock className="w-3 h-3" />
            <span>PHASE 02 PLANNED</span>
          </Badge>
        }
      />

      <Card withCrosshairs className="p-12 text-center border-dashed border-border bg-background-secondary/40">
        <div className="w-12 h-12 rounded-sm bg-background-elevated border border-border flex items-center justify-center text-accent mx-auto mb-4">
          <Settings className="w-6 h-6" />
        </div>
        <h2 className="text-xl font-heading font-extrabold uppercase text-foreground">
          PREFERENCES HUB RESERVED
        </h2>
        <p className="text-xs font-mono text-foreground-secondary max-w-md mx-auto mt-2 leading-relaxed">
          Webhooks, developer API access tokens, and customizable multi-currency preferences
          will be configurable in Phase 02.
        </p>
      </Card>
    </div>
  );
};

