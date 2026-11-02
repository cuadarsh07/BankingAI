import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { PasswordInput } from '@/components/ui/PasswordInput';
import { Button } from '@/components/ui/Button';
import { toast } from 'sonner';
import { UserPlus } from 'lucide-react';
import { Role } from '@/types';

const registerSchema = z.object({
  fullName: z.string().min(2, 'Full legal name is required'),
  username: z.string().email('Please enter a valid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  initialDeposit: z
    .string()
    .transform((val) => (val === '' ? 0 : parseFloat(val)))
    .refine((val) => !isNaN(val) && val >= 0, 'Initial deposit must be 0 or greater'),
  role: z.enum(['ROLE_CUSTOMER', 'ROLE_ADMIN'] as const),
});

type RegisterFormData = z.input<typeof registerSchema>;

export const RegisterPage: React.FC = () => {
  const { register: registerAuth } = useAuth();
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      username: '',
      password: '',
      initialDeposit: '1000',
      role: 'ROLE_CUSTOMER',
    },
  });

  const onSubmit = async (data: RegisterFormData) => {
    setIsSubmitting(true);
    try {
      await registerAuth({
        fullName: data.fullName,
        username: data.username,
        password: data.password,
        initialDeposit: parseFloat(data.initialDeposit as string) || 0,
        role: data.role as Role,
      });
      toast.success('Account successfully registered! Welcome to Aethel.');
      navigate('/dashboard');
    } catch (err: unknown) {
      const error = err as Error;
      toast.error(error.message || 'Registration failed. Please check your data.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-[82vh] flex items-center justify-center py-10 px-4">
      <div className="w-full max-w-lg space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-accent/10 border border-accent/25 rounded-full text-xs font-mono text-accent">
            <span className="w-1.5 h-1.5 bg-accent rounded-full animate-pulse" />
            <span>AI FRAUD SHIELD ACTIVE</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-heading font-extrabold tracking-tight text-foreground">
            Open Bank Account
          </h1>
          <p className="text-xs sm:text-sm text-foreground-secondary max-w-md mx-auto">
            Join next-generation banking with real-time scam interception and digital arrest defense.
          </p>
        </div>

        <Card withCrosshairs variant="elevated" className="border-border-strong shadow-2xl">
          <CardHeader>
            <CardTitle className="text-lg font-heading font-bold flex items-center gap-2">
              <UserPlus className="w-4 h-4 text-accent" />
              <span>Customer Registration</span>
            </CardTitle>
            <CardDescription>Enter legal details to create your secure banking identity</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <Input
                label="Full Legal Name"
                placeholder="e.g. Rajesh Kumar"
                {...register('fullName')}
                error={errors.fullName?.message}
              />

              <Input
                label="Email Address"
                placeholder="e.g. rajesh.kumar@example.com"
                {...register('username')}
                error={errors.username?.message}
                isMonospace
              />

              <PasswordInput
                label="Master Security Password"
                placeholder="At least 6 characters"
                {...register('password')}
                error={errors.password?.message}
              />

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Initial Deposit (₹ INR)"
                  placeholder="1000.00"
                  type="number"
                  step="0.01"
                  {...register('initialDeposit')}
                  error={errors.initialDeposit?.message}
                  isMonospace
                />

                <div className="space-y-1.5 text-left">
                  <label className="block text-xs font-mono uppercase tracking-tech text-foreground-secondary">
                    Account Classification
                  </label>
                  <select
                    {...register('role')}
                    className="w-full bg-background-secondary border border-border text-foreground text-xs font-mono rounded-sm px-3.5 py-2.5 focus:outline-none focus:border-accent"
                  >
                    <option value="ROLE_CUSTOMER">CUSTOMER ACCOUNT</option>
                    <option value="ROLE_ADMIN">SECURITY ADMINISTRATOR</option>
                  </select>
                </div>
              </div>

              <div className="pt-3">
                <Button
                  type="submit"
                  variant="primary"
                  className="w-full font-semibold"
                  isLoading={isSubmitting}
                >
                  CREATE BANK ACCOUNT
                </Button>
              </div>
            </form>

            <div className="mt-5 text-center text-xs font-mono text-foreground-secondary border-t border-border pt-4">
              Already have an active account?{' '}
              <Link to="/login" className="text-accent hover:underline font-semibold">
                Sign in to existing account &gt;
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};


