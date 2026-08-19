export const PUBLICATION_STATUS = {
  DRAFT: 'draft',
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

/** Config: set true to require validated English before publish */
export const NEWS_PUBLISH_CONFIG = {
  requireValidatedEnglish: false
};

export interface NewsTag {
  id: string;
  label: string;
  slug?: string;
  /** True when selected locally but not yet persisted (created on save). */
  isPending?: boolean;
}

export interface NewsCategory {
  id: string;
  label: string;
  slug?: string;
  /** True when selected locally but not yet persisted (created on save). */
  isPending?: boolean;
}

export interface NewsRelatedPost {
  id: number;
  title: string;
  titleEn?: string;
  thumbnailUrl?: string;
  publicationStatus?: PublicationStatus | string;
  publicationDate?: string | null;
  author?: string;
}

export const MAX_RELATED_POSTS = 3;

export interface NewsItem {
  id?: number;
  titleEs: string;
  summaryEs: string;
  bodyEs: string;
  titleEn: string;
  summaryEn: string;
  bodyEn: string;
  mainImageUrl: string;
  mainImageAltEs: string;
  mainImageAltEn: string;
  tags: NewsTag[];
  categories: NewsCategory[];
  relatedPosts: NewsRelatedPost[];
  author: string;
  publicationStatus: PublicationStatus;
  translationStatus: TranslationStatus;
  publicationDate: string | null;
  slug: string;
  metaTitle: string;
  metaDescription: string;
  ogTitle: string;
  ogDescription: string;
  ogImageUrl: string;
  publicUrl: string;
  /** Public media / website URL — producto.linkVisualizacion */
  mediaLink: string;
  featured: boolean;
  createdAt?: string;
  updatedAt?: string;
  publishedAt?: string | null;
  translationValidatedAt?: string | null;
  numVisitas?: number;
  numLikes?: number;
  /** S / N — producto científico */
  basal?: string;
}

export interface NewsListFilters {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
  publicationStatus?: PublicationStatus | '';
  translationStatus?: TranslationStatus | '';
  tagId?: string;
  categoryId?: string;
  fromDate?: string;
  toDate?: string;
  title?: string;
}

export interface NewsEditorialMetadata {
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
  /** @deprecated API-backed via noticias.feature; kept for legacy localStorage metadata */
  featured?: boolean;
  /** @deprecated API-backed via noticias_category; kept for legacy localStorage metadata */
  categories?: NewsCategory[];
  translationValidatedAt: string | null;
  /** Snapshot used to detect Spanish edits after validation */
  validatedSpanishSnapshot?: string;
}

export interface NewsSpanishContent {
  titleEs: string;
  summaryEs: string;
  bodyEs: string;
}

export interface NewsEnglishContent {
  titleEn: string;
  summaryEn: string;
  bodyEn: string;
}

export type NewsTranslationDirection = 'es_to_en' | 'en_to_es';

export interface NewsTranslationResult {
  titleEn?: string;
  summaryEn?: string;
  bodyEn?: string;
  titleEs?: string;
  summaryEs?: string;
  bodyEs?: string;
}

export const PUBLICATION_STATUS_LABELS: Record<PublicationStatus, string> = {
  draft: 'Draft',
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

export function createEmptyNewsItem(): NewsItem {
  return {
    titleEs: '',
    summaryEs: '',
    bodyEs: '',
    titleEn: '',
    summaryEn: '',
    bodyEn: '',
    mainImageUrl: '',
    mainImageAltEs: '',
    mainImageAltEn: '',
    tags: [],
    categories: [],
    relatedPosts: [],
    author: '',
    publicationStatus: PUBLICATION_STATUS.DRAFT,
    translationStatus: TRANSLATION_STATUS.NO_TRANSLATION,
    publicationDate: new Date().toISOString().slice(0, 10),
    slug: '',
    metaTitle: '',
    metaDescription: '',
    ogTitle: '',
    ogDescription: '',
    ogImageUrl: '',
    publicUrl: '',
    mediaLink: '',
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

export function buildSpanishSnapshot(item: Pick<NewsItem, 'titleEs' | 'summaryEs' | 'bodyEs'>): string {
  return JSON.stringify({
    titleEs: item.titleEs || '',
    summaryEs: item.summaryEs || '',
    bodyEs: item.bodyEs || ''
  });
}

/** True when Spanish title, summary and body have non-empty text (body ignores HTML tags). */
export function hasRequiredSpanishContent(
  item: Pick<NewsItem, 'titleEs' | 'summaryEs' | 'bodyEs'>
): boolean {
  if (!item.titleEs?.trim() || !item.summaryEs?.trim()) {
    return false;
  }
  return hasRichTextContent(item.bodyEs);
}

export function hasRichTextContent(html: string | null | undefined): boolean {
  if (!html?.trim()) return false;
  const text = html
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .trim();
  return text.length > 0;
}

export function hasSpanishContent(item: Pick<NewsItem, 'titleEn' | 'summaryEn' | 'bodyEn'>): boolean {
  return Boolean(
    (item.titleEn && item.titleEn.trim()) ||
    (item.summaryEn && item.summaryEn.trim()) ||
    (item.bodyEn && item.bodyEn.trim())
  );
}
