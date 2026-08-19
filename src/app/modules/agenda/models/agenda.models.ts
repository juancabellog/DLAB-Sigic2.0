import { NewsCategory } from '../../news/models/news.models';

export const PUBLICATION_STATUS = {
  DRAFT: 'draft',
  READY_TO_PUBLISH: 'ready_to_publish',
  PUBLISHED: 'published',
  UNPUBLISHED: 'unpublished'
} as const;

export type PublicationStatus = typeof PUBLICATION_STATUS[keyof typeof PUBLICATION_STATUS];

export const TRANSLATION_STATUS = {
  NO_TRANSLATION: 'no_translation',
  AUTO_GENERATED: 'auto_generated',
  MANUALLY_EDITED: 'manually_edited',
  REQUIRES_REVIEW: 'requires_review',
  VALIDATED: 'validated'
} as const;

export type TranslationStatus = typeof TRANSLATION_STATUS[keyof typeof TRANSLATION_STATUS];

export const EVENT_MODE = {
  IN_PERSON: 'in_person',
  ONLINE: 'online',
  HYBRID: 'hybrid'
} as const;

export type EventMode = typeof EVENT_MODE[keyof typeof EVENT_MODE];

/** Config: set true to require validated English before publish */
export const AGENDA_PUBLISH_CONFIG = {
  requireValidatedEnglish: false
};

export interface AgendaEvent {
  id?: number;
  titleEs: string;
  descriptionEs: string;
  titleEn: string;
  descriptionEn: string;
  mainImageUrl: string;
  mainImageAltEs: string;
  mainImageAltEn: string;
  categories: NewsCategory[];
  author: string;
  publicationStatus: PublicationStatus;
  translationStatus: TranslationStatus;
  eventDate: string | null;
  startTime: string;
  endTime: string;
  location: string;
  eventMode: EventMode | '';
  onlineUrl: string;
  slug: string;
  metaTitle: string;
  metaDescription: string;
  ogTitle: string;
  ogDescription: string;
  ogImageUrl: string;
  publicUrl: string;
  featured: boolean;
  createdAt?: string;
  updatedAt?: string;
  publishedAt?: string | null;
  translationValidatedAt?: string | null;
  basal?: string;
}

export interface AgendaListFilters {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
  publicationStatus?: PublicationStatus | '';
  categoryId?: string;
  eventMode?: EventMode | '';
  fromDate?: string;
  toDate?: string;
  title?: string;
  location?: string;
}

export interface AgendaEditorialMetadata {
  translationStatus: TranslationStatus;
  mainImageAltEs: string;
  mainImageAltEn: string;
  slug: string;
  metaTitle: string;
  metaDescription: string;
  ogTitle: string;
  ogDescription: string;
  ogImageUrl: string;
  publicUrl: string;
  featured?: boolean;
  categories?: NewsCategory[];
  translationValidatedAt: string | null;
  validatedSpanishSnapshot?: string;
  eventMode?: EventMode | '';
  onlineUrl?: string;
  endTime?: string;
}

export interface AgendaSpanishContent {
  titleEs: string;
  descriptionEs: string;
}

export interface AgendaEnglishContent {
  titleEn: string;
  descriptionEn: string;
}

export type AgendaTranslationDirection = 'es_to_en' | 'en_to_es';

export interface AgendaTranslationResult {
  titleEn?: string;
  descriptionEn?: string;
  titleEs?: string;
  descriptionEs?: string;
}

export const PUBLICATION_STATUS_LABELS: Record<PublicationStatus, string> = {
  draft: 'Draft',
  ready_to_publish: 'Ready to publish',
  published: 'Published',
  unpublished: 'Unpublished'
};

export const TRANSLATION_STATUS_LABELS: Record<TranslationStatus, string> = {
  no_translation: 'No translation',
  auto_generated: 'Auto-generated',
  manually_edited: 'Manually edited',
  requires_review: 'Requires review',
  validated: 'Validated'
};

export const EVENT_MODE_LABELS: Record<EventMode, string> = {
  in_person: 'In person',
  online: 'Online',
  hybrid: 'Hybrid'
};

export function createEmptyAgendaEvent(): AgendaEvent {
  const today = new Date();
  const eventDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  return {
    titleEs: '',
    descriptionEs: '',
    titleEn: '',
    descriptionEn: '',
    mainImageUrl: '',
    mainImageAltEs: '',
    mainImageAltEn: '',
    categories: [],
    author: '',
    publicationStatus: PUBLICATION_STATUS.DRAFT,
    translationStatus: TRANSLATION_STATUS.NO_TRANSLATION,
    eventDate,
    startTime: '09:00',
    endTime: '',
    location: '',
    eventMode: EVENT_MODE.IN_PERSON,
    onlineUrl: '',
    slug: '',
    metaTitle: '',
    metaDescription: '',
    ogTitle: '',
    ogDescription: '',
    ogImageUrl: '',
    publicUrl: '',
    featured: false,
    basal: 'N'
  };
}

export function slugify(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export function plainTextFromHtml(html: string | null | undefined): string {
  if (!html?.trim()) return '';
  return html.replace(/<[^>]*>/g, '').replace(/&nbsp;/gi, ' ').trim();
}

export function buildSpanishSnapshot(
  item: Pick<AgendaEvent, 'titleEs' | 'descriptionEs'>
): string {
  return JSON.stringify({
    titleEs: item.titleEs || '',
    descriptionEs: item.descriptionEs || ''
  });
}

export function hasRichTextContent(html: string | null | undefined): boolean {
  if (!html?.trim()) return false;
  return plainTextFromHtml(html).length > 0;
}

export function hasRequiredSpanishContent(
  item: Pick<AgendaEvent, 'titleEs' | 'descriptionEs'>
): boolean {
  if (!item.titleEs?.trim()) {
    return false;
  }
  return hasRichTextContent(item.descriptionEs);
}

export function hasEnglishContent(
  item: Pick<AgendaEvent, 'titleEn' | 'descriptionEn'>
): boolean {
  return Boolean(
    (item.titleEn && item.titleEn.trim()) ||
    (item.descriptionEn && item.descriptionEn.trim())
  );
}

export function isPastEvent(eventDate: string | null | undefined, referenceDate?: Date): boolean {
  if (!eventDate) return false;
  const ref = referenceDate ?? new Date();
  const today = new Date(ref.getFullYear(), ref.getMonth(), ref.getDate());
  const parsed = new Date(eventDate);
  if (Number.isNaN(parsed.getTime())) return false;
  const eventDay = new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate());
  return eventDay < today;
}

export function isEndTimeAfterStart(startTime: string, endTime: string): boolean {
  if (!endTime?.trim()) return true;
  if (!startTime?.trim()) return true;
  return endTime > startTime;
}

export function formatEventTimeRange(startTime: string, endTime: string): string {
  if (!startTime?.trim()) return '—';
  if (!endTime?.trim()) return startTime;
  return `${startTime} – ${endTime}`;
}

export function getPrimaryCategoryLabel(categories: NewsCategory[] | undefined): string {
  if (!categories?.length) return '—';
  return categories[0].label;
}
