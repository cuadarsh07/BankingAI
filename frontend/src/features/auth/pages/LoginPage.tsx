import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { ENV } from '@/config/env';
import { Input } from '@/components/ui/Input';
import { PasswordInput } from '@/components/ui/PasswordInput';
import { Button } from '@/components/ui/Button';
import { toast } from 'sonner';
import {
  Shield,
  Sparkles,
  UserCheck,
  AlertOctagon,
  Bot,
  Lock,
  Activity,
  ArrowRight,
  ShieldCheck,
  Zap,
} from 'lucide-react';

const loginSchema = z.object({
  username: z.string().min(1, 'Please enter a username or email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginPage: React.FC = () => {
  const { login, enableInstantOfflineDemo } = useAuth();
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [warmingMessage, setWarmingMessage] = useState<string | null>(null);

  // Auto-wake Render backend immediately on page hit
  useEffect(() => {
    fetch(`${ENV.API_BASE_URL}/api/auth/health`, { method: 'GET', mode: 'cors' }).catch(() => {
      fetch(`${ENV.API_BASE_URL}/`, { method: 'GET', mode: 'no-cors' }).catch(() => {});
    });
  }, []);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: '',
      password: '',
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsSubmitting(true);
    setWarmingMessage(null);
    const timer = setTimeout(() => {
      setWarmingMessage('Connecting to cloud backend (Render free tier wakes up in ~30s)...');
    }, 2500);

    try {
      await login(data);
      clearTimeout(timer);
      toast.success('Login successful. Welcome to Aethel Bank.');
      navigate('/dashboard');
    } catch (err: unknown) {
      clearTimeout(timer);
      const error = err as Error;
      toast.error(error.message || 'Authentication failed. Please verify credentials.');
    } finally {
      clearTimeout(timer);
      setIsSubmitting(false);
      setWarmingMessage(null);
    }
  };

  const handleQuickDemo = async (role: 'ROLE_CUSTOMER' | 'ROLE_ADMIN') => {
    setIsSubmitting(true);
    setWarmingMessage(null);
    const timer = setTimeout(() => {
      setWarmingMessage('Waking up cloud backend on Render (~30s)... or click "Instant Demo Preview" below.');
    }, 2500);

    try {
      const username = role === 'ROLE_CUSTOMER' ? 'alice@example.com' : 'admin@example.com';
      setValue('username', username);
      setValue('password', 'Password123');
      await login({ username, password: 'Password123' });
      clearTimeout(timer);
      toast.success(`Authenticated as ${role === 'ROLE_ADMIN' ? 'Security Admin' : 'Customer (Alice)'}`);
      navigate(role === 'ROLE_ADMIN' ? '/admin/dashboard' : '/dashboard');
    } catch (err: unknown) {
      clearTimeout(timer);
      console.warn('Backend sleeping or unreachable. Auto-engaging interactive demo session:', err);
      enableInstantOfflineDemo(role);
      toast.info(`Server warming up: Connected to Interactive Demo with full ${role === 'ROLE_ADMIN' ? 'Admin telemetry' : 'Alice Spender profile & transactions'}!`);
      navigate(role === 'ROLE_ADMIN' ? '/admin/dashboard' : '/dashboard');
    } finally {
      clearTimeout(timer);
      setIsSubmitting(false);
      setWarmingMessage(null);
    }
  };

  const handleInstantOfflineDemo = (role: 'ROLE_CUSTOMER' | 'ROLE_ADMIN') => {
    enableInstantOfflineDemo(role);
    toast.success(`Loaded Instant Demo session as ${role === 'ROLE_ADMIN' ? 'Security Admin' : 'Customer (Alice)'} with zero wait time.`);
    navigate(role === 'ROLE_ADMIN' ? '/admin/dashboard' : '/dashboard');
  };

  return (
    <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center py-8 lg:py-16 px-4 sm:px-6 lg:px-8">
      <div className="w-full max-w-7xl mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-14 items-center">
          
          {/* Left Column: Product Showcase & Value Proposition */}
          <div className="lg:col-span-7 space-y-6">
            
            {/* Live Security Badge */}
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-accent/10 border border-accent/25 text-xs font-mono text-accent">
              <span className="w-2 h-2 rounded-full bg-accent animate-pulse" />
              <span className="font-semibold">AI Fraud Shield & Digital Arrest Defense Engine</span>
            </div>

            {/* Main Headline */}
            <div className="space-y-3">
              <h1 className="text-3xl sm:text-4xl lg:text-5xl font-heading font-extrabold text-foreground tracking-tight leading-[1.15]">
                Intelligent Banking with{' '}
                <span className="text-accent underline decoration-accent/40 underline-offset-8">
                  Real-Time AI Defense
                </span>
              </h1>
              <p className="text-sm sm:text-base text-foreground-secondary leading-relaxed max-w-2xl">
                A resilient banking platform engineered with Spring Boot, PostgreSQL, and Google Gemini AI to protect your hard-earned funds against digital arrest extortion, social engineering scams, and unauthorized anomalies in real time.
              </p>
            </div>

            {/* Why Features Exist - Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5 pt-2">
              
              {/* Feature 1: Digital Arrest Prevention */}
              <div className="p-4 rounded-lg bg-background-elevated/70 border border-border hover:border-accent/40 transition-colors space-y-2">
                <div className="flex items-center gap-2 text-accent">
                  <div className="p-1.5 rounded bg-accent/10">
                    <AlertOctagon className="w-4 h-4 text-accent" />
                  </div>
                  <h3 className="text-xs font-heading font-bold uppercase tracking-wider text-foreground">
                    Digital Arrest Scam Shield
                  </h3>
                </div>
                <p className="text-xs text-foreground-secondary leading-normal">
                  Multilingual AI scanner (Hindi, English, Hinglish) detecting threats from scammers posing as Police, CBI, or ED demanding coercive transfers.
                </p>
              </div>

              {/* Feature 2: Statistical Anomaly Detection */}
              <div className="p-4 rounded-lg bg-background-elevated/70 border border-border hover:border-accent/40 transition-colors space-y-2">
                <div className="flex items-center gap-2 text-status-warning">
                  <div className="p-1.5 rounded bg-status-warning/10">
                    <Activity className="w-4 h-4 text-status-warning" />
                  </div>
                  <h3 className="text-xs font-heading font-bold uppercase tracking-wider text-foreground">
                    Behavioral Anomaly Engine
                  </h3>
                </div>
                <p className="text-xs text-foreground-secondary leading-normal">
                  Identifies sudden spikes (e.g. ₹2K avg vs ₹5L sudden transfer), velocity bursts, off-hours activity, and first-time unknown beneficiaries.
                </p>
              </div>

              {/* Feature 3: Zero Pre-Debit Hold Workflow */}
              <div className="p-4 rounded-lg bg-background-elevated/70 border border-border hover:border-accent/40 transition-colors space-y-2">
                <div className="flex items-center gap-2 text-status-success">
                  <div className="p-1.5 rounded bg-status-success/10">
                    <Lock className="w-4 h-4 text-status-success" />
                  </div>
                  <h3 className="text-xs font-heading font-bold uppercase tracking-wider text-foreground">
                    Zero Pre-Debit On-Hold
                  </h3>
                </div>
                <p className="text-xs text-foreground-secondary leading-normal">
                  Suspicious transfers enter a safe on-hold state with zero money debited until the customer answers security checks or bank review completes.
                </p>
              </div>

              {/* Feature 4: Contextual Banking Copilot */}
              <div className="p-4 rounded-lg bg-background-elevated/70 border border-border hover:border-accent/40 transition-colors space-y-2">
                <div className="flex items-center gap-2 text-accent">
                  <div className="p-1.5 rounded bg-accent/10">
                    <Bot className="w-4 h-4 text-accent" />
                  </div>
                  <h3 className="text-xs font-heading font-bold uppercase tracking-wider text-foreground">
                    Contextual AI Copilot
                  </h3>
                </div>
                <p className="text-xs text-foreground-secondary leading-normal">
                  24/7 AI security assistant powered by Gemini to answer banking limits, explain held transactions, and guide scam victims calmly.
                </p>
              </div>
            </div>

            {/* Architecture Highlights Footer */}
            <div className="flex flex-wrap items-center gap-2 pt-2 text-[11px] font-mono text-foreground-muted">
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-background-secondary border border-border">
                <ShieldCheck className="w-3 h-3 text-status-success" />
                <span>Deterministic Banking Policy</span>
              </span>
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-background-secondary border border-border">
                <Zap className="w-3 h-3 text-accent" />
                <span>LangChain4j + Google Gemini</span>
              </span>
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-background-secondary border border-border">
                <Lock className="w-3 h-3 text-status-warning" />
                <span>Pessimistic DB Locks</span>
              </span>
            </div>
          </div>

          {/* Right Column: Clean, Intuitive Login Card */}
          <div className="lg:col-span-5 w-full max-w-md mx-auto">
            <div className="bg-background-elevated border border-border-strong rounded-xl shadow-2xl overflow-hidden backdrop-blur-sm">
              
              {/* Clean Card Header without horizontal wrapping conflicts */}
              <div className="p-6 border-b border-border space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-md bg-accent/15 border border-accent/30 flex items-center justify-center text-accent">
                      <Shield className="w-4 h-4 fill-accent/20" />
                    </div>
                    <h2 className="text-xl font-heading font-bold text-foreground tracking-tight">
                      Sign In to Account
                    </h2>
                  </div>
                  <span className="text-[10px] font-mono font-semibold px-2.5 py-1 rounded-full bg-accent/15 text-accent border border-accent/30 tracking-wider">
                    SECURE LOGIN
                  </span>
                </div>
                <p className="text-xs text-foreground-secondary leading-relaxed">
                  Access your banking dashboard, transfer funds, or manage security alerts.
                </p>
              </div>

              <div className="p-6 space-y-5">
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                  <Input
                    label="Username or Email Address"
                    placeholder="e.g. alice@example.com"
                    {...register('username')}
                    error={errors.username?.message}
                    hint="Demo accounts: alice@example.com or admin@example.com"
                  />

                  <PasswordInput
                    label="Account Password"
                    placeholder="••••••••••••"
                    {...register('password')}
                    error={errors.password?.message}
                    hint="Demo password: Password123"
                  />

                  {warmingMessage && (
                    <div className="p-3 rounded-lg border border-accent/40 bg-accent/10 text-xs font-mono text-accent flex items-start gap-2.5 animate-pulse">
                      <Activity className="w-4 h-4 mt-0.5 shrink-0 animate-spin" />
                      <div className="space-y-1">
                        <p className="font-semibold">{warmingMessage}</p>
                        <p className="text-[11px] text-foreground-secondary">
                          Render free instances sleep after inactivity. Click an option below to enter immediately:
                        </p>
                        <div className="flex gap-2 pt-1">
                          <button
                            type="button"
                            onClick={() => handleInstantOfflineDemo('ROLE_CUSTOMER')}
                            className="px-2 py-0.5 rounded bg-accent text-background font-bold text-[10px] hover:opacity-90"
                          >
                            Explore Customer (0s wait)
                          </button>
                          <button
                            type="button"
                            onClick={() => handleInstantOfflineDemo('ROLE_ADMIN')}
                            className="px-2 py-0.5 rounded bg-foreground text-background font-bold text-[10px] hover:opacity-90"
                          >
                            Explore Admin (0s wait)
                          </button>
                        </div>
                      </div>
                    </div>
                  )}

                  <div className="pt-2">
                    <Button
                      type="submit"
                      variant="primary"
                      className="w-full text-sm font-semibold tracking-wide py-3 shadow-glow"
                      isLoading={isSubmitting}
                      rightIcon={<ArrowRight className="w-4 h-4" />}
                    >
                      SIGN IN TO ACCOUNT
                    </Button>
                  </div>
                </form>

                {/* Instant Real JWT Demo Access Helpers */}
                <div className="pt-4 border-t border-border space-y-3">
                  <div className="flex items-center justify-between text-[11px] font-mono text-foreground-muted">
                    <span className="flex items-center gap-1.5 text-accent font-semibold">
                      <Sparkles className="w-3.5 h-3.5" />
                      <span>INSTANT DEMO SESSIONS:</span>
                    </span>
                    <span className="text-[10px] text-foreground-muted">CLOUD + OFFLINE FALLBACK</span>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <button
                      type="button"
                      disabled={isSubmitting}
                      onClick={() => handleQuickDemo('ROLE_CUSTOMER')}
                      className="flex flex-col items-start p-3 rounded-lg border border-border bg-background-secondary/90 hover:bg-background-secondary hover:border-accent transition-all text-left group disabled:opacity-60"
                    >
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-foreground group-hover:text-accent">
                        <UserCheck className="w-4 h-4 text-accent" />
                        <span>Customer (Alice)</span>
                      </div>
                      <span className="text-[10px] font-mono text-foreground-muted mt-1 leading-tight">
                        6mo history + Copilot
                      </span>
                    </button>

                    <button
                      type="button"
                      disabled={isSubmitting}
                      onClick={() => handleQuickDemo('ROLE_ADMIN')}
                      className="flex flex-col items-start p-3 rounded-lg border border-border bg-background-secondary/90 hover:bg-background-secondary hover:border-status-warning transition-all text-left group disabled:opacity-60"
                    >
                      <div className="flex items-center gap-1.5 text-xs font-semibold text-foreground group-hover:text-status-warning">
                        <Shield className="w-4 h-4 text-status-warning" />
                        <span>Admin (Security)</span>
                      </div>
                      <span className="text-[10px] font-mono text-foreground-muted mt-1 leading-tight">
                        Fraud Command Center
                      </span>
                    </button>
                  </div>

                  {/* 0-second Direct Demo Bypass */}
                  <div className="p-2.5 rounded-lg border border-dashed border-border/80 bg-background-elevated/40 text-[11px] font-mono text-foreground-secondary flex items-center justify-between">
                    <span className="text-foreground-muted">Skip cloud spin-up:</span>
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => handleInstantOfflineDemo('ROLE_CUSTOMER')}
                        className="text-accent hover:underline font-semibold"
                      >
                        ⚡ Alice Demo
                      </button>
                      <span className="text-border">|</span>
                      <button
                        type="button"
                        onClick={() => handleInstantOfflineDemo('ROLE_ADMIN')}
                        className="text-status-warning hover:underline font-semibold"
                      >
                        ⚡ Admin Demo
                      </button>
                    </div>
                  </div>
                </div>

                {/* Register link */}
                <div className="pt-3 text-center text-xs font-mono text-foreground-secondary border-t border-border/60">
                  New to Aethel Bank?{' '}
                  <Link to="/register" className="text-accent hover:underline font-semibold inline-flex items-center gap-1">
                    <span>Create banking account</span>
                    <ArrowRight className="w-3 h-3" />
                  </Link>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};
