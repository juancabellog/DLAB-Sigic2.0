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
  summaryEs: string;
  descriptionEs: string;
  titleEn: string;
  summaryEn: string;
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
  organizerEs: string;
  organizerEn: string;
  speakerEs: string;
  speakerEn: string;
  audienceEs: string;
  audienceEn: string;
  ctaLabelEs: string;
  ctaLabelEn: string;
  ctaUrl: string;
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
  organizerEs?: string;
  organizerEn?: string;
  speakerEs?: string;
  speakerEn?: string;
  audienceEs?: string;
  audienceEn?: string;
  ctaLabelEs?: string;
  ctaLabelEn?: string;
  ctaUrl?: string;
}

export interface AgendaSpanishContent {
  titleEs: string;
  summaryEs: string;
  descriptionEs: string;
}

export interface AgendaTranslationResult {
  titleEn: string;
  summaryEn: string;
  descriptionEn: string;
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
  return {
    titleEs: '',
    summaryEs: '',
    descriptionEs: '',
    titleEn: '',
    summaryEn: '',
    descriptionEn: '',
    mainImageUrl: '',
    mainImageAltEs: '',
    mainImageAltEn: '',
    categories: [],
    author: '',
    publicationStatus: PUBLICATION_STATUS.DRAFT,
    translationStatus: TRANSLATION_STATUS.NO_TRANSLATION,
    eventDate: null,
    startTime: '',
    endTime: '',
    location: '',
    eventMode: '',
    onlineUrl: '',
    organizerEs: '',
    organizerEn: '',
    speakerEs: '',
    speakerEn: '',
    audienceEs: '',
    audienceEn: '',
    ctaLabelEs: '',
    ctaLabelEn: '',
    ctaUrl: '',
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

export function buildSpanishSnapshot(
  item: Pick<AgendaEvent, 'titleEs' | 'summaryEs' | 'descriptionEs'>
): string {
  return JSON.stringify({
    titleEs: item.titleEs || '',
    summaryEs: item.summaryEs || '',
    descriptionEs: item.descriptionEs || ''
  });
}

export function hasRichTextContent(html: string | null | undefined): boolean {
  if (!html?.trim()) return false;
  const text = html
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .trim();
  return text.length > 0;
}

export function hasRequiredSpanishContent(
  item: Pick<AgendaEvent, 'titleEs' | 'summaryEs' | 'descriptionEs'>
): boolean {
  if (!item.titleEs?.trim() || !item.summaryEs?.trim()) {
    return false;
  }
  return hasRichTextContent(item.descriptionEs);
}

export function hasEnglishContent(
  item: Pick<AgendaEvent, 'titleEn' | 'summaryEn' | 'descriptionEn'>
): boolean {
  return Boolean(
    (item.titleEn && item.titleEn.trim()) ||
    (item.summaryEn && item.summaryEn.trim()) ||
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
