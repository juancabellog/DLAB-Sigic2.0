export type LocalDateFormat = 'MMM yyyy' | 'MMM dd, yyyy' | 'dd MMM yyyy' | 'd MMMM yyyy';

const FORMAT_OPTIONS: Record<LocalDateFormat, Intl.DateTimeFormatOptions> = {
  'MMM yyyy': { month: 'short', year: 'numeric' },
  'MMM dd, yyyy': { month: 'short', day: 'numeric', year: 'numeric' },
  'dd MMM yyyy': { day: 'numeric', month: 'short', year: 'numeric' },
  'd MMMM yyyy': { day: 'numeric', month: 'long', year: 'numeric' }
};

/**
 * Parses a date-only string (YYYY-MM-DD) as a local calendar date.
 * Avoids the UTC midnight shift that makes `new Date('2026-06-25')` show as the previous day.
 */
export function parseLocalDateOnly(value: string): Date | null {
  const match = value.trim().match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!match) {
    return null;
  }
  const year = parseInt(match[1], 10);
  const month = parseInt(match[2], 10) - 1;
  const day = parseInt(match[3], 10);
  const date = new Date(year, month, day);
  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month ||
    date.getDate() !== day
  ) {
    return null;
  }
  return date;
}

/**
 * Formats API date strings without timezone shift for date-only values.
 */
export function formatLocalDate(
  value: string | null | undefined,
  format: LocalDateFormat = 'MMM dd, yyyy',
  locale = 'en-GB'
): string {
  if (!value?.trim()) {
    return '';
  }
  const trimmed = value.trim();
  const localDate = parseLocalDateOnly(trimmed);
  if (localDate) {
    return localDate.toLocaleDateString(locale, FORMAT_OPTIONS[format]);
  }
  const parsed = new Date(trimmed);
  if (Number.isNaN(parsed.getTime())) {
    return trimmed;
  }
  return parsed.toLocaleDateString(locale, FORMAT_OPTIONS[format]);
}
