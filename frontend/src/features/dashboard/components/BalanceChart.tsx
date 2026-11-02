import React from 'react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { formatCurrency } from '@/lib/utils';

interface BalanceChartProps {
  data?: { date: string; balance: number }[];
}

const defaultChartData = [
  { date: 'OCT 01', balance: 42000 },
  { date: 'OCT 06', balance: 48500 },
  { date: 'OCT 12', balance: 44200 },
  { date: 'OCT 18', balance: 59000 },
  { date: 'OCT 24', balance: 67300 },
  { date: 'OCT 30', balance: 84500 },
  { date: 'OCT 31', balance: 92450 },
];

export const BalanceChart: React.FC<BalanceChartProps> = ({ data = defaultChartData }) => {
  return (
    <div className="w-full h-56 pt-2">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
          <defs>
            <linearGradient id="balanceOrangeGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#FF5533" stopOpacity={0.35} />
              <stop offset="95%" stopColor="#FF5533" stopOpacity={0.0} />
            </linearGradient>
          </defs>
          <XAxis
            dataKey="date"
            stroke="var(--color-border-subtle)"
            tick={{ fill: 'var(--color-text-muted)', fontSize: 10, fontFamily: 'JetBrains Mono' }}
            axisLine={false}
            tickLine={false}
          />
          <YAxis
            stroke="var(--color-border-subtle)"
            tick={{ fill: 'var(--color-text-muted)', fontSize: 10, fontFamily: 'JetBrains Mono' }}
            axisLine={false}
            tickLine={false}
            tickFormatter={(val) => `₹${(val / 1000).toFixed(0)}k`}
          />
          <Tooltip
            content={({ active, payload }) => {
              if (active && payload && payload.length) {
                const val = payload[0]?.value as number;
                const label = payload[0]?.payload?.date as string;
                return (
                  <div className="bg-background-elevated border border-border-strong p-2.5 rounded-sm shadow-card text-xs font-mono">
                    <p className="text-foreground-secondary mb-1">{label}</p>
                    <p className="text-accent font-bold text-sm">
                      {formatCurrency(val)}
                    </p>
                  </div>
                );
              }
              return null;
            }}
          />
          <Area
            type="monotone"
            dataKey="balance"
            stroke="#FF5533"
            strokeWidth={2}
            fillOpacity={1}
            fill="url(#balanceOrangeGrad)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};

