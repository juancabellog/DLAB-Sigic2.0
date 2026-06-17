/** Normalizes a taxonomy term for accent-insensitive, case-insensitive matching. */
export function normalizeTerm(value: string): string {
  return (value || '')
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, ' ');
}

export function termsMatch(a: string, b: string): boolean {
  return normalizeTerm(a) === normalizeTerm(b);
}

export function termContains(haystack: string, needle: string): boolean {
  const n = normalizeTerm(needle);
  if (!n) return true;
  return normalizeTerm(haystack).includes(n);
}

export function hasDuplicateTerm(labels: string[], candidate: string): boolean {
  return labels.some(label => termsMatch(label, candidate));
}

export interface TagLike {
  id: string;
  label: string;
  isPending?: boolean;
}

export const PENDING_TAXONOMY_PREFIX = 'pending:';

export function isPendingTaxonomyItem(item: TagLike): boolean {
  return !!item.isPending || item.id.startsWith(PENDING_TAXONOMY_PREFIX);
}

/**
 * Local in-memory tag filtering for catalogs up to a few hundred items.
 * Can be replaced later by remote searchTags(query) without changing the component API.
 */
export function filterTagsLocally<T extends TagLike>(
  catalog: T[],
  query: string,
  selected: TagLike[]
): T[] {
  const selectedLabels = selected.map(t => t.label);
  return catalog.filter(
    tag =>
      termContains(tag.label, query) &&
      !hasDuplicateTerm(selectedLabels, tag.label)
  );
}

export function canCreateTagFromInput(
  input: string,
  catalog: TagLike[],
  selected: TagLike[]
): boolean {
  const value = input.trim();
  if (!value) return false;
  const selectedLabels = selected.map(t => t.label);
  if (hasDuplicateTerm(selectedLabels, value)) return false;
  return !catalog.some(tag => termsMatch(tag.label, value));
}

export function createPendingTaxonomyId(label: string): string {
  const slug = label
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '') || 'item';
  return `${PENDING_TAXONOMY_PREFIX}${slug}-${Date.now()}`;
}
