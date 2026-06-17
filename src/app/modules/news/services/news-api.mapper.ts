import {
  NewsCategory,
  NewsEditorialMetadata,
  NewsItem,
  NewsRelatedPost,
  NewsTag,
  PUBLICATION_STATUS,
  PublicationStatus,
  TRANSLATION_STATUS,
  TranslationStatus
} from '../models/news.models';

export interface LocalizedTextApi {
  us?: string;
  es?: string;
}

export interface NoticiaApiDTO {
  id?: number;
  title?: LocalizedTextApi;
  excerpt?: LocalizedTextApi;
  body?: LocalizedTextApi;
  estado?: { id?: number; code?: string; label?: string };
  numVisitas?: number;
  numLikes?: number;
  image?: string;
  firstPublishedDate?: string;
  lastPublishedDate?: string;
  tags?: { id: string; label?: string; slug?: string }[];
  categories?: { id: string; label?: string; slug?: string }[];
  /** S / N */
  feature?: string;
  relatedPostIds?: number[];
  relatedPosts?: {
    id: number;
    title?: string;
    titleEn?: string;
    image?: string;
    author?: string;
    publicationDate?: string;
    publicationStatus?: string;
  }[];
  username?: string;
  createdAt?: string;
  updatedAt?: string;
  basal?: string;
}

export interface PaginatedNewsApi {
  content: NoticiaApiDTO[];
  totalElements: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

const ESTADO_TO_STATUS: Record<number, PublicationStatus> = {
  1: PUBLICATION_STATUS.PUBLISHED,
  2: PUBLICATION_STATUS.DRAFT,
  3: PUBLICATION_STATUS.UNPUBLISHED
};

const STATUS_TO_ESTADO: Record<PublicationStatus, number> = {
  [PUBLICATION_STATUS.PUBLISHED]: 1,
  [PUBLICATION_STATUS.DRAFT]: 2,
  [PUBLICATION_STATUS.UNPUBLISHED]: 3
};

export function mapEstadoToPublicationStatus(estado?: { id?: number; code?: string }): PublicationStatus {
  if (estado?.id && ESTADO_TO_STATUS[estado.id]) {
    return ESTADO_TO_STATUS[estado.id];
  }
  const code = (estado?.code || '').toUpperCase();
  if (code === 'PUBLISHED') return PUBLICATION_STATUS.PUBLISHED;
  if (code === 'UNPUBLISHED') return PUBLICATION_STATUS.UNPUBLISHED;
  return PUBLICATION_STATUS.DRAFT;
}

export function mapPublicationStatusToEstadoId(status: PublicationStatus): number {
  return STATUS_TO_ESTADO[status] ?? 2;
}

export function mergeApiWithMetadata(dto: NoticiaApiDTO, meta: NewsEditorialMetadata): NewsItem {
  const tags: NewsTag[] = (dto.tags || []).map(t => ({
    id: t.id,
    label: t.label || t.id,
    slug: t.slug
  }));

  const categories: NewsCategory[] = (dto.categories || []).map(c => ({
    id: c.id,
    label: c.label || c.id,
    slug: c.slug
  }));

  const relatedPosts: NewsRelatedPost[] = (dto.relatedPosts || []).map(r => ({
    id: r.id,
    title: r.title || '',
    titleEn: r.titleEn,
    thumbnailUrl: r.image,
    author: r.author,
    publicationDate: r.publicationDate,
    publicationStatus: mapEstadoToPublicationStatus(
      r.publicationStatus ? { code: r.publicationStatus } : undefined
    )
  }));

  const hasEn = Boolean(
    (dto.title?.us?.trim()) || (dto.excerpt?.us?.trim()) || (dto.body?.us?.trim())
  );

  let translationStatus = meta.translationStatus;
  if (!hasEn && translationStatus !== TRANSLATION_STATUS.NO_TRANSLATION) {
    // keep stored status
  } else if (!hasEn) {
    translationStatus = TRANSLATION_STATUS.NO_TRANSLATION;
  }

  return {
    id: dto.id,
    titleEs: dto.title?.es || '',
    titleEn: dto.title?.us || '',
    summaryEs: dto.excerpt?.es || '',
    summaryEn: dto.excerpt?.us || '',
    bodyEs: dto.body?.es || '',
    bodyEn: dto.body?.us || '',
    mainImageUrl: dto.image || '',
    mainImageAltEs: meta.mainImageAltEs,
    mainImageAltEn: meta.mainImageAltEn,
    tags,
    author: dto.username || '',
    publicationStatus: mapEstadoToPublicationStatus(dto.estado),
    translationStatus,
    publicationDate: dto.firstPublishedDate || dto.lastPublishedDate || null,
    slug: meta.slug,
    metaTitle: meta.metaTitle,
    metaDescription: meta.metaDescription,
    ogTitle: meta.ogTitle,
    ogDescription: meta.ogDescription,
    ogImageUrl: meta.ogImageUrl || dto.image || '',
    publicUrl: meta.publicUrl,
    featured: mapFeatureToBoolean(dto.feature) ?? meta.featured ?? false,
    categories: categories.length ? categories : (meta.categories || []),
    relatedPosts,
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt,
    publishedAt: dto.firstPublishedDate || null,
    translationValidatedAt: meta.translationValidatedAt,
    numVisitas: dto.numVisitas,
    numLikes: dto.numLikes,
    basal: dto.basal || 'N'
  };
}

export function mapNewsItemToApiPayload(item: NewsItem): NoticiaApiDTO {
  return {
    title: { es: item.titleEs, us: item.titleEn },
    excerpt: { es: item.summaryEs, us: item.summaryEn },
    body: { es: item.bodyEs, us: item.bodyEn },
    image: item.mainImageUrl,
    firstPublishedDate: item.publicationDate || undefined,
    lastPublishedDate:
      item.publicationStatus === PUBLICATION_STATUS.PUBLISHED
        ? item.publicationDate || undefined
        : undefined,
    estado: {
      id: mapPublicationStatusToEstadoId(item.publicationStatus ?? PUBLICATION_STATUS.DRAFT)
    },
    tags: item.tags.map(t => ({ id: t.id, label: t.label, slug: t.slug })),
    categories: (item.categories || []).map(c => ({ id: c.id, label: c.label, slug: c.slug })),
    feature: item.featured ? 'S' : 'N',
    relatedPostIds: (item.relatedPosts || []).map(r => r.id),
    basal: item.basal ?? 'N'
  };
}

function mapFeatureToBoolean(feature?: string | null): boolean | undefined {
  if (feature == null || feature === '') return undefined;
  const v = feature.trim().toUpperCase();
  if (v === 'S' || v === 'Y' || v === '1') return true;
  if (v === 'N' || v === '0') return false;
  return undefined;
}

export function extractMetadataFromNewsItem(item: NewsItem): NewsEditorialMetadata {
  return {
    translationStatus: item.translationStatus,
    mainImageAltEs: item.mainImageAltEs,
    mainImageAltEn: item.mainImageAltEn,
    slug: item.slug,
    metaTitle: item.metaTitle,
    metaDescription: item.metaDescription,
    ogTitle: item.ogTitle,
    ogDescription: item.ogDescription,
    ogImageUrl: item.ogImageUrl,
    publicUrl: item.publicUrl,
    translationValidatedAt: item.translationValidatedAt || null,
    validatedSpanishSnapshot:
      item.translationStatus === TRANSLATION_STATUS.VALIDATED || item.translationValidatedAt
        ? buildSnapshotFromItem(item)
        : undefined
  };
}

function buildSnapshotFromItem(item: NewsItem): string {
  return JSON.stringify({
    titleEs: item.titleEs || '',
    summaryEs: item.summaryEs || '',
    bodyEs: item.bodyEs || ''
  });
}
