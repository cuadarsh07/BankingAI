import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Format currency strictly in Indian Rupees (INR - ₹) with standard Indian numbering format.
 */
export function formatCurrency(
  amount: number | string | undefined | null,
  currency: string = 'INR'
): string {
  if (amount === undefined || amount === null) return '₹0.00';
  const numericAmount = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (isNaN(numericAmount)) return '₹0.00';

  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numericAmount);
}

/**
 * Format dates with clean timestamp layout
 */
export function formatDate(dateString: string | undefined | null): string {
  if (!dateString) return '—';
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    return new Intl.DateTimeFormat('en-IN', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    }).format(date);
  } catch {
    return dateString;
  }
}

/**
 * Mask account number for security (e.g. "•••• •••• 4821")
 */
export function maskAccountNumber(accountNumber: string | undefined | null): string {
  if (!accountNumber) return '•••• ----';
  if (accountNumber.length <= 4) return accountNumber;
  const last4 = accountNumber.slice(-4);
  return `•••• •••• ${last4}`;
}
